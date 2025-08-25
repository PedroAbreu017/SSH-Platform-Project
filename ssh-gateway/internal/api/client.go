package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

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

// Client handles communication with Spring Boot API
type Client struct {
	baseURL    string
	httpClient *http.Client
}

// NewClient creates a new API client
func NewClient(baseURL string) *Client {
	return &Client{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// Authenticate authenticates user credentials with the Spring Boot API
func (c *Client) Authenticate(username, password string) (string, int, error) {
	authReq := AuthRequest{
		Username: username,
		Password: password,
	}

	jsonData, err := json.Marshal(authReq)
	if err != nil {
		return "", 0, fmt.Errorf("failed to marshal auth request: %v", err)
	}

	resp, err := http.Post(c.baseURL+"/auth/login", "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return "", 0, fmt.Errorf("failed to send auth request: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", 0, fmt.Errorf("authentication failed with status: %d", resp.StatusCode)
	}

	var authResp AuthResponse
	if err := json.NewDecoder(resp.Body).Decode(&authResp); err != nil {
		return "", 0, fmt.Errorf("failed to decode auth response: %v", err)
	}

	return authResp.AccessToken, authResp.User.ID, nil
}

// GetUserContainers fetches containers for authenticated user
func (c *Client) GetUserContainers(token string) ([]Container, error) {
	req, err := http.NewRequest("GET", c.baseURL+"/containers", nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %v", err)
	}

	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get containers: status %d", resp.StatusCode)
	}

	var containers []Container
	if err := json.NewDecoder(resp.Body).Decode(&containers); err != nil {
		return nil, fmt.Errorf("failed to decode containers response: %v", err)
	}

	return containers, nil
}

// ValidateToken validates if a token is still valid
func (c *Client) ValidateToken(token string) error {
	req, err := http.NewRequest("GET", c.baseURL+"/auth/validate-token", nil)
	if err != nil {
		return fmt.Errorf("failed to create validate request: %v", err)
	}

	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to validate token: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("token validation failed: status %d", resp.StatusCode)
	}

	return nil
}

// GetContainerLogs retrieves logs from a container
func (c *Client) GetContainerLogs(token string, containerID int, lines int) ([]string, error) {
	url := fmt.Sprintf("%s/containers/%d/logs?lines=%d", c.baseURL, containerID, lines)
	
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create logs request: %v", err)
	}

	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get logs: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get logs: status %d", resp.StatusCode)
	}

	var logs []string
	if err := json.NewDecoder(resp.Body).Decode(&logs); err != nil {
		return nil, fmt.Errorf("failed to decode logs: %v", err)
	}

	return logs, nil
}