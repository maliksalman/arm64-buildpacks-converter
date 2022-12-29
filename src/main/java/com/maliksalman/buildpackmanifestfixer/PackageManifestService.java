package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.regex.Matcher;

@Service
public class PackageManifestService {

    private final ReplacementPatternWithPrefix localImagesReplacement;

    public PackageManifestService(MainConfiguration configuration) {
        this.localImagesReplacement = configuration.localImagesReplacement();
    }

    public JsonNode fixPackageManifest(JsonNode manifest) {

        // fix the dependencies registry URLs
        JsonNode dependencies = manifest.get("dependencies");
        if (dependencies.isArray()) {
            Iterator<JsonNode> dependenciesIterator = dependencies.elements();
            while (dependenciesIterator.hasNext()) {
                JsonNode dependency = dependenciesIterator.next();
                String existingUri = dependency.get("uri").asText();

                Matcher matcher = localImagesReplacement.pattern().matcher(existingUri);
                if (matcher.matches()) {
                    String newUri = matcher.replaceAll(localImagesReplacement.getReplacementPattern());
                    ((ObjectNode) dependency).put("uri", newUri);
                }
            }
        }

        // Add the /buildpack/uri section if missing
        ObjectNode node = (ObjectNode) manifest.get("buildpack");
        if (node == null) {
            node = ((ObjectNode) manifest).putObject("buildpack");
        }
        node.put("uri", ".");

        return manifest;
    }
}
