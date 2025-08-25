package gateway

import (
	"fmt"
	"io"
	"log"
	"net"
	"strconv"
	"strings"

	"golang.org/x/crypto/ssh"
	"ssh-gateway/internal/api"
	"ssh-gateway/internal/config"
)

// Server handles SSH gateway server operations
type Server struct {
	config    config.Config
	hostKey   ssh.Signer
	apiClient *api.Client
}

// NewServer creates a new SSH gateway server
func NewServer(cfg config.Config, hostKey ssh.Signer) *Server {
	return &Server{
		config:    cfg,
		hostKey:   hostKey,
		apiClient: api.NewClient(cfg.APIBaseURL),
	}
}

// Start begins listening for SSH connections
func (s *Server) Start() error {
	sshConfig := &ssh.ServerConfig{
		PasswordCallback:  s.passwordCallback,
		PublicKeyCallback: s.publicKeyCallback,
	}
	sshConfig.AddHostKey(s.hostKey)

	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", s.config.SSHPort))
	if err != nil {
		return fmt.Errorf("failed to listen on port %d: %v", s.config.SSHPort, err)
	}
	defer listener.Close()

	log.Printf("SSH Gateway listening on :%d", s.config.SSHPort)

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Printf("Failed to accept connection: %v", err)
			continue
		}

		go s.handleConnection(conn, sshConfig)
	}
}

// passwordCallback handles password-based authentication
func (s *Server) passwordCallback(conn ssh.ConnMetadata, password []byte) (*ssh.Permissions, error) {
	username := conn.User()
	log.Printf("SSH password auth attempt for user: %s", username)

	// Authenticate with the Spring Boot API
	token, userID, err := s.apiClient.Authenticate(username, string(password))
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
func (s *Server) publicKeyCallback(conn ssh.ConnMetadata, key ssh.PublicKey) (*ssh.Permissions, error) {
	return nil, fmt.Errorf("public key auth not implemented yet")
}

// handleConnection processes an individual SSH connection
func (s *Server) handleConnection(netConn net.Conn, sshConfig *ssh.ServerConfig) {
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
		go s.handleChannel(sshConn, newChannel)
	}
}

// handleChannel processes SSH channels (session, direct-tcpip, etc.)
func (s *Server) handleChannel(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
	switch newChannel.ChannelType() {
	case "session":
		s.handleSessionChannel(sshConn, newChannel)
	case "direct-tcpip":
		s.handleDirectTCPIP(sshConn, newChannel)
	default:
		log.Printf("Rejecting channel type: %s", newChannel.ChannelType())
		newChannel.Reject(ssh.UnknownChannelType, "channel type not supported")
	}
}

// handleSessionChannel handles interactive SSH sessions with container selection
func (s *Server) handleSessionChannel(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
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
	containers, err := s.apiClient.GetUserContainers(token)
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
				s.startContainerSelection(channel, containers, token, username)
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
func (s *Server) startContainerSelection(channel ssh.Channel, containers []api.Container, token, username string) {
	// Show initial menu
	s.showContainerMenu(channel, containers)

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

				// Create proxy to container
				proxy := NewProxy()
				channel.Write([]byte(fmt.Sprintf("\r\nConnecting to container '%s'...\r\n", selectedContainer.Name)))
				proxy.ProxyToContainer(channel, selectedContainer, username)
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

// showContainerMenu displays available containers to the user
func (s *Server) showContainerMenu(channel ssh.Channel, containers []api.Container) {
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

// handleDirectTCPIP handles port forwarding
func (s *Server) handleDirectTCPIP(sshConn *ssh.ServerConn, newChannel ssh.NewChannel) {
	// Port forwarding implementation could go here
	newChannel.Reject(ssh.Prohibited, "port forwarding not implemented")
}