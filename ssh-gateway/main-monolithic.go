package main

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"golang.org/x/crypto/ssh"
)

// Config holds the SSH gateway configuration
type Config struct {
	SSHPort     int    `json:"ssh_port"`
	APIBaseURL  string `json:"api_base_url"`
	JWTSecret   string `json:"jwt_secret"`
	HostKeyPath string `json:"host_key_path"`
}

// Container represents a container from the API
type Container struct {
	ID           int    `json:"id"`
	ContainerID  string `json:"containerId"`
	Name         string `json:"name"`
	Status       string `json:"status"`
	SSHPort      int    `json:"sshPort"`
	Running      bool   `json:"running"`
}

// AuthRequest represents authentication data
type AuthRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// AuthResponse represents authentication response
type AuthResponse struct {
	AccessToken string `json:"accessToken"`
	User        struct {
		ID       int    `json:"id"`
		Username string `json:"username"`
	} `json:"user"`
}

// SSHGateway handles SSH connections and proxying
type SSHGateway struct {
	config     Config
	hostKey    ssh.Signer
	httpClient *http.Client
}

func main() {
	config := Config{
		SSHPort:     2222,
		APIBaseURL:  "http://localhost:8080/api",
		JWTSecret:   "mySecretKey123456789012345678901234567890",
		HostKeyPath: "host_key",
	}

	gateway, err := NewSSHGateway(config)
	if err != nil {
		log.Fatal("Failed to create SSH gateway:", err)
	}

	log.Printf("SSH Gateway starting on port %d", config.SSHPort)
	if err := gateway.Start(); err != nil {
		log.Fatal("SSH Gateway failed:", err)
	}
}

// NewSSHGateway creates a new SSH gateway instance
func NewSSHGateway(config Config) (*SSHGateway, error) {
	hostKey, err := loadOrCreateHostKey(config.HostKeyPath)
	if err != nil {
		return nil, fmt.Errorf("failed to load host key: %v", err)
	}

	return &SSHGateway{
		config:  config,
		hostKey: hostKey,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}, nil
}

// Start begins listening for SSH connections
func (g *SSHGateway) Start() error {
	sshConfig := &ssh.ServerConfig{
		PasswordCallback: g.passwordCallback,
		PublicKeyCallback: g.publicKeyCallback,
	}
	sshConfig.AddHostKey(g.hostKey)

	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", g.config.SSHPort))
	if err != nil {
		return fmt.Errorf("failed to listen on port %d: %v", g.config.SSHPort, err)
	}
	defer listener.Close()

	log.Printf("SSH Gateway listening on :%d", g.config.SSHPort)

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Printf("Failed to accept connection: %v", err)
			continue
		}

		go g.handleConnection(conn, sshConfig)
	}
}

// passwordCallback handles password-based authentication
func (g *SSHGateway) passwordCallback(conn ssh.ConnMetadata, password []byte) (*ssh.Permissions, error) {
	username := conn.User()
	log.Printf("SSH password auth attempt for user: %s", username)

	// Authenticate with the Spring Boot API
	token, userID, err := g.authenticateWithAPI(username, string(password))
	if err != nil {
		log.Printf("Authentication failed for %s: %v", username, err)
		return nil, fmt.Errorf("authentication failed")
	}

	log.Printf("Authentication successful for user: %s (ID: %d)", username, userID)

	return &ssh.Permissions{
		Extensions: map[string]string{
			"token":  token,
			"userID": strconv.Itoa(userID),
		},
	}, nil
}

// publicKeyCallback handles public key authentication (placeholder)
func (g *SSHGateway) publicKeyCallback(conn ssh.ConnMetadata, key ssh.PublicKey) (*ssh.Permissions, error) {
	return nil, fmt.Errorf("public key auth not implemented yet")
}

// authenticateWithAPI authenticates user credentials with the Spring Boot API
func (g *SSHGateway) authenticateWithAPI(username, password string) (string, int, error) {
	authReq := AuthRequest{
		Username: username,
		Password: password,
	}

	jsonData, err := json.Marshal(authReq)
	if err != nil {
		return "", 0, err
	}

	resp, err := http.Post(g.config.APIBaseURL+"/auth/login", "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return "", 0, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", 0, fmt.Errorf("authentication failed with status: %d", resp.StatusCode)
	}

	var authResp AuthResponse
	if err := json.NewDecoder(resp.Body).Decode(&authResp); err != nil {
		return "", 0, err
	}

	return authResp.AccessToken, authResp.User.ID, nil
}

// handleConnection processes an individual SSH connection
func (g *SSHGateway) handleConnection(netConn net.Conn, sshConfig *ssh.ServerConfig) {
	defer netConn.Close()

	// Upgrade to SSH connection
	sshConn, newChans, reqs, err := ssh.NewServerConn(netConn, sshConfig)
	if err != nil {
		log.Printf("Failed to create SSH connection: %v", err)
		return
	}
	defer sshConn.Close()

	log.Printf("New SSH connection from %s (%s)", sshConn.RemoteAddr(), sshConn.User())

	// Handle global requests
	go ssh.DiscardRequests(reqs)

	// Handle channels
	for newChannel := range newChans {
		go g.handleChannel(sshConn, newChannel)
	}
}

// handleChannel processes SSH channels (session, direct-tcpip, etc.)
func (g *SSHGateway) handleChannel(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
	switch newChannel.ChannelType() {
	case "session":
		g.handleSessionChannel(sshConn, newChannel)
	case "direct-tcpip":
		g.handleDirectTCPIP(sshConn, newChannel)
	default:
		log.Printf("Rejecting channel type: %s", newChannel.ChannelType())
		newChannel.Reject(ssh.UnknownChannelType, "channel type not supported")
	}
}

// handleSessionChannel handles interactive SSH sessions with container selection
func (g *SSHGateway) handleSessionChannel(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
	channel, reqs, err := newChannel.Accept()
	if err != nil {
		log.Printf("Failed to accept session channel: %v", err)
		return
	}
	defer channel.Close()

	// Extract user info from permissions
	token := sshConn.Permissions.Extensions["token"]
	username := sshConn.User()

	// Get user's containers
	containers, err := g.getUserContainers(token)
	if err != nil {
		log.Printf("Failed to get containers for %s: %v", username, err)
		channel.Write([]byte("Failed to get your containers. Please contact support.\r\n"))
		return
	}

	// Handle PTY and shell requests
	go func() {
		for req := range reqs {
			switch req.Type {
			case "pty-req":
				req.Reply(true, nil)
			case "shell":
				req.Reply(true, nil)
				// Start interactive container selection
				g.startContainerSelection(channel, containers, token, username)
			case "exec":
				req.Reply(true, nil)
			default:
				req.Reply(false, nil)
			}
		}
	}()

	// Keep channel open
	io.Copy(io.Discard, channel)
}

// startContainerSelection shows container menu and handles selection
func (g *SSHGateway) startContainerSelection(channel ssh.Channel, containers []Container, token, username string) {
	// Show initial menu
	g.showContainerMenu(channel, containers)

	if len(containers) == 0 {
		return
	}

	// Show prompt for selection
	channel.Write([]byte("\r\nSelect container number (1-" + strconv.Itoa(len(containers)) + ") or 'quit': "))

	// Read user input
	input := make([]byte, 1)
	var selection strings.Builder
	
	for {
		n, err := channel.Read(input)
		if err != nil {
			log.Printf("Error reading from channel: %v", err)
			return
		}
		
		if n > 0 {
			char := input[0]
			
			if char == '\r' || char == '\n' {
				// Process selection
				selectionStr := strings.TrimSpace(selection.String())
				
				if selectionStr == "quit" || selectionStr == "exit" {
					channel.Write([]byte("\r\nGoodbye!\r\n"))
					return
				}
				
				// Parse container selection
				containerIndex, err := strconv.Atoi(selectionStr)
				if err != nil || containerIndex < 1 || containerIndex > len(containers) {
					channel.Write([]byte("\r\nInvalid selection. Try again: "))
					selection.Reset()
					continue
				}
				
				selectedContainer := containers[containerIndex-1]
				
				// Check if container is running
				if !selectedContainer.Running {
					channel.Write([]byte(fmt.Sprintf("\r\nContainer '%s' is not running. Please start it first.\r\n", selectedContainer.Name)))
					channel.Write([]byte("Select another container or 'quit': "))
					selection.Reset()
					continue
				}
				
				// Proxy to container
				channel.Write([]byte(fmt.Sprintf("\r\nConnecting to container '%s'...\r\n", selectedContainer.Name)))
				g.proxyToContainer(channel, selectedContainer, username)
				return
				
			} else if char == 127 || char == 8 { // Backspace
				if selection.Len() > 0 {
					channel.Write([]byte("\b \b")) // Erase character on terminal
					sel := selection.String()
					selection.Reset()
					selection.WriteString(sel[:len(sel)-1])
				}
			} else if char >= 32 && char <= 126 { // Printable characters
				channel.Write(input[:n]) // Echo character
				selection.WriteByte(char)
			}
		}
	}
}

// proxyToContainer creates a proper SSH proxy connection to the selected container
func (g *SSHGateway) proxyToContainer(channel ssh.Channel, container Container, username string) {
	// Connect to container SSH port
	containerAddr := fmt.Sprintf("localhost:%d", container.SSHPort)
	
	// Create SSH client connection to container
	sshConfig := &ssh.ClientConfig{
		User: "root",
		Auth: []ssh.AuthMethod{
			ssh.Password("password"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
		Timeout:         10 * time.Second,
	}
	
	containerSSHConn, err := ssh.Dial("tcp", containerAddr, sshConfig)
	if err != nil {
		log.Printf("Failed to create SSH connection to container %s at %s: %v", container.Name, containerAddr, err)
		channel.Write([]byte(fmt.Sprintf("Failed to connect to container via SSH. Error: %v\r\n", err)))
		return
	}
	defer containerSSHConn.Close()

	// Create session on container
	containerSession, err := containerSSHConn.NewSession()
	if err != nil {
		log.Printf("Failed to create session to container %s: %v", container.Name, err)
		channel.Write([]byte("Failed to create session to container\r\n"))
		return
	}
	defer containerSession.Close()

	log.Printf("SSH proxy session established for user %s to container %s (%s)", username, container.Name, containerAddr)

	// Set up terminal modes
	modes := ssh.TerminalModes{
		ssh.ECHO:          1,
		ssh.TTY_OP_ISPEED: 14400,
		ssh.TTY_OP_OSPEED: 14400,
	}

	// Request PTY on container
	if err := containerSession.RequestPty("xterm-256color", 80, 24, modes); err != nil {
		log.Printf("Failed to request PTY on container: %v", err)
		channel.Write([]byte("Failed to request PTY on container\r\n"))
		return
	}

	// Get stdin/stdout pipes for container session
	containerStdin, err := containerSession.StdinPipe()
	if err != nil {
		log.Printf("Failed to get stdin pipe: %v", err)
		return
	}
	defer containerStdin.Close()

	containerStdout, err := containerSession.StdoutPipe()
	if err != nil {
		log.Printf("Failed to get stdout pipe: %v", err)
		return
	}

	containerStderr, err := containerSession.StderrPipe()
	if err != nil {
		log.Printf("Failed to get stderr pipe: %v", err)
		return
	}

	// Start shell on container
	if err := containerSession.Shell(); err != nil {
		log.Printf("Failed to start shell on container: %v", err)
		channel.Write([]byte("Failed to start shell on container\r\n"))
		return
	}

	// Start bidirectional copying
	done := make(chan error, 3)

	// Client -> Container (stdin)
	go func() {
		defer containerStdin.Close()
		_, err := io.Copy(containerStdin, channel)
		done <- err
	}()

	// Container -> Client (stdout)
	go func() {
		_, err := io.Copy(channel, containerStdout)
		done <- err
	}()

	// Container -> Client (stderr)
	go func() {
		_, err := io.Copy(channel, containerStderr)
		done <- err
	}()

	// Wait for session to end
	go func() {
		err := containerSession.Wait()
		done <- err
	}()

	// Wait for any goroutine to finish
	err = <-done
	if err != nil && err != io.EOF {
		log.Printf("SSH proxy session error: %v", err)
	}

	log.Printf("SSH proxy session ended for user %s, container %s", username, container.Name)
}

// handleDirectTCPIP handles port forwarding
func (g *SSHGateway) handleDirectTCPIP(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
	// Port forwarding implementation could go here
	newChannel.Reject(ssh.Prohibited, "port forwarding not implemented")
}

// getUserContainers fetches containers for authenticated user
func (g *SSHGateway) getUserContainers(token string) ([]Container, error) {
	req, err := http.NewRequest("GET", g.config.APIBaseURL+"/containers", nil)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := g.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get containers: status %d", resp.StatusCode)
	}

	var containers []Container
	if err := json.NewDecoder(resp.Body).Decode(&containers); err != nil {
		return nil, err
	}

	return containers, nil
}

// showContainerMenu displays available containers to the user
func (g *SSHGateway) showContainerMenu(channel ssh.Channel, containers []Container) {
	channel.Write([]byte("\r\n=== SSH Platform - Container Gateway ===\r\n\r\n"))

	if len(containers) == 0 {
		channel.Write([]byte("No containers available.\r\n"))
		channel.Write([]byte("Create containers via: curl -X POST http://localhost:8080/api/containers\r\n"))
		return
	}

	channel.Write([]byte("Available containers:\r\n\r\n"))
	for i, container := range containers {
		status := "Stopped"
		if container.Running {
			status = "Running"
		}
		channel.Write([]byte(fmt.Sprintf("%d. %s (%s) - %s - Port: %d\r\n", 
			i+1, container.Name, container.ContainerID[:12], status, container.SSHPort)))
	}
}

// loadOrCreateHostKey loads existing host key or creates a new one
func loadOrCreateHostKey(keyPath string) (ssh.Signer, error) {
	// Try to load existing key
	if _, err := os.Stat(keyPath); err == nil {
		keyData, err := os.ReadFile(keyPath)
		if err != nil {
			return nil, err
		}
		return ssh.ParsePrivateKey(keyData)
	}

	// Generate new RSA key
	key, err := generateRSAKey(2048)
	if err != nil {
		return nil, err
	}

	// Save key to file
	if err := os.WriteFile(keyPath, key, 0600); err != nil {
		return nil, err
	}

	return ssh.ParsePrivateKey(key)
}

// generateRSAKey generates a new RSA private key
func generateRSAKey(bits int) ([]byte, error) {
	privateKey, err := rsa.GenerateKey(rand.Reader, bits)
	if err != nil {
		return nil, fmt.Errorf("failed to generate private key: %v", err)
	}

	privateKeyPEM := &pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: x509.MarshalPKCS1PrivateKey(privateKey),
	}

	return pem.EncodeToMemory(privateKeyPEM), nil
}