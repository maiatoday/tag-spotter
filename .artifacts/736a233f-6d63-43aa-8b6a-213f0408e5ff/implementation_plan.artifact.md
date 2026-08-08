# Refactor Hardcoded Versions in libs.versions.toml

The goal is to move hardcoded version strings from the `[libraries]` section to the `[versions]` section in `gradle/libs.versions.toml` and use `version.ref` to reference them.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///Users/maia/workspace/maiatoday/tag-spotter/gradle/libs.versions.toml)

- Add `guava = "33.6.0-android"` to the `[versions]` section.
- Add `kotlinxSerializationJson = "1.11.0"` to the `[versions]` section.
- Update `guava` library to use `version.ref = "guava"`.
- Update `kotlinx-serialization-json` library to use `version.ref = "kotlinxSerializationJson"`.

## Verification Plan

### Automated Tests
- Run `./gradlew help` to ensure the version catalog is still valid and there are no syntax errors in `libs.versions.toml`.
