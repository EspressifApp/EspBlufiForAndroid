# EspBlufiForAndroid
This is a demo app to control the ESP device which run [BluFi](https://github.com/espressif/esp-idf/tree/master/examples/bluetooth/blufi)

## Lib Source Code
- See [lib-blufi](lib-blufi)

## How to Import
- Add this in your root `build.gradle` at the end of repositories:
  ```
  allprojects {
      repositories {
          ...
          maven { url 'https://jitpack.io' }
      }
  }
   ```
- And add a dependency code to your app module's `build.gradle` file.
  ```
  implementation 'com.github.EspressifApp:lib-blufi-android:2.3.7'
  ```

## ESPRSSIF MIT License
- See [License](LICENSE)

## Development Documents
- See [Doc](doc/Introduction_to_the_EspBlufi_API_Interface_for_Android__en.md)

## Release APKS
- See [Releases](https://github.com/EspressifApp/EspBlufiForAndroid/releases)

## Update Log
- App [Log](log/updatelog-en.md)
- Lib [Log](lib-blufi/log/changelog_en.md)
