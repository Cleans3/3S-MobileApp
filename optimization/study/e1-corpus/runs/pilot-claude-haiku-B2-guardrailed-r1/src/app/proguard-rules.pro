# App-specific R8 keep rules.
# The app uses no reflection-based libraries: OkHttp ships its own consumer rules,
# org.json and the Keystore APIs are platform classes, and Compose needs no extra rules.
# If a release build breaks after minification, add the smallest keep rule that fixes it here.
