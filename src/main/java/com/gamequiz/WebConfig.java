package com.gamequiz;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Upewnij się, że na końcu ścieżki ZAWSZE jest slash "/"
        registry.addResourceHandler("/moje-obrazki/**")
                .addResourceLocations(
                        "file:///F:/Projekty/GameQuiz/GameQuiz/src/main/java/com/gamequiz/images_e1/",
                        "file:///F:/Projekty/GameQuiz/GameQuiz/src/main/java/com/gamequiz/images_e2/"
                );

        registry.addResourceHandler("/moje-dzwieki/**")
                .addResourceLocations("file:///F:/Projekty/GameQuiz/GameQuiz/src/main/java/com/gamequiz/sounds/");
    }
}