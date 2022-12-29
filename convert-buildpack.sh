#!/bin/bash
set -eo pipefail

export BP_ID="$1"
export BP_VER="$2"
BP_BASE_DIR="$3"
COMPOSITE="$4"
OCI_IMAGE_PREFIX="$5"

if [ -z "$BP_ID" ] || [ -z "$BP_VER" ] || [ -z "$BP_BASE_DIR" ] || [ -z "$COMPOSITE" ] || [ -z "$OCI_IMAGE_PREFIX" ]; then
	echo ""
	echo "USAGE:"
	echo "  $0 <buildpack-id> <buildpack-version> <buildpacks-base-dir> <skip-composite-buildpacks> <oci-image-prefix>"
	echo ""
	echo "Enter the buildpack id/version for a buildpack."
	echo "The script will create an arm64 version image for the buildpack"
	echo ""
	exit 255
fi

replaceBuildpackToml() {
	TOML_BP_VER=$1
	curl -X POST -H "Content-Type: application/json" \
		-d "$(yj -t < buildpack.toml)" \
		"http://localhost:58585/buildpack/$TOML_BP_VER" -s | yj -jt -i > new-buildpack.toml
	mv new-buildpack.toml buildpack.toml
}

replacePackageToml() {
	curl -X POST -H "Content-Type: application/json" \
		-d "$(yj -t < package.toml)" \
		"http://localhost:58585/package" -s | yj -jt -i > new-package.toml
	mv new-package.toml package.toml
}

BP_DIR="$BP_BASE_DIR/$BP_ID:$BP_VER"
pushd "$BP_DIR" >/dev/null

	BP_IMAGE_ID=$(echo "${BP_ID}" | sed 's|\(.*\)/\(.*\)$|\1-\2|')
    BP_DEPENDENCIES=$(yj -t < buildpack.toml  | jq '.order[]?.group[]' -c  | wc -l)
    if [ $BP_DEPENDENCIES -eq 0 ] && [ "$COMPOSITE" == "false" ]; then
        echo ">>> processing simple buildpack - $BP_ID:$BP_VER <<<"

		replaceBuildpackToml $BP_VER
		create-package --destination ./out --version "$BP_VER"
		pushd ./out >/dev/null
			pack buildpack package \
				"${OCI_IMAGE_PREFIX}/${BP_IMAGE_ID}-arm64:${BP_VER}"
		popd >/dev/null

    elif [ $BP_DEPENDENCIES -gt 0 ] && [ "$COMPOSITE" == "true" ]; then
        echo ">>> processing composite buildpack - $BP_ID:$BP_VER <<<"

		replaceBuildpackToml $BP_VER
		replacePackageToml
		pack buildpack package \
			--config ./package.toml \
			--pull-policy never \
			"${OCI_IMAGE_PREFIX}/${BP_IMAGE_ID}-arm64:${BP_VER}"

    fi

popd >/dev/null

