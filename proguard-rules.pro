# Release builds use R8 (see app/build.gradle.kts). kotlinx.serialization and Compose ship
# their own consumer rules, so nothing is needed here for the app to work.
# Keep line numbers in crash reports:
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
