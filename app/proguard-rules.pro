# R8 rules for the release build.
#
# Nothing app-specific is needed: the app uses no reflection, and AndroidX
# ships its own consumer rules for anything R8 cannot see statically.
#
# Keep line numbers so a stack trace from a release build can still be
# deobfuscated with the mapping file R8 writes to
# app/build/outputs/mapping/release/mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
