package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class DependencyArtifactVerifier {

    private final RestTemplate template = new RestTemplate();

    private final File artifactShaCacheDir;
    private final ObjectMapper mapper;

    public DependencyArtifactVerifier(Jackson2ObjectMapperBuilder builder,
                                      MainConfiguration configuration) {
        this.mapper = builder.build();
        this.artifactShaCacheDir = configuration.artifactShaCacheDir();
    }

    public String verify(String artifactUri) throws DependencyArtifactNotFoundException {

        try {

            // Look in the cache first - so we don't have to download the artifact
            String uriMd5 = DigestUtils.md5Hex(artifactUri);
            File uriCachedFile = new File(artifactShaCacheDir, uriMd5 + ".json");
            if (uriCachedFile.exists()) {
                try (FileInputStream fis = new FileInputStream(uriCachedFile)) {
                    return mapper
                            .readValue(fis, DependencyArtifactInfo.class)
                            .sha256();
                }
            }

            // If we are here, lets download the artifact
            long time = System.currentTimeMillis();
            File file = template.execute(artifactUri, HttpMethod.GET, null, response -> {
                File ret = File.createTempFile("download", "tmp");
                StreamUtils.copy(response.getBody(), new FileOutputStream(ret));
                return ret;
            });
            log.info("Verified: Uri=[{}], DownloadDurationSeconds={}",
                    artifactUri,
                    Duration.ofMillis(System.currentTimeMillis()-time).toSeconds());

            // calculate the sha256
            String sha256 = DigestUtils.sha256Hex(new FileInputStream(file));

            // write out the cache - so we don't have to re-download
            // the artifact again - some of them are HUGE
            try(FileOutputStream fos = new FileOutputStream(uriCachedFile)) {
                mapper.writeValue(fos, DependencyArtifactInfo.builder()
                        .uri(artifactUri)
                        .sha256(sha256)
                        .downloadedAt(OffsetDateTime
                                .now()
                                .withOffsetSameInstant(ZoneOffset.UTC))
                        .sizeInBytes(file.length())
                        .build());
            }

            return sha256;

        } catch (Exception e) {
            log.warn("Can't verify dependency-artifact Uri=[{}] Message=[{}], Exception={}", artifactUri, e.getMessage(), e.getClass().getCanonicalName());
            throw new DependencyArtifactNotFoundException(artifactUri);
        }
    }

    public static class DependencyArtifactNotFoundException extends RuntimeException {
        public DependencyArtifactNotFoundException(String msg) {
            super(msg);
        }
    }
}
