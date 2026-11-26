package com.aidsa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Configures strict localhost CORS allowlists (never wildcard) and serves built frontend static
 * assets when available on disk so the desktop shell and local browser work offline.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    private final AppProperties properties;

    public WebMvcConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.security() != null && properties.security().allowedOrigins() != null
                ? properties.security().allowedOrigins()
                : List.of("http://127.0.0.1:8787", "http://localhost:8787");

        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Mentor-Local-Token", "X-Mentor-Client")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        resolveFrontendDistDir().ifPresent(distDir -> {
            String location = distDir.toUri().toString();
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            log.info("Serving frontend static bundle from {}", distDir);
            registry.addResourceHandler("/**")
                    .addResourceLocations(location, "classpath:/static/");
        });
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (resolveFrontendDistDir().isPresent()) {
            registry.addViewController("/").setViewName("forward:/index.html");
        }
    }

    private Optional<Path> resolveFrontendDistDir() {
        String configured = properties.frontend() != null ? properties.frontend().distPath() : null;
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured).toAbsolutePath().normalize();
            if (Files.isRegularFile(p.resolve("index.html"))) {
                return Optional.of(p);
            }
        }
        List<Path> candidates = List.of(
                Path.of("apps", "frontend", "dist"),
                Path.of("..", "frontend", "dist"),
                Path.of("..", "..", "apps", "frontend", "dist")
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized.resolve("index.html"))) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }
}
