package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class BuildpackManifestService {

    private final DependencyArtifactVerifier verifier;
    private final MainConfiguration configuration;

    public JsonNode fixBuildpackManifest(JsonNode manifest, String version) {

        // fix the id and version
        JsonNode buildpack = manifest.get("buildpack");
        updateIdAttribute(buildpack);
        updateVersionAttribute(buildpack, version);

        // fix the names in buildpack references
        JsonNode orders = manifest.get("order");
        if (orders != null && orders.isArray()) {
            Iterator<JsonNode> ordersIterator = orders.elements();
            while (ordersIterator.hasNext()) {
                JsonNode order = ordersIterator.next();
                JsonNode groups = order.get("group");
                if (groups != null && groups.isArray()) {
                    Iterator<JsonNode> groupsIterator = groups.elements();
                    while (groupsIterator.hasNext()) {
                        JsonNode group = groupsIterator.next();
                        updateIdAttribute(group);
                    }
                }
            }
        }

        // fix the dependencies
        JsonNode dependencies = manifest.at("/metadata/dependencies");
        if (dependencies != null && dependencies.isArray()) {
            Iterator<JsonNode> iterator = dependencies.elements();
            while (iterator.hasNext()) {
                if (iterator.next() instanceof ObjectNode node) {

                    BuildpackDependency original = BuildpackDependency.builder()
                            .purl(node.has("purl") ? node.get("purl").asText() : null)
                            .sha256(node.has("sha256") ? node.get("sha256").asText() : null)
                            .uri(node.get("uri").asText())
                            .build();

                    BuildpackDependency fixed = fix(original);

                    node.put("purl", fixed.getPurl());
                    node.put("uri", fixed.getUri());
                    node.put("sha256", fixed.getSha256());
                }
            }
        }

        return manifest;
    }

    private void updateIdAttribute(JsonNode node) {
        if (node.has("id")) {
            ((ObjectNode) node).put("id", node.get("id").asText() + "-arm64");
        }
    }

    private void updateVersionAttribute(JsonNode node, String version) {
        ((ObjectNode) node).put("version", version);
    }

    protected BuildpackDependency fix(BuildpackDependency original) {

        Optional<String> alternativeUri = determineAlternativeUri(original.getUri());
        Optional<String> alternativePurl = determineAlternativePurl(original.getPurl(), alternativeUri);
        Optional<String> alternativeSha256 = Optional.empty();

        if (alternativeUri.isPresent()) {
            alternativeSha256 = Optional.of(verifier.verify(alternativeUri.get()));
        }

        return BuildpackDependency.builder()
                .uri(alternativeUri.orElse(original.getUri()))
                .purl(alternativePurl.orElse(original.getPurl()))
                .sha256(alternativeSha256.orElse(original.getSha256()))
                .build();
    }

    private Optional<String> determineAlternativeUri(String uri) {

        if (configuration.artifactsSkipped().stream()
                .noneMatch(pattern -> pattern.matcher(uri).matches())) {

            for (ReplacementPattern pattern : configuration.artifactsReplacement()) {
                Matcher matcher = pattern.pattern().matcher(uri);
                if (matcher.matches()) {
                    return Optional.of(matcher.replaceAll(pattern.replacement()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> determineAlternativePurl(String purl, Optional<String> alternativeUri) {
        if (alternativeUri.isPresent() && purl != null) {
            if (purl.contains("amd64")) {
                return Optional.of(purl.replace("amd64", "arm64"));
            } else {
                return Optional.of(purl + "?arch=arm64");
            }
        }
        return Optional.empty();
    }

    @Builder
    @Value
    public static class BuildpackDependency {
        String purl;
        String sha256;
        String uri;
    }
}
