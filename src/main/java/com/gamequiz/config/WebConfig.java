package com.gamequiz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves the quiz media (images, sounds) from the configured directory under {@code /media/**}. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final QuizProperties properties;

    public WebConfig(QuizProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path mediaRoot = Path.of(properties.getMediaDir()).toAbsolutePath().normalize();
        String location = mediaRoot.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
