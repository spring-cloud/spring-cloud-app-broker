#!/bin/bash
set -euo pipefail

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository

git clone git-repo stage-git-repo
echo

pushd stage-git-repo >/dev/null
git config user.name "Spring Buildmaster"
git config user.email "buildmaster@springframework.org"

snapshotVersion=$(awk -F '=' '$1 == "version" { print $2 }' gradle.properties)
if [[ $RELEASE_TYPE == "M" ]]; then
  stageVersion=$(get_next_milestone_release "$snapshotVersion")
  nextVersion="$snapshotVersion"
elif [[ $RELEASE_TYPE == "RC" ]]; then
  stageVersion=$(get_next_rc_release "$snapshotVersion")
  nextVersion="$snapshotVersion"
elif [[ $RELEASE_TYPE == "RELEASE" ]]; then
  stageVersion=$(get_next_release "$snapshotVersion")
  nextVersion=$(bump_version_number "$snapshotVersion")
else
  echo "Unknown release type $RELEASE_TYPE" >&2
  exit 1
fi

echo "Current version is v$snapshotVersion"
echo "Version to stage is v$stageVersion"
echo "Next development version will be v$nextVersion"
echo

git checkout -b "merge-on-success/$stageVersion"
echo

echo "Tagging version being staged (v$stageVersion)"
sed -i "s/version=$snapshotVersion/version=$stageVersion/" gradle.properties
git add gradle.properties
git commit -m "Release v$stageVersion"
git tag -a "v$stageVersion" -m "Release v$stageVersion"
echo

./gradlew --no-daemon clean install -Dmaven.repo.local="${repository}"
echo

echo "Setting next development version (v$nextVersion)"
sed -i "s/version=$stageVersion/version=$nextVersion/" gradle.properties
git add gradle.properties
git commit -m "Next development version (v$nextVersion)"
echo

echo "DONE"

popd >/dev/null
