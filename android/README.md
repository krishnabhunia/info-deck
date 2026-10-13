# android/ — InfoDeck app

Gradle project for the InfoDeck Android app. The version comes from [`../VERSION`](../VERSION).

```bash
./gradlew :app:testReleaseUnitTest      # 278 JUnit tests
./gradlew :app:assembleRelease          # app/build/outputs/apk/release/
```

Signing reads `INFODECK_KEYSTORE`, `INFODECK_STOREPASS`, `INFODECK_KEYPASS` from the environment.
Without them the APK is built unsigned. No keystore or password is stored in this repository.

Structure, data model and flows: [../docs/architecture.md](../docs/architecture.md).
