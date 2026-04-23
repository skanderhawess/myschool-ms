package com.myschool.userservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test d'integration du contexte Spring Boot.
 *
 * DESACTIVE dans le pipeline CI car il charge le contexte complet, qui tente de :
 *   - Se connecter a MySQL (HikariCP) pour executer data.sql
 *   - S'enregistrer aupres d'Eureka (discovery-service)
 * Ces dependances ne sont pas disponibles dans l'environnement Jenkins.
 *
 * A reactiver uniquement lors de tests d'integration dedies (Testcontainers).
 */
@Disabled("Disabled in CI - requires MySQL and Eureka")
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionnellement vide — reactiver avec Testcontainers pour les tests d'integration
    }
}
