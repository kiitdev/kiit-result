#!/usr/bin/env bash
set -uo pipefail

# Compiles and runs sample-swift against the KiitResult.framework. SKIE compiles its generated
# Swift wrapper code (onEnum, the __Sealed enums, etc.) directly into the framework's own
# KiitResult.swiftmodule at build time — `import KiitResult` is all a consumer needs, no separate
# SKIE-generated source files to reference or compile.
#
# This references the Gradle build output directly rather than a real published XCFramework + SPM
# package, since that distribution pipeline doesn't exist yet (see README roadmap) — same "point
# at the build output for local verification" approach samples/sample-ts uses.
#
# Run from this directory: ./run.sh

cd "$(dirname "$0")"

FRAMEWORK_DIR="../../kiit-result/build/bin/iosSimulatorArm64/debugFramework"

if [[ ! -d "$FRAMEWORK_DIR/KiitResult.framework" ]]; then
  echo "error: $FRAMEWORK_DIR/KiitResult.framework not found." >&2
  echo "Build it first: ./gradlew :kiit-result:linkDebugFrameworkIosSimulatorArm64" >&2
  exit 1
fi

SDK_PATH="$(xcrun --sdk iphonesimulator --show-sdk-path)"

swiftc \
  -sdk "$SDK_PATH" \
  -target arm64-apple-ios16.4-simulator \
  -F "$FRAMEWORK_DIR" \
  -framework KiitResult \
  -I "$FRAMEWORK_DIR" \
  Sources/sample-swift/main.swift \
  -o /tmp/sample-swift-result-bin
COMPILE_STATUS=$?
if [[ "$COMPILE_STATUS" != "0" ]]; then
  exit "$COMPILE_STATUS"
fi

# Running an iOS-simulator-targeted binary directly on the host (even setting DYLD_ROOT_PATH)
# fails — it needs `dyld_sim`, which is only available inside an actual booted simulator. Boot
# one, run inside it via `simctl spawn`, then shut it back down — in a trap, so a failing run
# still leaves the simulator the way it found it, not just the successful-exit path.
DEVICE="iPhone 14"
BOOTED_BY_US=0
if ! xcrun simctl list devices | grep -q "$DEVICE.*Booted"; then
  xcrun simctl boot "$DEVICE"
  BOOTED_BY_US=1
fi

cleanup() {
  if [[ "$BOOTED_BY_US" == "1" ]]; then
    xcrun simctl shutdown "$DEVICE" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

xcrun simctl spawn "$DEVICE" /tmp/sample-swift-result-bin
exit $?
