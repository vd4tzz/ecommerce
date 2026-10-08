package io.d4tzz.ecommerce.spring.openapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.filters.GlobalOpenApiMethodFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Commerce REST API")
                .description("API documentation for E-Commerce platform")
                .version("v1.0.0")
                .contact(new Contact().name("d4tzz").email("support@d4tzz.io"))
                .license(new License().name("Apache 2.0").url("https://springdoc.org")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .name(BEARER_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }

    @Bean
    public GlobalOpenApiMethodFilter onlyTaggedControllersFilter() {
        return method ->
            AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), Tag.class)
            || AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), Tags.class)
            || AnnotatedElementUtils.hasAnnotation(method, Tag.class)
            || AnnotatedElementUtils.hasAnnotation(method, Tags.class);
    }
}
