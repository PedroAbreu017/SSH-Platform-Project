package gateway

import (
	"fmt"
	"io"
	"log"
	"time"

	"golang.org/x/crypto/ssh"
	"ssh-gateway/internal/api"
)

// Proxy handles SSH proxy operations
type Proxy struct{}

// NewProxy creates a new proxy instance
func NewProxy() *Proxy {
	return &Proxy{}
}

// ProxyToContainer creates a proper SSH proxy connection to the selected container
func (p *Proxy) ProxyToContainer(channel ssh.Channel, container api.Container, username string) {
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

	// Request PTY on container with proper dimensions
	if err := containerSession.RequestPty("xterm-256color", 80, 24, modes); err != nil {
		log.Printf("Failed to request PTY on container: %v", err)
		channel.Write([]byte("Failed to request PTY on container\r\n"))
		return
	}

	// Get pipes for container session
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

	// Start bidirectional proxy with proper error handling
	done := make(chan error, 4)

	// Client -> Container (stdin)
	go func() {
		defer containerStdin.Close()
		_, err := io.Copy(containerStdin, channel)
		done <- fmt.Errorf("stdin copy ended: %v", err)
	}()

	// Container -> Client (stdout)
	go func() {
		_, err := io.Copy(channel, containerStdout)
		done <- fmt.Errorf("stdout copy ended: %v", err)
	}()

	// Container -> Client (stderr)
	go func() {
		_, err := io.Copy(channel, containerStderr)
		done <- fmt.Errorf("stderr copy ended: %v", err)
	}()

	// Wait for session to end
	go func() {
		err := containerSession.Wait()
		done <- fmt.Errorf("session ended: %v", err)
	}()

	// Wait for any goroutine to finish
	err = <-done
	if err != nil && err != io.EOF {
		log.Printf("SSH proxy session ended with: %v", err)
	}

	log.Printf("SSH proxy session ended for user %s, container %s", username, container.Name)
}

// ProxyCommand executes a single command on the container
func (p *Proxy) ProxyCommand(channel ssh.Channel, container api.Container, username, command string) {
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
		log.Printf("Failed to create SSH connection to container %s: %v", container.Name, err)
		channel.Write([]byte(fmt.Sprintf("Failed to connect to container: %v\r\n", err)))
		return
	}
	defer containerSSHConn.Close()

	// Create session on container
	containerSession, err := containerSSHConn.NewSession()
	if err != nil {
		log.Printf("Failed to create session to container %s: %v", container.Name, err)
		channel.Write([]byte("Failed to create session\r\n"))
		return
	}
	defer containerSession.Close()

	log.Printf("Executing command '%s' on container %s for user %s", command, container.Name, username)

	// Execute command
	output, err := containerSession.CombinedOutput(command)
	if err != nil {
		channel.Write([]byte(fmt.Sprintf("Command failed: %v\r\n", err)))
		return
	}

	// Send output back to client
	channel.Write(output)
	channel.Write([]byte("\r\nCommand completed.\r\n"))

	log.Printf("Command execution completed for user %s on container %s", username, container.Name)
}

// CheckContainerHealth checks if container SSH is responsive
func (p *Proxy) CheckContainerHealth(container api.Container) error {
	containerAddr := fmt.Sprintf("localhost:%d", container.SSHPort)

	sshConfig := &ssh.ClientConfig{
		User: "root",
		Auth: []ssh.AuthMethod{
			ssh.Password("password"),
		},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
		Timeout:         5 * time.Second,
	}

	// Try to establish connection
	conn, err := ssh.Dial("tcp", containerAddr, sshConfig)
	if err != nil {
		return fmt.Errorf("container %s SSH not accessible: %v", container.Name, err)
	}
	defer conn.Close()

	// Try to create a session
	session, err := conn.NewSession()
	if err != nil {
		return fmt.Errorf("container %s SSH session failed: %v", container.Name, err)
	}
	defer session.Close()

	// Run simple command to verify functionality
	if err := session.Run("echo 'health check'"); err != nil {
		return fmt.Errorf("container %s SSH command execution failed: %v", container.Name, err)
	}

	return nil
}