// src/test/java/com/platform/integration/InfrastructureTest.java
package com.platform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InfrastructureTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void databaseConnectionWorks() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            
            var metaData = connection.getMetaData();
            assertThat(metaData.getDatabaseProductName()).isEqualToIgnoringCase("PostgreSQL");
        }
    }

    @Test
    void userRepositoryWorks() {
        assertThat(userRepository).isNotNull();
        assertThat(testUser).isNotNull();
        assertThat(testUser.getUsername()).isEqualTo("testuser");
        
        // Verificar se usuário foi salvo no banco
        var foundUser = userRepository.findByUsername("testuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void mockMvcWorks() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/actuator/health"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isOk());
    }
}