package com.gamequiz.service;

import com.gamequiz.config.QuizProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/** Knows where the quiz media lives and what is available. Media is served under {@code /media/**}. */
@Component
public class MediaCatalog {

    private static final String STAGE1_IMAGES = "images/stage1";
    private static final String STAGE2_IMAGES = "images/stage2";
    private static final String SOUNDS = "sounds";

    private final Path root;

    public MediaCatalog(QuizProperties properties) {
        this.root = Path.of(properties.getMediaDir()).toAbsolutePath().normalize();
    }

    public String stage1ImageUrl(String fileName) {
        return "/media/" + STAGE1_IMAGES + "/" + fileName;
    }

    public String stage2ImageUrl(String fileName) {
        return "/media/" + STAGE2_IMAGES + "/" + fileName;
    }

    public String soundUrl(String fileName) {
        return "/media/" + SOUNDS + "/" + fileName;
    }

    /** Whether the stage-2 image file exists, so that a missing file is shown as a placeholder, not a broken image. */
    public boolean stage2ImageExists(String fileName) {
        return Files.isRegularFile(root.resolve(STAGE2_IMAGES).resolve(fileName));
    }
}
