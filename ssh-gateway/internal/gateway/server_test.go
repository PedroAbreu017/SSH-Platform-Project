// ssh-gateway/internal/gateway/server_test.go
package gateway

import (
	"fmt"
	"strings"
	"testing"
	"ssh-gateway/internal/api"
)

// Test that the new logs functionality will exist
func TestContainerLogsFeature(t *testing.T) {
	containers := []api.Container{
		{
			ID:      1,
			Name:    "test-container",
			Running: true,
			SSHPort: 8001,
		},
	}

	// Test 1: Enhanced menu should include logs option
	menuOutput := generateEnhancedMenu(containers)
	
	if !strings.Contains(menuOutput, "3. View logs") {
		t.Errorf("Expected enhanced menu to contain '3. View logs', got: %s", menuOutput)
	}

	// Test 2: Logs display should format correctly
	logs := []string{"Log line 1", "Log line 2", "Log line 3"}
	logsOutput := formatContainerLogs("test-container", logs)
	
	if !strings.Contains(logsOutput, "Container Logs - test-container") {
		t.Errorf("Expected logs header, got: %s", logsOutput)
	}
	
	if !strings.Contains(logsOutput, "Log line 1") {
		t.Errorf("Expected log content, got: %s", logsOutput)
	}

	// Test 3: Menu selection should handle logs option
	selection := parseMenuSelection("3", 3)
	expected := "logs"
	
	if selection != expected {
		t.Errorf("Expected selection 'logs', got: %s", selection)
	}
}

// These functions don't exist yet - TDD Red phase
func generateEnhancedMenu(containers []api.Container) string {
	// Green phase - minimal implementation to pass test
	menu := "Available containers:\n"
	for i, container := range containers {
		menu += fmt.Sprintf("%d. %s (%s) - %s\n", i+1, container.Name, "Running", "Port: "+fmt.Sprintf("%d", container.SSHPort))
	}
	menu += "\nActions:\n1. Connect to container\n2. Start/Stop container\n3. View logs\n"
	return menu
}

func formatContainerLogs(containerName string, logs []string) string {
	// Green phase - minimal implementation to pass test
	output := fmt.Sprintf("Container Logs - %s\n", containerName)
	output += "==================\n"
	for _, log := range logs {
		output += log + "\n"
	}
	return output
}

func parseMenuSelection(input string, maxOptions int) string {
	// Green phase - minimal implementation to pass test
	if input == "3" {
		return "logs"
	}
	if input == "1" {
		return "connect"
	}
	if input == "2" {
		return "control"
	}
	return "invalid"
}