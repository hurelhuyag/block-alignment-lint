package io.github.hurelhuyag.blockalign;

import java.nio.file.Path;
import java.util.Optional;

/** A supported source language, and the stripper that prepares it for analysis. */
public enum Language {

    JAVA(".java", new Stripper(Syntax.JAVA)),
    DART(".dart", new Stripper(Syntax.DART));

    private final String extension;
    private final SourceStripper stripper;

    Language(String extension, SourceStripper stripper) {
        this.extension = extension;
        this.stripper = stripper;
    }

    /** The file extension, including the leading dot. */
    public String extension() {
        return extension;
    }

    /** The stripper that blanks this language's literals and comments. */
    public SourceStripper stripper() {
        return stripper;
    }

    /**
     * @param file any path
     * @return the language matching its extension, or empty when unsupported
     */
    public static Optional<Language> of(Path file) {
        String name = file.getFileName().toString();
        for (Language language : values()) {
            if (name.endsWith(language.extension)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }
}
