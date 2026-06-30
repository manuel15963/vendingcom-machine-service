package com.vendingcom.machine_service.util.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vendingComMachineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VendingCom Machine Service API")
                        .version("1.0.0")
                        .description("""
                                Microservicio de gestión de máquinas vending de VendingCom.

                                Incluye:
                                - Registro y consulta de máquinas
                                - Eventos y documentos de la máquina
                                - Catálogos del módulo (estados, tipos de evento y tipos de documento)
                                - Auditoría de cambios
                                """)
                        .contact(new Contact()
                                .name("VendingCom")
                                .email("adolfo.berrocal@vallegrande.edu.pe"))
                        .license(new License()
                                .name("Proyecto universitario")
                                .url("https://vendingcom.local")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
