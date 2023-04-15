#!/bin/bash -x
set -eo pipefail

BUILDER=$1
OCI_IMAGE_PREFIX=$2
VERSION=$3

if [ -z $BUILDER ] || [ -z $OCI_IMAGE_PREFIX ] || [ -z $VERSION ] ; then 
    echo
    echo "Usage: $0 <src-builder-image> <oci-image-prefix> <new-builder-version>"
    echo "Example: $0 paketobuildpacks/builder:tiny docker.io/maliksalman focal-1.0.0"
    echo
    exit 1
fi

# make the directory for keeping builder info
BUILDER_DIR=".work/builders/$BUILDER"
mkdir -p $BUILDER_DIR

# create cache-dir
CACHE_DIR=".work/cache"
mkdir -p $CACHE_DIR

# start the manifest-fixer service
docker rm -f buildpacks-manifest-fixer 2> /dev/null
docker run -d \
    --name buildpacks-manifest-fixer \
    -e CONFIG_ARTIFACTSHACACHEDIR=/cache \
    -e CONFIG_IMAGEPREFIX=$OCI_IMAGE_PREFIX \
    -v $PWD/$CACHE_DIR:/cache \
    -p 58585:8080 \
    maliksalman/buildpacks-manifest-fixer:1.0.0-arm64

# download the remote builder's manifest
pack builder inspect $BUILDER \
    -o json | jq .remote_info > "$BUILDER_DIR/info.json"

# Pass #1: clone all buildpacks mentioned in the manifest
for BP in $(jq '.buildpacks[] | {id,version}' -c $BUILDER_DIR/info.json); do
    BP_ID=$(echo "$BP" | jq -r ".id")
	BP_VER=$(echo "$BP" | jq -r ".version")
    ./clone-buildpack.sh "$BP_ID" "$BP_VER" ".work/buildpacks"
done

# Pass #2, convert all simple buildpacks that don't have other buildpack dependencies
for BP in $(jq '.buildpacks[] | {id,version}' -c $BUILDER_DIR/info.json); do
    BP_ID=$(echo "$BP" | jq -r ".id")
	BP_VER=$(echo "$BP" | jq -r ".version")
    ./convert-buildpack.sh "$BP_ID" "$BP_VER" ".work/buildpacks" "false" $OCI_IMAGE_PREFIX
done

# Pass #3, convert all composite buildpacks 
for BP in $(jq '.buildpacks[] | {id,version}' -c $BUILDER_DIR/info.json); do
    BP_ID=$(echo "$BP" | jq -r ".id")
	BP_VER=$(echo "$BP" | jq -r ".version")
    ./convert-buildpack.sh "$BP_ID" "$BP_VER" ".work/buildpacks" "true" $OCI_IMAGE_PREFIX
done

# Figure out stack-id
export STACK_ID="$(jq '.stack.id' $BUILDER_DIR/info.json -r)"
export STACK_IMAGES_PREFIX=$(echo $BUILDER | sed 's|\(.*\)/\(.*\):\(.*\)|\1-\2-\3|')

# Create stack run image
STACK_RUN_IMAGE="$OCI_IMAGE_PREFIX/$STACK_IMAGES_PREFIX-run:arm64-$VERSION"
docker build . \
    -t  $STACK_RUN_IMAGE \
    --build-arg STACK_ID="$STACK_ID" \
    --target run

# Create stack build image
STACK_BUILD_IMAGE="$OCI_IMAGE_PREFIX/$STACK_IMAGES_PREFIX-build:arm64-$VERSION"
docker build . \
    -t $STACK_BUILD_IMAGE \
    --build-arg STACK_ID="$STACK_ID" \
    --target build

# Reconstruct builder manifest from original
jq  --arg STACK_ID "$STACK_ID" \
    --arg STACK_IMAGES_PREFIX "$OCI_IMAGE_PREFIX/$STACK_IMAGES_PREFIX" \
    '. += {stack: {id: $STACK_ID, "images-prefix": $STACK_IMAGES_PREFIX}}' \
    $BUILDER_DIR/info.json > $BUILDER_DIR/input.json

curl -X POST -H "Content-Type: application/json" \
    -d "$(cat $BUILDER_DIR/input.json)" \
    "http://localhost:58585/builder/arm64-${VERSION}" -s | yj -jt -i > $BUILDER_DIR/builder.toml

# Create builder image
BUILDER_IMAGE="$OCI_IMAGE_PREFIX/$STACK_IMAGES_PREFIX:arm64-$VERSION"
pack builder create "$BUILDER_IMAGE" \
    --config "$BUILDER_DIR/builder.toml" \
    --pull-policy never

# stop the manifest-fixer service
docker rm -f buildpacks-manifest-fixer 2> /dev/null

# Publish run, build, and builder images to registry with arm64 tags
docker push $STACK_RUN_IMAGE
docker push $STACK_BUILD_IMAGE
docker push $BUILDER_IMAGE

# Create docker manifests to merge offical paketo and arm64 builder image under a generic name
MANIFEST_IMAGE="$OCI_IMAGE_PREFIX/$STACK_IMAGES_PREFIX:$VERSION"
MANIFEST_EXISTS=$(docker manifest inspect $MANIFEST_IMAGE >/dev/null 2>/dev/null; echo $?)
if [ $MANIFEST_EXISTS -eq 0 ]; then
    docker manifest rm $MANIFEST_IMAGE
fi

docker pull $BUILDER
docker manifest create $MANIFEST_IMAGE \
    $BUILDER_IMAGE \
    $BUILDER
docker manifest push $MANIFEST_IMAGE

## PROFIT!!! ##