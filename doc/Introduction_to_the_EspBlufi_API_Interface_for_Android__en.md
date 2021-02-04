[[cn]](Introduction_to_the_EspBlufi_API_Interface_for_Android__cn.md)

# Introduction to the EspBlufi API Interface for Android
------
This guide is a basic introduction to the APIs provided by Espressif to facilitate the customers' secondary development of BluFi.

## Communicate with the device using BlufiClient

- Create a BlufiClient instance

    ```java
    BlufiClient client = new BlufiClient(context, device);

    // BlufiCallback is declared as an abstract class, which can be used to notify the app of the data sent by the device. Please refer to the BlufiCallbackMain inner class in BlufiActivity.
    BlufiCallback blufiCallback = ;
    client.setBlufiCallback(blufiCallback);
    
    // Gatt system callback
	BluetoothGattCallback gattCallback = ;
	client.setGattCallback(gattCallback);
    ```

- Configure the maximum length of each data packet

    ```java
    int limit = 128; // Configure the maximum length of each post packet. If the length of a post packet exceeds the maximum packet length, the post packet will be split into fragments.
    client.setPostPackageLengthLimit(limit);
    ```

- Establish a BLE connection

	```java
	// If establish connection successfully，client will discover service and characteristic for Blufi
	// client can communicate with Device after receive callback function 'onGattPrepared' in BlufiCallback
	client.connect();
	```

- Negotiate data security with the device

    ```java
    client.negotiateSecurity();
    ```

    ```java
    // The result of negotiation will be sent back by the BlufiCallback function
    @Override
    public void onNegotiateSecurityResult(BlufiClient client, int status) {
        // status is the result of negotiation: "0" - successful, otherwise - failed.
    }
    ```

- Request Device Version

    ```java
    client.requestDeviceVersion();
    ```
    ```java
    // The device version is notified in the BlufiCallback function
    @Override
    public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
        // status is the result of encryption: "0" - successful, otherwise - failed.

        switch (status) {
            case STATUS_SUCCESS:
                String version = getVersionString(); // Get the version number
                break;
            default:
                break;
        }
    }
    ```

- Request the device's current Wi-Fi scan result

    ```java
    client.requestDeviceWifiScan();
    ```

    ```java
    // The scan result is notified in the BlufiCallback function
    @Override
    public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
        // status is the result: "0" - successful, otherwise - failed.
        
        switch (status) {
            case STATUS_SUCCESS:
                for (BlufiScanResult scanResult : results) {
                    String ssid = scanResult.getSsid(); // Obtain Wi-Fi SSID
                    int rssi = scanResult.getRssi(); // Obtain Wi-Fi RSSI
                }
                break;
            default:
                break;
        }
    }
    ```

- Send custom data

    ```java
    byte[] data = "Custom Data".getBytes();
    client.postCustomData(data);
    ```
        
    ```java
    // The result of sending customer data is notified in the BlufiCallback function
    @Override
    public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
        // status is the result: "0" - successful, otherwise - failed.

        // data is the custom data to be sent
    }

    // Receive customer data sent by the device
    @Override
    public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
        // status is the result: "0" - successful, otherwise - failed.
        // data is the custom data received
    }
    ```

- Request the current status of the device

    ```java
    client.requestDeviceStatus();
    ```

    ```java
    // The device status is notified in the BlufiCallback function.
    @Override
    public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
        // status is the result: "0" - successful, otherwise - failed.

        switch (status) {
            case STATUS_SUCCESS:
                int opMode = response.getOpMode();
                if (opMode == BlufiParameter.OP_MODE_STA) {
                    // Station mode is currently enabled.
                    int conn = response.getStaConnectionStatus(); // Obtain the current status of the device: "0" - Wi-Fi connection established, otherwise - no Wi-Fi connection.
                    String ssid = response.getStaSSID(); // Obtain the SSID of the current Wi-Fi connection
                    String bssid = response.getStaBSSID(); // Obtain the BSSID of the current Wi-Fi connection
                    String password = response.getStaPassword(); // Obtain the password of the current Wi-Fi connection
                } else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
                    // SoftAP mode is currently enabled
                    String ssid = response.getSoftAPSSID(); // Obtain the device SSID
                    int channel = response.getSoftAPChannel(); // Obtain the device channel
                    int security = response.getSoftAPSecurity(); // Obtain the security option of the device: "0" - no security, "2" - WPA, "3" - WPA2, "4" - WPA/WPA2.
                    int connMaxCount = response.getSoftAPMaxConnectionCount(); // The number of maximum connections
                    int connCount = response.getSoftAPConnectionCount(); // The number of existing connections
                } else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
                    // Station/SoftAP mode is currently enabled
                    // Similar to Station and SoftAP modes
                }
                break;
            default:
                break;
        }
    }
    ```

- Configure the device

    ```java
    BlufiConfigureParams params = new BlufiConfigureParams();
    int opMode = // Configure the device mode: "1" - Station, "2" - SoftAP, "3" - Station/SoftAP.
    params.setOpMode(deviceMode);
    if (opMode == BlufiParameter.OP_MODE_STA) {
        // Configure the device in Station mode
        params.setStaSSID(ssid); // Configure the Wi-Fi SSID
        params.setStaPassword(password); // Configure the Wi-Fi password. For public Wi-Fi networks, this option can be ignored or configured to return void.
        // Note: 5G Wi-Fi is not supported by the device. Please have a look.
    } else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
        // Configure Device in SoftAP
        params.setSoftAPSSID(ssid); // Configure the device SSID
        params.setSoftAPSecurity(security); // Configure the security option of the device: "0" - no security, "2" - WPA, "3" - WPA2, "4" - WPA/WPA2.
        params.setSoftAPPAssword(password); // This option is mandatory, if the security option value is not "0".
        params.setSoftAPChannel(channel); // Configure the device channel
        params.setSoftAPMaxConnection(maxConnection); // Configure the number of maximum connections for the device

    } else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
        // Similar to Station and SoftAP modes
    }

    client.configure(params);
    ```

    ```java
    // The result of data sending obtained with the BlufiCallback function
    @Override
    public void onPostConfigureParams(BlufiClient client, int status) {
        // status is the result: "0" - successful, otherwise - failed.
    }

    // Indicate the change of status after the configuration
    @Override
    public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
        // Request the current status of the device
    }
    ```

- Request the device to break BLE connection
    ```java
    // If the app breaks a connection with the device and the device has not detected this fact, the app has no means to re-establish the connection.
    client.requestCloseConnection();
    ```

- Close BlufiClient
    ```java
    // Release resources
    client.close();
    ```

## Notes on BlufiCallback

```java
// Discover Gatt service over
// Discover failed if service, writeChar or notifyChar is null
public void onGattPrepared(BlufiClient client, BluetoothGatt gatt, BluetoothGattService service, BluetoothGattCharacteristic writeChar, BluetoothGattCharacteristic notifyChar) {
}

// Receive the notification of the device
// false indicates that the processing has not been completed yet and that the procewssing will be transferred to BlufiClient.
// true indicates that the processing has been completed and there will be no data processing or function calling afterwards.
public boolean onGattNotification(BlufiClient client, int pkgType, int subType, byte[] data) {
    return false;
}

// Error code sent by the device
public void onError(BlufiClient client, int errCode) {
}

// The result of the security negotiation with the device
public void onNegotiateSecurityResult(BlufiClient client, int status) {
}

// The result of the device configuration
public void onConfigureResult(BlufiClient client, int status) {
}

// Information on the received device version
public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
}

// Information on the received device status
public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
}

// Information on the received device Wi-Fi scan
public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
}

// The result of sending custom data
public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
}

// The received custom data from the device
public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
}
```
