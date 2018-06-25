# Update Log

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
