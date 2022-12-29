package com.maliksalman.buildpackmanifestfixer;

import lombok.Builder;

import java.util.regex.Pattern;

@Builder
public record ReplacementPatternWithPrefix(
    Pattern pattern,
    String replacement,
    String replacementPrefix
) {

    public String getReplacementPattern() {
        return replacementPrefix + replacement;
    }
}