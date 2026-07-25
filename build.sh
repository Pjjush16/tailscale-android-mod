#!/bin/bash

# Build script for Tailscale Android with Proxy Mode

set -e

echo "=== Tailscale Android Proxy Mode Builder ==="
echo ""

# Check if we're in the right directory
if [ ! -f "patch/proxy-mode.patch" ]; then
    echo "Error: patch/proxy-mode.patch not found"
    echo "Please run this script from the repository root"
    exit 1
fi

# Clone official repository if not exists
if [ ! -d "tailscale-android" ]; then
    echo "Cloning official Tailscale Android repository..."
    git clone https://github.com/tailscale/tailscale-android.git
fi

cd tailscale-android

# Apply patch
echo "Applying proxy mode patch..."
patch -p1 < ../patch/proxy-mode.patch

# Build AAR
echo "Building libtailscale AAR..."
./tool/go mod tidy
cd libtailscale
go build -buildmode=c-archive -o libtailscale.a .

# Build APK
echo "Building APK..."
cd ../android
./gradlew assembleDebug

echo ""
echo "=== Build Complete ==="
echo "APK location: android/app/build/outputs/apk/debug/"
echo ""
