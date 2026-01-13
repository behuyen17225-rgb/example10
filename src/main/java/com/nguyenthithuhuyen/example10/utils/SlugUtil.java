package com.nguyenthithuhuyen.example10.utils;

import java.text.Normalizer;
import java.util.Locale;

public class SlugUtil {

    public static String slugify(String input) {
        if (input == null) return "";

        String nowhitespace = input.trim().replaceAll("\\s+", "-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = normalized
                .replaceAll("[^\\w-]", "")
                .replaceAll("_", "-")
                .toLowerCase(Locale.ENGLISH);

        return slug;
    }
}
