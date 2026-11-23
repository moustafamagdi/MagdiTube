# Release APK

Build and sign using the existing release key, then run:

    ./scripts/Stage-Release.ps1 -SignedApk <signed-apk> -SdkPath <android-sdk>

The script checks the package and signer, stages `MM-Tube.apk`, and reads the versionCode from the APK. Supply `-DraftTag vX.Y.Z` to upload the verified file to an existing empty draft using authenticated GitHub CLI. The asset is uploaded without a display label. The release body contains `<!-- mm-version-code: <code> -->`, read by MM Auto 0.2.17 and newer. Review the draft before publishing. GitHub adds the source archives.

Use a version tag such as `v1.2.3` or `v1.2.3-rc.1`. Older MM Auto builds only recognize versioned filenames. Distribute an MM Auto build with fixed-name support to users before switching their feeds, or provide its APK for a manual upgrade.
