# EspBlufi Api说明

------

## BLE连接
- 调用 Android 系统API发起BLE连接，获得 BluetoothGatt
```java
BluetoothGatt gatt;
BluetoothGattCallback gattCallback = ; // 实现Gatt回调, 可参考 BlufiActivity 内部类 GattCallback
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    gatt = bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
} else {
    gatt = bluetoothDevice.connectGatt(context, false, gattCallback);
}
```

- 连接成功后获取 BluetoothGattService
    - UUID 为 0000ffff-0000-1000-8000-00805f9b34fb
- 获得 service 后获取 BluetoothGattCharacteristic
    - APP 向 Device 写数据的 characteristic UUID 为 0000ff01-0000-1000-8000-00805f9b34fb
    - Device 向 APP 推送消息的 characteristic UUID 为 0000ff02-0000-1000-8000-00805f9b34fb， 使用 Notification 方式

## 使用 BlufiClient 与 Device 发起通信
- 实例化 BlufiClient
```java
// BlufiCallback 为抽象类, 回调 Device 通信过程中的数据, 实现您自己的需求, 可参考 BlufiActivity 内部类 BlufiCallbackMain,
BlufiCallback blufiCallback = ;
BlufiClient client = new BlufiClient(gatt, writeCharact, notifyCharact, blufiCallback);
```
```java
// 需要在之前 BluetoothGattCallback 的两个回调方法中调用 client 的方法
// 若没用调用实例 BlufiClient 的这两个方法, 将无法进行数据通信

@Override
public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    client.onCharacteristicWrite(gatt, characteristic, status);
}

@Override
public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    client.onCharacteristicChanged(gatt, characteristic);
}
```

- 设置 Blufi 发送数据时每包数据的最大长度
```java
int limit = 128; // 设置长度限制, 若数据超出限制将进行分包发送
client.setPostPackageLengthLimit(limit)
```

- 与 Device 协商数据加密
```java
client.negotiateSecurity();
```
```java
// 协商结果在 BlufiCallback 回调方法内通知
@Override
public void onNegotiateSecurityResult(BlufiClient client, int status) {
    // status 为 0 表示加密成功, 否则为失败
}
```

- 请求获取设备版本
```java
client.requestDeviceVersion();
```
```java
// 设备版本在 BlufiCallback 回调方法内通知
@Override
public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
    // status 为 0 表示加密成功, 否则为失败
    switch (status) {
        case STATUS_SUCCESS:
            String version = getVersionString(); // 获得版本号
            break;
        default:
            break;
    }
}
```

- 请求获取设备当前扫描到的Wifi信号
```java
client.requestDeviceWifiScan();
```
```java
// 扫描结果在 BlufiCallback 回调方法内通知
@Override
public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
    // status 为 0 表示获取数据成功, 否则为失败
    switch (status) {
        case STATUS_SUCCESS:
            for (BlufiScanResult scanResult : results) {
                String ssid = scanResult.getSsid(); // 获得Wifi SSID
                int rssi = scanResult.getRssi(); // 获得Wifi RSSI
            }
            break;
        default:
            break;
    }
}
```

- 发送自定义数据
```java
byte[] data = "Custom Data".getBytes();
client.postCustomData(data);
```
```java
// 自定义发送结果在 BlufiCallback 回调方法内通知
@Override
public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
    // status 为 0 表示发送成功, 否则为发送失败
    // data 为需要发送的自定义数据
}

// 受到设备端发送的自定义数据
@Override
public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
    // status 为 0 表示成功接收
}
```

- 请求获取设备当前状态
```java
client.requestDeviceStatus();
```
```java
// 设备状态在 BlufiCallback 回调方法内通知
@Override
public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
    // status 为 0 表示获取状态成功, 否则为失败
    switch (status) {
        case STATUS_SUCCESS:
            int opMode = response.getOpMode();
            if (opMode == BlufiParameter.OP_MODE_STA) {
                // 当前为 Station 模式
                int conn = response.getStaConnectionStatus(); // 获取 Device 当前连接状态: 0 表示有wifi连接, 否则没有wifi连接
                String ssid = response.getStaSSID(); // 获取 Device 当前连接的Wifi的SSID
                String bssid = response.getStaBSSID(); // 获取 Device 当前连接的Wifi的BSSID
                String password = response.getStaPassword(); // 获取 Device当前连接的Wifi的密码
            } else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
                // 当前为 SoftAP 模式
                String ssid = response.getSoftAPSSID(); // 获取 Device 的 SSID
                int channel = response.getSoftAPChannel(); // 获取 Device 的信道
                int security = response.getSoftAPSecurity(); // 获取 Device 的加密方式: 0 为不加密, 2 为 WPA, 3 为 WPA2, 4 为 WPA/WPA2
                int connMaxCount = response.getSoftAPMaxConnectionCount(); // 能够连上 Device 的最大个数
                int connCount = response.getSoftAPConnectionCount(); // 当前连接 Device 的个数
            } else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
                // 当前为 Station 和 SoftAP 共存模式
                // 获取状态方法同上 Station 和 SoftAP 模式
            }
            break;
        default:
            break;
    }
}
```

- 对设备进行配网
```java
BlufiConfigureParams params = new BlufiConfigureParams();
int opMode = // 设置需要配置的模式: 1 为Station模式, 2 为 SoftAP模式, 3为Station模式和SoftAP共存模式
params.setOpMode(deviceMode);
if (opMode == BlufiParameter.OP_MODE_STA) {
    // 设置Station配置信息
    params.setStaSSID(ssid); // 设置Wifi SSID
    params.setStaPassword(password); // 设置Wifi密码, 若是开放Wifi则不设或设空
    // 注意: 设备不支持连接5G Wifi, 建议做检查
} else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
    // 设置SoftAP配置信息
    params.setSoftAPSSID(ssid); // 设置Device的SSID
    params.setSoftAPSecurity(security); // 设置Device的加密方式: 0 为不加密, 2 为 WPA, 3 为 WPA2, 4 为 WPA/WPA2
    params.setSoftAPPAssword(password); // 若 security 非0 则设置
    params.setSoftAPChannel(channel); // 设置Device的信道, 可不设
    params.setSoftAPMaxConnection(maxConnection); // 设置可连接Device的最大个数, 可不设

} else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
    // 同上两个
}

client.configure(params);
```
```java
// 设置信息发送结果在 BlufiCallback 回调方法内通知
@Override
public void onConfigureResult(BlufiClient client, int status) {
    // status 为 0 表示发送配置信息成功, 否则为失败
}

// 配置后的状态变化回调
@Override
public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
    // 同上方请求获取设备当前状态
}
```

- 请求 Device 断开BLE连接
```java
// 因为 APP 端主动断开连接有时候 Device 端无法获知
client.requestCloseConnection();
```

- 关闭 BlufiClient
```java
// 释放资源
client.close();
```

## BlufiCallback 说明
```java
// 收到 Device 的 通知数据
// 返回 false 表示处理尚未结束, 交给 BlufiClient 继续后续处理
// 返回 true 表示处理结束, 后续将不再解析该数据, 也不会调用回调方法
public boolean onGattNotification(BlufiClient client, int pkgType, int subType, byte[] data) {
    return false;
}

// BluetoothGatt 关闭时调用
public void onGattClose(BlufiClient client) {
}

// 收到 Device 发出的错误代码
public void onError(BlufiClient client, int errCode) {
}

// 与 Device 协商加密的结果
public void onNegotiateSecurityResult(BlufiClient client, int status) {
}

// 发送配置信息的结果
public void onConfigureResult(BlufiClient client, int status) {
}

// 收到 Device 的版本信息
public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
}

// 收到 Device 的状态信息
public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
}

// 收到 Device 扫描到的Wifi信息
public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
}

// 发送自定义数据的结果
public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
}

// 收到 Device 的自定义信息
public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
}
```
