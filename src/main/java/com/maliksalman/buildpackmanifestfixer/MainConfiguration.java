package com.maliksalman.buildpackmanifestfixer;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

@Builder
@ConfigurationProperties("config")
public record MainConfiguration(

    List<Pattern> artifactsSkipped,
    List<ReplacementPattern> artifactsReplacement,
    File artifactShaCacheDir,
    ReplacementPatternWithPrefix localImagesReplacement,
    String lifecycleArtifactTemplate

) { }
