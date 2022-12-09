# Paketo buildpack/builder arm64 converter

Scripts in this repository will try to create an `arm64` copy of the offical paketo buildpacks and builders. We need this so that people with M1 base macs and those running buildpacks on `arm64` servers can leverage the amazing buildpacks technology.

The code here is highly inspired by and sometimes outright lifted the excellent https://github.com/dmikusa/paketo-arm64 repo.

## General Conversion Strategy

### For tiny, base, full paketo builder

- Convert all published paketo builders/buildpacks for arm64
- Publish builder, build, & run images to docker registry
- Use `docker manifests` to join the arm64 and official builder images under one name

### For each builder

- Inspect the builder manifest
- For each buildpack mentioned in the manifest, build arm64 version image.  We might need to do this in 2 stages. Stage 1: build all buildpacks without dependent buildpacks. Stage2: build the buildpacks that have depdendcies):
    - Clone the buildpack github repo
    - Inspect buildpack.toml
    - For each item in `metadata.dependencies`, find and replace metadata with arm64 version
    - Build the buildpack image
- Reconstruct the manifest referencing newer buildpacks
- Create builder image
    
