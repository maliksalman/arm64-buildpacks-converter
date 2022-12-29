#!/bin/bash

# Make sure docker daemon is present
docker ps > /dev/null 2> /dev/null
if [ $? -ne 0 ]; then
  echo 'ERROR: Docker daemon is not available'
  exit 1
fi

# create the image
export NATIVE_IMAGE=true
if [ "$(uname -m)" == "arm64" ]; then
  ./gradlew bootBuildImage \
    --imageName=maliksalman/buildpacks-manifest-fixer:1.0.0-arm64 \
    --builder=maliksalman/paketobuildpacks-builder-tiny:focal-1.0.1
else
  ./gradlew bootBuildImage \
    --imageName=maliksalman/buildpacks-manifest-fixer:1.0.0-amd64
fi