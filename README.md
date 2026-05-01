# Ultimate Production-Ready web2apk Template

This template allows you to generate a custom, production-ready Android APK and App Bundle (AAB) via GitHub Actions.

## 🔥 Enterprise-Grade Features
- **Native Android Permissions:** Hardware camera, location, and storage are fully supported.
- **File System Support:** Users can download PDFs/files to their device and upload images from their gallery via the web interface.
- **External Intents:** URLs like `whatsapp://`, `mailto:`, or `tel:` open natively in the user's apps rather than crashing the WebView.
- **Pull-to-Refresh & Offline States:** Swipe down to reload. If the device is offline, a native "Connection Error" screen is shown instead of a broken webpage.
- **Custom User-Agent:** The User-Agent string is suffixed with `Web2APK-Android` so your website can detect app users.
- **Automated App Icons:** Provide a single 512x512 URL, and the CI uses `ImageMagick` to generate all required Android densities (mdpi to xxxhdpi) automatically.
- **Play Store Ready:** Generates both `.apk` (for direct distribution) and `.aab` (required for Google Play Store).

## 🖱️ How to Build via GitHub UI (Manual)
1. Go to the **Actions** tab in your repository.
2. Select the **Build Ultimate Production App** workflow.
3. Click **Run workflow**.
4. Fill in the required details:
    * `App Name`: Your app's display name.
    * `Target URL`: The website to load.
    * `Icon URL`: Direct link to your app icon (PNG/JPG).
    * `Version Code`: Integer (e.g. 1, 2, 3). Must increase every update.
    * `Version Name`: String (e.g. 1.0.0).
    * **Keystore Inputs:** Enter your Base64 keystore and passwords (see section below for API automation).
5. Wait for the build to finish and download the artifacts!

---

## 🌐 Web API Documentation (Programmatic Build Trigger)

Since this repository runs on GitHub Actions, you can trigger the build programmatically using the official **GitHub REST API**.

**Endpoint:**
```http
POST https://api.github.com/repos/{OWNER}/{REPO}/actions/workflows/build.yml/dispatches
```

**Headers:**
```json
{
  "Authorization": "Bearer YOUR_GITHUB_PERSONAL_ACCESS_TOKEN",
  "Accept": "application/vnd.github.v3+json",
  "X-GitHub-Api-Version": "2022-11-28"
}
```

**JSON Body Payload:**
```json
{
  "ref": "main",
  "inputs": {
    "app_name": "My App",
    "target_url": "https://example.com",
    "icon_url": "https://example.com/logo.png",
    "version_code": "2",
    "version_name": "1.0.1",
    "keystore_base64": "MIIG... (Base64 string of your keystore.jks)",
    "keystore_password": "your_secure_password",
    "key_alias": "key_alias_name",
    "key_password": "your_secure_key_password"
  }
}
```

### 💡 API Integration Example (cURL)
You can trigger this from your own backend (PHP, Python, Node.js) by sending a request like this:

```bash
curl -X POST \
  https://api.github.com/repos/Athar5443/web2apk/actions/workflows/build.yml/dispatches \
  -H "Authorization: Bearer ghp_YOUR_TOKEN_HERE" \
  -H "Accept: application/vnd.github.v3+json" \
  -d '{"ref":"main","inputs":{"app_name":"App","target_url":"https://app.com","icon_url":"","version_code":"1","version_name":"1.0.0","keystore_base64":"...","keystore_password":"...","key_alias":"...","key_password":"..."}}'
```

### 🔐 Note on Persistent Keystores
To ensure your app can be updated on user devices and the Google Play Store, the `keystore_base64`, `keystore_password`, `key_alias`, and `key_password` **MUST remain exactly the same** for every future build of that specific app/domain. If you are building an automated platform, you must store these securely in your database!
