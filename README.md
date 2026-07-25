# Tailscale Android - Proxy Mode with Built-in Browser

This repository contains modifications to the official Tailscale Android client to add:

1. **Proxy Mode** - Run Tailscale as a local SOCKS5/HTTP proxy without VPN permission
2. **Built-in Browser** - Access HTTP/FTP services on Tailnet with auto-proxy configuration
3. **Split Tunnel Mode** - Route only Tailscale traffic through VPN

## Features

### Proxy Mode (代理模式)
- SOCKS5 proxy on `127.0.0.1:1080`
- HTTP proxy on `127.0.0.1:8080`
- No VPN permission required
- Coexists with Clash/V2Ray and other proxy apps

### Built-in Browser (内置浏览器)
- Auto-configured to use local proxy (127.0.0.1:8080)
- Direct access to Tailnet HTTP/FTP services
- Address bar with navigation controls
- Welcome page with usage instructions

### Split Tunnel Mode (分流模式)
- Only routes Tailscale traffic (100.64.0.0/10)
- Other traffic uses system default route
- Allows other VPN/proxy apps to work simultaneously

## How to Build

### Prerequisites
- Official Tailscale Android source: https://github.com/tailscale/tailscale-android
- Android Studio
- Go 1.21+
- Android SDK & NDK

### Step 1: Clone Official Repository
```bash
git clone https://github.com/tailscale/tailscale-android.git
cd tailscale-android
```

### Step 2: Apply Patch
```bash
patch -p1 < patch/proxy-mode.patch
```

### Step 3: Build
```bash
./build.sh
```

Or use Android Studio to open and build the project.

## Installation

1. Enable "Unknown sources" in Android settings
2. Install the APK from `android/build/outputs/apk/`
3. Open the app and enable "Proxy Mode" in Settings
4. Use the built-in browser to access Tailnet services

## Files Modified

- `android/src/main/AndroidManifest.xml` - Added BuiltInBrowserActivity
- `android/src/main/java/com/tailscale/ipn/BuiltInBrowserActivity.kt` - New built-in browser
- `android/src/main/java/com/tailscale/ipn/ui/view/SettingsView.kt` - Added proxy mode toggle and browser button
- `android/src/main/java/com/tailscale/ipn/ui/viewModel/SettingsViewModel.kt` - Added proxy mode state
- `libtailscale/proxy.go` - Go bindings for proxy server
- `libtailscale/proxyserver.go` - Proxy server implementation

## Usage

1. Install the modified Tailscale app
2. Open app → Settings
3. Enable "Proxy Mode (代理模式)"
4. A button "Open Browser (打开浏览器)" will appear
5. Tap to open the built-in browser
6. Enter Tailnet URLs (e.g., `http://100.x.x.x/`)

## License

BSD-3-Clause (same as Tailscale)
