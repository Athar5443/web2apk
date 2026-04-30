# Dynamic web2apk Template

This template allows you to generate a custom Android APK via GitHub Actions.

## Features
- **Dynamic App Name:** Set your app's display name.
- **Dynamic Package Name:** Automatically generated as `athars.<appname>`.
- **Dynamic URL:** Specify the website to load in the WebView.
- **Custom Icon:** Provide a URL to a PNG/JPG image to use as the app icon.

## How to use via GitHub UI
1. Go to the **Actions** tab in this repository.
2. Select the **Build Dynamic APK** workflow.
3. Click **Run workflow**.
4. Fill in the `App Name`, `Target URL`, and `Icon URL`.
5. Wait for the build to finish and download the APK from the artifacts.

## How to use via API
Check the `api/trigger.js` file for a Node.js example on how to trigger builds programmatically.

### Security
**Do not share your GitHub Personal Access Token.** Always use environment variables in your production API.
