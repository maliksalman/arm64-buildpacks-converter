package com.maliksalman.buildpackmanifestfixer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class BuilderManifest {

    String description;
    List<Buildpack> buildpacks;
    Lifecycle lifecycle;
    List<Order> order;
    Stack stack;

    @Builder
    @Value
    public static class Order {
        List<Group> group;
    }

    @Builder
    @Value
    public static class Group {
        String id;
        String version;
    }

    @Builder
    @Value
    public static class Lifecycle {
        String uri;
    }

    @Builder
    @Value
    public static class Buildpack {
        String id;
        String version;
        String uri;
    }

    @Builder
    @Value
    public static class Stack {

        @JsonProperty("build-image")
        String buildImage;

        @JsonProperty("run-image")
        String runImage;

        String id;
    }
}