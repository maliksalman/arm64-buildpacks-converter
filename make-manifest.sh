#!/bin/bash

MANIFEST_IMAGE=$1
CONV_BUILDER=$2
ORIG_BUILDER=$3

docker manifest inspect $MANIFEST_IMAGE >/dev/null 2>/dev/null
if [[ $? -eq 0 ]]; then
    docker manifest rm $MANIFEST_IMAGE
fi

docker manifest create $MANIFEST_IMAGE \
    $CONV_BUILDER \
    $ORIG_BUILDER

docker manifest push $MANIFEST_IMAGE
