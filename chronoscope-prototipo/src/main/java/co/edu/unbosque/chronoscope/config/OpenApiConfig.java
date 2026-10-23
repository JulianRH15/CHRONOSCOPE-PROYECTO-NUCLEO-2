package co.edu.unbosque.chronoscope.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Titulo y descripcion que aparecen en Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chronoScopeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ChronoScope - API del prototipo")
                .version("1.0.0")
                .description("Generación y gestión de horarios de clases. Épica 1: ajuste y publicación "
                        + "del horario. Épica 2: cierre de grupos con re-optimización de mínima perturbación."));
    }
}
