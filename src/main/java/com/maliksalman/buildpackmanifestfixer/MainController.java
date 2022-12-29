package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MainController {

    private final BuildpackManifestService buildpackManifestService;
    private final PackageManifestService packageManifestService;
    private final BuilderManifestService builderManifestService;

    @PostMapping("/buildpack/{version}")
    public JsonNode fixBuildpackManifest(@RequestBody JsonNode manifest,
                                         @PathVariable String version) {
        return buildpackManifestService.fixBuildpackManifest(manifest, version);
    }

    @PostMapping("/package")
    public JsonNode fixPackageManifest(@RequestBody JsonNode manifest) {
        return packageManifestService.fixPackageManifest(manifest);
    }

    @PostMapping("/builder/{version}")
    public BuilderManifest createBuilderManifest(@RequestBody JsonNode info,
                                                 @PathVariable String version) {
        return builderManifestService.createFromInfo(info, version);
    }
}
