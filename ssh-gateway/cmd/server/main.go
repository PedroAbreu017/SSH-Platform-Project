package main

import (
	"log"

	"ssh-gateway/internal/config"
	"ssh-gateway/internal/gateway"
	"ssh-gateway/pkg/crypto"
)

func main() {
	// Load configuration
	cfg, err := config.LoadConfig("config.json")
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Load or create host key
	hostKey, err := crypto.LoadOrCreateHostKey(cfg.HostKeyPath)
	if err != nil {
		log.Fatalf("Failed to load host key: %v", err)
	}

	// Create and start SSH gateway server
	server := gateway.NewServer(cfg, hostKey)
	
	log.Printf("Starting SSH Gateway on port %d", cfg.SSHPort)
	log.Printf("API Base URL: %s", cfg.APIBaseURL)
	
	if err := server.Start(); err != nil {
		log.Fatalf("SSH Gateway failed: %v", err)
	}
}