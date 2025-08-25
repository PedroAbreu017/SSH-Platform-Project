package main

import (
	"log"

	"ssh-gateway/internal/config"
	"ssh-gateway/internal/gateway"
	"ssh-gateway/pkg/crypto"
)

func main() {
	cfg, err := config.LoadConfig("config.json")
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	hostKey, err := crypto.LoadOrCreateHostKey(cfg.HostKeyPath)
	if err != nil {
		log.Fatalf("Failed to load host key: %v", err)
	}

	server := gateway.NewServer(cfg, hostKey)
	
	log.Printf("Starting SSH Gateway (Modular) on port %d", cfg.SSHPort)
	
	if err := server.Start(); err != nil {
		log.Fatalf("SSH Gateway failed: %v", err)
	}
}