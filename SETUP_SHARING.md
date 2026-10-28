# One-time setup for "Share as image"

Sharing a picture needs one small addition to your `AndroidManifest.xml`. Without it the app
closes the moment you tap **Share** on the preview.

1. Open `app/src/main/AndroidManifest.xml`.
2. Inside the `<application ...>` tag (after the closing `</activity>`), paste:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

3. Make sure the file `app/src/main/res/xml/file_paths.xml` exists (it is included in the zip).
4. Click **Sync Now** (Gradle needs to download the QR-code library `com.google.zxing:core`).

## Why it is needed
Android does not let one app read another app's files. The `FileProvider` hands WhatsApp,
Instagram and friends a temporary, read-only permission for the single picture being shared,
and nothing else. `file_paths.xml` limits that to the `shared/` folder inside the app's cache.
