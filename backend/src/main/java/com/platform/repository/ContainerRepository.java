package com.platform.repository;

import com.platform.model.entity.Container;
import com.platform.model.entity.User;
import com.platform.model.enums.ContainerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {
    
    List<Container> findByUser(User user);
    
    Optional<Container> findByIdAndUser(Long id, User user);
    
    List<Container> findByUserAndStatus(User user, ContainerStatus status);
    
    long countByUser(User user);
    
    long countByUserAndStatus(User user, ContainerStatus status);
    
    long countByUserAndStatusIn(User user, List<ContainerStatus> statuses);
    
    @Query("SELECT c FROM Container c WHERE c.user = :user AND c.status IN :statuses")
    List<Container> findByUserAndStatusIn(@Param("user") User user, @Param("statuses") List<ContainerStatus> statuses);
    
    @Query("SELECT c.sshPort FROM Container c WHERE c.sshPort IS NOT NULL")
    List<Integer> findAllSshPorts();
    
    @Query("SELECT c FROM Container c WHERE c.createdAt >= :startDate")
    List<Container> findContainersCreatedAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(c) FROM Container c WHERE c.status = :status")
    long countByStatus(@Param("status") ContainerStatus status);
    
    @Query("SELECT c FROM Container c WHERE c.containerId = :containerId")
    Optional<Container> findByContainerId(@Param("containerId") String containerId);
    
    @Query("SELECT c FROM Container c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "AND c.user = :user")
    List<Container> searchByNameAndUser(@Param("search") String search, @Param("user") User user);
}
