# Release builds run R8 (see app/build.gradle.kts). Keep rules are added here
# only when a reflection-based library needs them. This app currently uses
# OkHttp, Coil and Compose, which ship their own consumer rules, and org.json,
# which is part of the Android platform.
