package config

import (
	"encoding/json"
	"os"
)

type Config struct {
	SSHPort     int    `json:"ssh_port"`
	APIBaseURL  string `json:"api_base_url"`
	JWTSecret   string `json:"jwt_secret"`
	HostKeyPath string `json:"host_key_path"`
}

func DefaultConfig() Config {
	return Config{
		SSHPort:     2222,
		APIBaseURL:  "http://localhost:8080/api",
		JWTSecret:   "mySecretKey123456789012345678901234567890",
		HostKeyPath: "host_key",
	}
}

func LoadConfig(filePath string) (Config, error) {
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		return DefaultConfig(), nil
	}

	data, err := os.ReadFile(filePath)
	if err != nil {
		return Config{}, err
	}

	var config Config
	if err := json.Unmarshal(data, &config); err != nil {
		return Config{}, err
	}

	return config, nil
}