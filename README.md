# OpenCode for Android

A lightweight native Android wrapper for the [OpenCode](https://opencode.ai) web UI. Connect to your own OpenCode server from your phone.

## Features
- Enter your server URL once; it is remembered
- Optional username/password for password-protected servers (`OPENCODE_SERVER_PASSWORD`)
- Login/cookies persist, file uploads, mic and camera supported
- Plain `http://` servers supported (LAN use)
- External links open in your browser
- Press Back twice on the first page to change server settings

## Setup
1. On your computer run: `opencode web --hostname 0.0.0.0`
2. Install the APK from [Releases](../../releases/latest) (allow installs from unknown sources)
3. Enter your server address, e.g. `http://192.168.1.10:4096`

Requires Android 7.0+.

## Build
Run the **Build APK** GitHub Action, or open the folder in Android Studio and choose Build > Build APK(s). Pushing a `v*` tag publishes `OpenCode.apk` to a release.

The APK is debug-signed, suitable for personal use.
