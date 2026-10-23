package co.edu.unbosque.chronoscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Prototipo de alta fidelidad de ChronoScope.
 * Al arrancar, abrir http://localhost:8080 (interfaz Angular)
 * y http://localhost:8080/swagger-ui.html (documentacion de la API).
 */
@SpringBootApplication
public class ChronoScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoScopeApplication.class, args);
    }
}
