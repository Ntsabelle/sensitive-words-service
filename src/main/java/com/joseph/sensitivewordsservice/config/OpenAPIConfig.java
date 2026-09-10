package com.joseph.sensitivewordsservice.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI sensitiveWordOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sensitive Words Service API")
                        .version("1.0")
                        .description("API for managing sensitive words and sanitizing text.")
                        .contact(new Contact().name("Joseph").email("tshedison929@gmail.com")));
    }
}
