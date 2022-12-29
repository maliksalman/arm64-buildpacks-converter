package com.maliksalman.buildpackmanifestfixer;

import com.maliksalman.buildpackmanifestfixer.BuildpackManifestService.BuildpackDependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildpackDependencyServiceTest {

    BuildpackManifestService underTest;
    DependencyArtifactVerifier verifier;

    @BeforeEach
    void setup() {
        verifier = mock(DependencyArtifactVerifier.class);
        underTest = new BuildpackManifestService(verifier, MainConfiguration.builder()
                .artifactsReplacement(List.of(new ReplacementPattern(Pattern.compile("(.*)(amd64)(.*)"), "$1arm64$3")))
                .artifactsSkipped(Collections.emptyList())
                .build());
    }

    @Test
    void nothingChanges() {

        // GIVEN
        BuildpackDependency d = BuildpackDependency.builder()
                .uri("https://site/sbt.tgz")
                .purl("pkg:generic/sbt@1.8.0")
                .sha256("checksum")
                .build();

        // WHEN
        BuildpackDependency fix = underTest.fix(d);

        // THEN
        assertThat(fix.getUri()).isEqualTo(d.getUri());
        assertThat(fix.getPurl()).isEqualTo(d.getPurl());
        assertThat(fix.getSha256()).isEqualTo(d.getSha256());
    }
    @Test
    void uriChanges_noPurl() {

        // GIVEN
        when(verifier.verify(anyString())).thenReturn("sha256:123");
        BuildpackDependency d = BuildpackDependency.builder()
                .uri("https://github.com/anchore/syft/releases/download/v0.62.1/syft_0.62.1_linux_amd64.tar.gz")
                .purl("pkg:generic/anchore-syft@0.62.1")
                .sha256("89860504694a05a75688991ac24281cb84cfa61d48c973ddee7559fa7fc0a60e")
                .build();

        // WHEN
        BuildpackDependency fix = underTest.fix(d);

        // THEN
        assertThat(fix.getUri()).isEqualTo("https://github.com/anchore/syft/releases/download/v0.62.1/syft_0.62.1_linux_arm64.tar.gz");
        assertThat(fix.getPurl()).isEqualTo("pkg:generic/anchore-syft@0.62.1?arch=arm64");
        assertThat(fix.getSha256()).isEqualTo("sha256:123");
    }

    @Test
    void uriChanges_withPurl() {

        // GIVEN
        when(verifier.verify(anyString())).thenReturn("sha256:123");
        BuildpackDependency d = BuildpackDependency.builder()
                .uri("https://github.com/anchore/syft/releases/download/v0.62.1/syft_0.62.1_linux_amd64.tar.gz")
                .purl("pkg:generic/anchore-syft@0.62.1?arch=amd64")
                .sha256("89860504694a05a75688991ac24281cb84cfa61d48c973ddee7559fa7fc0a60e")
                .build();

        // WHEN
        BuildpackDependency fix = underTest.fix(d);

        // THEN
        assertThat(fix.getUri()).isEqualTo("https://github.com/anchore/syft/releases/download/v0.62.1/syft_0.62.1_linux_arm64.tar.gz");
        assertThat(fix.getPurl()).isEqualTo("pkg:generic/anchore-syft@0.62.1?arch=arm64");
        assertThat(fix.getSha256()).isEqualTo("sha256:123");
    }

}