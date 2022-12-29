package com.maliksalman.buildpackmanifestfixer;

import lombok.Builder;

import java.util.regex.Pattern;

@Builder
public record ReplacementPattern(

    Pattern pattern,
    String replacement

) { }