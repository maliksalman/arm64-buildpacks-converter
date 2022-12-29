package com.maliksalman.buildpackmanifestfixer;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record DependencyArtifactInfo(
        String uri,
        String sha256,
        OffsetDateTime downloadedAt,
        long sizeInBytes
) { }