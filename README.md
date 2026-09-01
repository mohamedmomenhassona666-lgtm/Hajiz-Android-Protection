# حاجز / Hajiz

Hajiz is a local-first Android application that helps a person protect their
next decision by filtering selected internet domains and offering calm,
non-medical support during an urge.

## What it does

- Runs an official Android `VpnService` with a local DNS inspection point.
- Normalizes domain names, including case, trailing dots, ports, IDN/Punycode,
  exact domains, subdomains, wildcard rules, and IPv4 addresses.
- Stores the blocklist in a local Room database. Version 1 seeds only safe test
  domains: `blocked.example` and `adult-test.example`.
- Stores only aggregate local statistics: blocked attempts today, protected
  days, and Urge Mode uses.
- Provides Arabic RTL resources by default, with English resources available.
- Provides an Android Keystore-backed protection PIN and an Accountability Mode
  architecture.
- Detects the Device Owner / Profile Owner state without trying to bypass
  Android security.
- Restores the previously enabled protection state after reboot when Android
  permits a foreground service start.

## Architecture

```text
app/src/main/java/com/hajiz/app/
  data/       Room blocklist and DataStore settings
  filtering/  Domain normalization, matching, providers, DNS packets
  security/   Keystore PIN, configuration validation, device policy state
  ui/         Compose navigation shell, screens, settings, privacy, Urge Mode
  vpn/        VpnService, boot receiver, foreground notification
```

`BlocklistProvider` is the replacement boundary for a future authenticated
remote provider. The first release intentionally uses `LocalBlocklistProvider`
only. Browsing history is not sent to a server by default.

## How VPN filtering works

Hajiz asks Android for a local VPN permission. The service configures a
DNS-focused route and inspects IPv4 UDP DNS requests sent to the configured
resolver. A normalized question name is compared to the enabled local rules.
Blocked questions receive a local NXDOMAIN response; allowed questions are
forwarded to the upstream DNS resolver through a protected underlying socket.

The filtering engine is independent and unit-testable. It is deliberately
safe-by-default and does not inspect page bodies, collect URLs, or display the
blocked domain to the user.

This is not a full packet-forwarding firewall. Apps that use encrypted DNS
directly, hard-coded IP addresses, IPv6 paths, QUIC/DoH/DoT, or their own
network stack may bypass DNS-only filtering. The Android VPN API and the
device manufacturer also control what can be enforced.

## Android permissions

- `INTERNET` and `ACCESS_NETWORK_STATE`: local networking and state checks.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: the visible VPN
  service required by modern Android versions.
- `RECEIVE_BOOT_COMPLETED`: request restoration after reboot when permitted.
- `POST_NOTIFICATIONS`: show the visible protection-service notification on
  Android versions that require notification permission.
- `BIND_VPN_SERVICE`: declares the official VPN service contract.

The app does not request accessibility, contacts, device storage, location,
or unrelated private-data permissions.

## Strong protection and security model

There are three practical levels:

1. **Basic Protection** — protection runs while the app is installed and the
   VPN service is active.
2. **Strong Protection** — use Android Settings to enable Always-on VPN and,
   when available, VPN lockdown. Lockdown can prevent ordinary internet
   traffic while the VPN is disconnected.
3. **Accountability Mode** — sensitive settings can be protected by a PIN and
   a trusted person can be recorded as an accountability partner. The PIN
   digest is derived with PBKDF2 and encrypted with an Android Keystore AES-GCM
   key; the PIN is never stored as plaintext.

The app uses no root exploits, accessibility abuse, hidden persistence,
silent installation, credential theft, or privilege escalation. A
`DevicePolicyProtectionManager` abstraction reports whether the app is
Device
Owner, Profile Owner, or a normal personal-device app so managed-device
policies can be added safely later.

## Important limitation

> A normal Android application cannot guarantee 100% prevention of access to
> all internet content or guarantee that it cannot be uninstalled. The
> strongest protection depends on Android's official VPN, Always-on VPN, VPN
> lockdown, and device-management capabilities.

Uninstall prevention is not attempted. A stronger managed-device deployment
requires official Device Owner / Profile Owner enrollment and policies owned
by the device administrator.

## Build in Android Studio

1. Open the `android/` directory (not the repository root) in Android Studio.
2. Configure Android Studio/Gradle to use JDK 17 and install Android SDK
   Platform 35 and Build-Tools 35.x.
3. Select the `app` configuration and run it on an Android 8.0+ device or
   emulator.
4. On first launch, complete onboarding and grant the Android VPN permission.
5. For stronger protection, open Protection Settings and use the Android VPN
   Settings button to enable Always-on VPN and VPN lockdown if supported.

Command-line build from the `android/` directory:

```bash
./gradlew assembleDebug
```

The Gradle wrapper scripts and wrapper JAR are included in this repository;
the wrapper downloads the pinned Gradle 8.9 distribution when needed. The
debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`. A release build can be produced
with `./gradlew assembleRelease` and is written under
`app/build/outputs/apk/release/`; configure signing through
`gradle.properties` outside source control or Android Studio's signing
configuration. No signing credentials belong in this repository.

This environment does not include an Android SDK or JDK by default, so it
cannot compile the project unless Android SDK Platform 35 and JDK 17 are
installed and configured. Android Studio's bundled JDK 17 is suitable.

## Testing

Run the local JVM tests from Android Studio or with:

```bash
./gradlew test
```

Before creating a release artifact, run the complete local validation from the
same `android/` directory:

```bash
./gradlew test lint assembleDebug
./gradlew assembleRelease
```

Tests cover domain normalization, IDN/Punycode, invalid IP input, exact and
subdomain matching, wildcard matching, and security configuration validation.
Only safe placeholder domains are used.

## Blocked page behavior

The VPN layer returns a DNS failure instead of exposing a destination page.
The app UI never displays explicit content, explicit website names, full URLs,
or page contents. A future browser-facing companion page can use the neutral
copy “This content has been blocked. You chose to protect yourself from this
type of content.”