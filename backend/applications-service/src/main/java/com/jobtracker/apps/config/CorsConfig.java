package com.jobtracker.apps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@ConditionalOnProperty(name = "cors.enabled", havingValue = "true", matchIfMissing = true)
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowed;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowed.split(","))
                .allowedMethods("GET","POST","PATCH","DELETE","OPTIONS")
                .allowedHeaders("Authorization","Content-Type","Idempotency-Key")
                .allowCredentials(true);
    }
}