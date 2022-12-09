#!/bin/bash

BUILDER=$1
if [[ -z $BUILDER ]]; then 
    echo
    echo "Usage: $0 <src-builder-image>"
    echo "Example: $0 paketobuildpacks/builder:base"
    echo
    exit 1
fi

# make the directory for keeping builder info
BUILDER_DIR=".work/builders/$BUILDER"
mkdir -p $BUILDER_DIR

# download the remote builder's manifest
pack builder inspect $BUILDER \
    -o json | jq .remote_info > "$BUILDER_DIR/info.json"

# iterate over all the buildpacks mentioned in the manifest and clone them
for BP in $(jq '.buildpacks[] | {id,version}' -c $BUILDER_DIR/info.json); do
    BP_ID=$(echo "$BP" | jq -r ".id")
	BP_VER=$(echo "$BP" | jq -r ".version")
    ./clone-buildpack.sh "$BP_ID" "$BP_VER" ".work/buildpacks"
done

# Pass #1, convert all buildpacks that don't have other buildpack dependencies


# Pass #3, convert all buildpacks that have other buildpack dependencies


# Create stack run image

# Create stack build image

# Reconstruct builder manifest from original

# Create builder image

# Publish run, build, and builder images to registry with arm64 tags

# Create docker manifests to merge offical paketo and arm64 builder image under a generic name

## PROFIT!!! ##