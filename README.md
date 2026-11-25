# FPPuck Community

An unofficial Android app for following the Federal Prospects Hockey League: schedules, scores, standings, teams, player statistics, available shots on goal, and public league play-by-play. Favorites and themes stay on the device; no account or profile is required.

This is a standalone community edition. It has its own application ID, `org.fppuck.community`, and can coexist with other FPPuck editions. It does not include Google accounts, account-wide badges, push enrollment, production account APIs, private administrative tools, or watcher integrations. It requires no service account, API key, or private backend.

## Build

Install JDK 21 or newer and Android SDK Platform 37. Open the project in Android Studio, or set `JAVA_HOME` and `ANDROID_HOME` for your machine. Do not commit `local.properties`.

The official Gradle 9.6.0 wrapper is included. Its distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`.

On Windows:

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

On Linux/macOS:

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

The first build downloads public Gradle and Maven dependencies. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk` and uses your local Android debug signing key. No release signing key is supplied.

Optional device tests:

```sh
./gradlew :app:connectedDebugAndroidTest
```

## Data and privacy

Sports information and media are fetched from public FPHL/HockeyShift and linked league media services. Those providers can receive ordinary network metadata, including your IP address. Their content and availability are outside this project's control. The app does not contain a developer account server, analytics SDK, or advertising SDK. Favorites and settings are not uploaded to an FPPuck service. Android backup is disabled for this edition.

Do not add production credentials or private endpoints to a fork. See [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## Attribution

This project is not affiliated with or endorsed by the FPHL or its teams. League names, marks, statistics, and media remain subject to their owners' rights. The source-code license does not grant rights to third-party content. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

Original project code is licensed under the [MIT License](LICENSE). Third-party rights and notices remain separate.
