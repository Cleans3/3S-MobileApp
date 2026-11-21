# R8 is enabled for release builds (isMinifyEnabled / isShrinkResources).
# The app uses org.json (platform), OkHttp and Coil, which ship their own consumer rules.
# Add a -keep rule here only if a reflection-based library is introduced and R8 breaks it.
