package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuilderManifestService {

    private final MainConfiguration configuration;

    public BuilderManifest createFromInfo(JsonNode info, String version) {

        String lifecycleVersion = info.at("/lifecycle/version").asText();
        String lifecycleArtifactUri = configuration.lifecycleArtifactTemplate()
                .replace("{{VERSION}}", lifecycleVersion);

        List<BuilderManifest.Buildpack> buildpacks = getBuildpacks(info);

        return BuilderManifest.builder()
                .description(info.get("description").asText())
                .buildpacks(buildpacks)
                .lifecycle(BuilderManifest.Lifecycle.builder()
                        .uri(lifecycleArtifactUri)
                        .build())
                .order(buildpacks.stream()
                        .map(bp -> BuilderManifest.Order.builder()
                                .group(List.of(BuilderManifest.Group.builder()
                                        .id(bp.getId())
                                        .version(bp.getVersion())
                                        .build()))
                                .build())
                        .collect(Collectors.toList()))
                .stack(BuilderManifest.Stack.builder()
                        .id(info.at("/stack/id").asText())
                        .buildImage(info.at("/stack/images-prefix").asText() + "-build:" + version)
                        .runImage(info.at("/stack/images-prefix").asText() + "-run:" + version)
                        .build())
                .build();
    }

    private List<BuilderManifest.Buildpack> getBuildpacks(JsonNode info) {

        ArrayList<BuilderManifest.Buildpack> list = new ArrayList();
        info.get("detection_order")
                .elements()
                .forEachRemaining(node -> {
                    node.get("buildpacks")
                            .elements()
                            .forEachRemaining(bp -> {
                                BuilderManifest.Buildpack buildpack = BuilderManifest.Buildpack.builder()
                                        .id(bp.get("id").asText() + "-arm64")
                                        .version(bp.get("version").asText())
                                        .uri(getUri(bp.get("id").asText(), bp.get("version").asText()))
                                        .build();
                                if (!list.contains(buildpack)) {
                                    list.add(buildpack);
                                }
                            });
                });

        return list;
    }

    private String getUri(String id, String version) {

        ReplacementPatternWithPrefix imagesReplacement = configuration.localImagesReplacement();
        Matcher matcher = imagesReplacement
                .pattern()
                .matcher(id + ":" + version);
        if (matcher.matches()) {
            return matcher.replaceAll(imagesReplacement.getReplacementPattern());
        }

        return null;
    }
}
