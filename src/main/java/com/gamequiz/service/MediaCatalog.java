package com.gamequiz.service;

import com.gamequiz.config.QuizProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Knows where the quiz media lives and what is available. Media is served under {@code /media/**}. */
@Component
public class MediaCatalog {

    private static final String STAGE1_IMAGES = "images/stage1";
    private static final String STAGE2_IMAGES = "images/stage2";
    private static final String SOUNDS = "sounds";

    /** Stage-2 images are named {@code p<question>_<number>.<ext>}; any common image extension works. */
    private static final Pattern STAGE2_FILE =
            Pattern.compile("(p\\d+_\\d+)\\.(jpg|jpeg|png|jfif|webp|gif)", Pattern.CASE_INSENSITIVE);

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

    /** Base name (lower case, e.g. {@code p3_2}) to actual file name of every stage-2 image on disk. */
    public Map<String, String> scanStage2Images() {
        Map<String, String> images = new HashMap<>();
        for (String fileName : listFiles(STAGE2_IMAGES)) {
            Matcher matcher = STAGE2_FILE.matcher(fileName);
            if (matcher.matches()) {
                images.put(matcher.group(1).toLowerCase(Locale.ROOT), fileName);
            }
        }
        return images;
    }

    public List<String> listStage1Images() {
        return listFiles(STAGE1_IMAGES);
    }

    public List<String> listSounds() {
        return listFiles(SOUNDS);
    }

    private List<String> listFiles(String relativeDirectory) {
        Path directory = root.resolve(relativeDirectory);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
