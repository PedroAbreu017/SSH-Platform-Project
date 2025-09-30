package com.platform.repository;

import com.platform.model.entity.SSHKey;
import com.platform.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SSHKeyRepository extends JpaRepository<SSHKey, Long> {
    
    List<SSHKey> findByUser(User user);
    
    List<SSHKey> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<SSHKey> findByIdAndUser(Long id, User user);
    
    long countByUser(User user);
    
    boolean existsByUserAndPublicKey(User user, String publicKey);
    
    // Corrigido: active -> enabled
    List<SSHKey> findByUserAndEnabled(User user, boolean enabled);
    
    // Corrigido: active -> enabled
    @Query("SELECT s FROM SSHKey s WHERE s.user = :user AND s.enabled = true")
    List<SSHKey> findEnabledKeysByUser(@Param("user") User user);
    
    // Corrigido: nome e campo
    @Query("SELECT s FROM SSHKey s WHERE s.publicKey = :publicKey AND s.enabled = true")
    Optional<SSHKey> findByPublicKeyAndEnabled(@Param("publicKey") String publicKey);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM SSHKey s WHERE s.user = :user AND s.name = :name")
    boolean existsByUserAndName(@Param("user") User user, @Param("name") String name);
}
