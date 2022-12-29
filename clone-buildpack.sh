#!/bin/bash
set -eo pipefail

BP_ID="$1"
BP_VER="$2"
BP_BASE_DIR="$3"

if [ -z "$BP_ID" ] || [ -z "$BP_VER" ] || [ -z "$BP_BASE_DIR" ]; then
	echo ""
	echo "USAGE:"
	echo "  $0 <buildpack-id> <buildpack-version> <buildpacks-base-dir>"
	echo ""
	echo "Enter the buildpack id/version for a buildpack."
	echo "The script will clone the buildpack if not already cloned."
	echo ""
	exit 255
fi

### NOTE 
# Verified that we can generate Github REPO addresses from BP_ID
# for URL in $(jq '.buildpacks[] | ("https://github.com/" + .id) ' -rc  builder-src.json | sort | uniq); do echo $URL; curl -s -o /dev/null -w "%{http_code}\n" $URL; done
###

BP_DIR="$BP_BASE_DIR/$BP_ID:$BP_VER"
if [ -d $BP_DIR ]; then
    echo ">>> Skipping cloning $BP_ID:$BP_VER <<<"
    pushd "$BP_DIR" >/dev/null
	git restore .
	git clean -fd
	popd >/dev/null
else
    echo ">>> Cloning $BP_ID:$BP_VER into $BP_DIR <<<"
    git clone "https://github.com/$BP_ID" "$BP_DIR"
    pushd "$BP_DIR" >/dev/null
    git -c "advice.detachedHead=false" checkout "v$BP_VER"
    popd >/dev/null
fi
