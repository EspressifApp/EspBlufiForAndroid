[[简体中文]](updatelog-zh-rCN.md)

# Update Log

## v1.5.3
- Fix some bugs

## v1.5.2
- Fixed an issue where threads could lock up when calling BlufiClient.close()
- A bug will occur if we call BluetoothGatt.requestMTU(int) after connecting BLE on Samsung Android 10, so we cancel call it

## v1.5.1
- Fix BluFi packet length

## v1.5.0
- Update BlufiClient APIs
- Update development [doc](../doc)
- Change minSdkVersion to 21 (Android 5.0)

## v1.4.3
- Make blufilibrary module fully independent, delete apptools module
- Change targetSdkVersion to Android Q(29)

## v1.4.2
- Add app upgrade option in Settings

## v1.4.1
- Add error code in BluFi callback function，see class BlufiCallback

## v1.4.0
- Add Chinese
- Fix bug on the device which MTU length is too short
- Optimize the Blufi data generate and parse

## v1.3.2
- Fix configure network failed if AP using non-UTF8 character encoding SSID

## 6/25/2018
- Blufi source code opened, see module blufilibrary
- Delete BlufiCommunicator

## v1.3.1
- Fix some bugs

## v1.3.0
- New UI
- New Blufi lib interface. See class BlufiClient
- Usage of class BlufiClient see class BlufiActivity

## v1.2.7
- ESP-IDF don't support SoftAP security WEP, remove the options

## v1.2.6
- Add some functions in class BlufiCommunicator
- Get blufi device scanned wifi list, call getWifiList()
- APP post custom data, call postCustomData(byte[] data)
- APP receive custom notification from device, call receiveCustomdata(long timeout)

## v1.2.5
- Add function closeConnection() in class BlufiCommunicator

## v1.2.4
- Optimizing the scanning of BLE devices
- Set ble device name prefix filter in Settings
- Add configure print
- Save configured AP password
- Add function getStatus() in class BlufiCommunicator

## v1.2.3
- Cancel connect ble when select the device
- Add configure station option: Wait connect response

## v1.2.2
- BLE lib refactoring
- Add configure option: Multithread configure

## v1.1.1
- Fix configure too slow

## v1.1.0
- Add batch configure
