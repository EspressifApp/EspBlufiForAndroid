# EspBlufiForAndroid
This is a demo app to control the ESP device which run [BluFi](https://github.com/espressif/esp-idf/tree/master/examples/bluetooth/bluedroid/ble/blufi)

## Lib Source Code
- See [lib-blufi-android](https://github.com/EspressifApp/lib-blufi-android)

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
  implementation 'com.github.EspressifApp:lib-blufi-android:2.3.4'
  ```

## ESPRSSIF MIT License
- See [License](ESPRESSIF_MIT_LICENSE)

## Development Documents
- See [Doc](doc/Introduction_to_the_EspBlufi_API_Interface_for_Android__en.md)

## Release APKS
- See [Releases](https://github.com/EspressifApp/EspBlufiForAndroid/releases)

## Update Log
- See [Log](log/updatelog-en.md)
