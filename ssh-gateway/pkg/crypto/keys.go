package crypto

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"os"

	"golang.org/x/crypto/ssh"
)

// LoadOrCreateHostKey loads existing host key or creates a new one
func LoadOrCreateHostKey(keyPath string) (ssh.Signer, error) {
	// Try to load existing key
	if _, err := os.Stat(keyPath); err == nil {
		keyData, err := os.ReadFile(keyPath)
		if err != nil {
			return nil, fmt.Errorf("failed to read host key: %v", err)
		}
		
		signer, err := ssh.ParsePrivateKey(keyData)
		if err != nil {
			return nil, fmt.Errorf("failed to parse host key: %v", err)
		}
		
		return signer, nil
	}

	// Generate new RSA key
	key, err := GenerateRSAKey(2048)
	if err != nil {
		return nil, fmt.Errorf("failed to generate RSA key: %v", err)
	}

	// Save key to file
	if err := os.WriteFile(keyPath, key, 0600); err != nil {
		return nil, fmt.Errorf("failed to save host key: %v", err)
	}

	signer, err := ssh.ParsePrivateKey(key)
	if err != nil {
		return nil, fmt.Errorf("failed to parse generated key: %v", err)
	}

	return signer, nil
}

// GenerateRSAKey generates a new RSA private key
func GenerateRSAKey(bits int) ([]byte, error) {
	if bits < 2048 {
		return nil, fmt.Errorf("key size must be at least 2048 bits")
	}

	privateKey, err := rsa.GenerateKey(rand.Reader, bits)
	if err != nil {
		return nil, fmt.Errorf("failed to generate private key: %v", err)
	}

	// Encode private key to PKCS#1 ASN.1 PEM
	privateKeyPEM := &pem.Block{
		Type:  "RSA PRIVATE KEY",
		Bytes: x509.MarshalPKCS1PrivateKey(privateKey),
	}

	return pem.EncodeToMemory(privateKeyPEM), nil
}

// GenerateSSHKeyPair generates SSH public/private key pair
func GenerateSSHKeyPair(bits int) ([]byte, []byte, error) {
	privateKeyPEM, err := GenerateRSAKey(bits)
	if err != nil {
		return nil, nil, err
	}

	// Parse private key
	signer, err := ssh.ParsePrivateKey(privateKeyPEM)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to parse private key: %v", err)
	}

	// Get public key
	publicKey := signer.PublicKey()
	publicKeyBytes := ssh.MarshalAuthorizedKey(publicKey)

	return privateKeyPEM, publicKeyBytes, nil
}

// ValidatePrivateKey validates if a private key is valid
func ValidatePrivateKey(keyData []byte) error {
	_, err := ssh.ParsePrivateKey(keyData)
	return err
}