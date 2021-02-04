[[en]](Introduction_to_the_EspBlufi_API_Interface_for_Android__en.md)

# EspBlufi for Android API 接口说明

------

为了方便用户进行 Blufi 的二次开发，我司针对 EspBlufi for Android 提供了一些 API 接口。本文档将对这些接口进行简要介绍。

## 使用 BlufiClient 与 Device 发起通信

- 实例化 BlufiClient
	
	```java
	BlufiClient client = new BlufiClient(context, device);

	// BlufiCallback 为抽象类，回调 Device 通信过程中的数据，实现您自己的需求，可参考 BlufiActivity 的内部类 BlufiCallbackMain
	BlufiCallback blufiCallback = ;
	client.setBlufiCallback(blufiCallback);

	// Gatt 系统回调
	BluetoothGattCallback gattCallback = ;
	client.setGattCallback(gattCallback);
	```

- 设置 Blufi 发送数据时每包数据的最大长度

	```java
	int limit = 128; // 设置长度限制，若数据超出限制将进行分包发送
	client.setPostPackageLengthLimit(limit)
	```

- 与 Device 建立连接

	```java
	// 若 client 与设备建立连接，client 将主动扫描 Blufi 的服务和特征
	// 用户在收到 BlufiCallback 回调 onGattPrepared 后才可以与设备发起通信
	client.connect();
	```

- 与 Device 协商数据加密
	
	```java
	client.negotiateSecurity();
	```

	```java
	// 协商结果在 BlufiCallback 回调方法内通知
	@Override
	public void onNegotiateSecurityResult(BlufiClient client, int status) {
	    // status 为 0 表示加密成功，否则为失败
	}
	```

- 请求获取 Device 版本

	```java
	client.requestDeviceVersion();
	```
	
	```java
	// 设备版本在 BlufiCallback 回调方法内通知
	
	@Override
	public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
	    // status 为 0 表示加密成功，否则为失败
	
	    switch (status) {
	        case STATUS_SUCCESS:
	            String version = getVersionString(); // 获得版本号
	            break;
	        default:
	            break;
	    }
	}
	```

- 请求获取 Device 当前扫描到的 Wi-Fi 信号
	
	```java
	client.requestDeviceWifiScan();
	```
	
	```java
	// 扫描结果在 BlufiCallback 回调方法内通知
	public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
	    // status 为 0 表示获取数据成功，否则为失败
	    
	    switch (status) {
	        case STATUS_SUCCESS:
	            for (BlufiScanResult scanResult : results) {
	                String ssid = scanResult.getSsid(); // 获得 Wi-Fi SSID
	                int rssi = scanResult.getRssi(); // 获得 Wi-Fi RSSI
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
	// 自定义数据发送结果在 BlufiCallback 回调方法内通知
	@Override
	public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
	    // status 为 0 表示发送成功，否则为发送失败
	
	    // data 为需要发送的自定义数据
	}
	
	// 收到 Device 端发送的自定义数据
	@Override
	public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
	    // status 为 0 表示成功接收
	    // data 为收到的数据
	}
	```

- 请求获取 Device 当前状态

	```java
	client.requestDeviceStatus();
	```
	
	```java
	// Device 状态在 BlufiCallback 回调方法内通知
	@Override
	public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
	    // status 为 0 表示获取状态成功，否则为失败
	
	    switch (status) {
	        case STATUS_SUCCESS:
	            int opMode = response.getOpMode();
	            if (opMode == BlufiParameter.OP_MODE_STA) {
	                // 当前为 Station 模式
	                int conn = response.getStaConnectionStatus(); // 获取 Device 当前连接状态：0 表示有 Wi-Fi 连接，否则没有 Wi-Fi 连接
	                String ssid = response.getStaSSID(); // 获取 Device 当前连接的 Wi-Fi 的 SSID
	                String bssid = response.getStaBSSID(); // 获取 Device 当前连接的 Wi-Fi 的 BSSID
	                String password = response.getStaPassword(); // 获取 Device 当前连接的 Wi-Fi 密码
	            } else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
	                // 当前为 SoftAP 模式
	                String ssid = response.getSoftAPSSID(); // 获取 Device 的 SSID
	                int channel = response.getSoftAPChannel(); // 获取 Device 的信道
	                int security = response.getSoftAPSecurity(); // 获取 Device 的加密方式：0 为不加密，2 为 WPA，3 为 WPA2，4 为 WPA/WPA2
	                int connMaxCount = response.getSoftAPMaxConnectionCount(); // 最多可连接的 Device 个数
	                int connCount = response.getSoftAPConnectionCount(); // 当前已连接 的 Device 个数
	            } else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
	                // 当前为 Station 和 SoftAP 共存模式
	                // 获取状态方法同 Station 模式和 SoftAP 模式
	            }
	            break;
	        default:
	            break;
	    }
	}
	```

- 对 Device 进行配网
	
	```java
	BlufiConfigureParams params = new BlufiConfigureParams();
	int opMode = // 设置需要配置的模式：1 为 Station 模式，2 为 SoftAP 模式，3 为 Station 和 SoftAP 共存模式
	params.setOpMode(deviceMode);
	if (opMode == BlufiParameter.OP_MODE_STA) {
	    // 设置 Station 配置信息
	    params.setStaSSID(ssid); // 设置 Wi-Fi SSID
	    params.setStaPassword(password); // 设置 Wi-Fi 密码，若是开放 Wi-Fi 则不设或设空
	    // 注意：Device 不支持连接 5G Wi-Fi，建议提前检查一下是不是 5G Wi-Fi
	} else if (opMode == BlufiParameter.OP_MODE_SOFTAP) {
	    // 设置 SoftAP 配置信息
	    params.setSoftAPSSID(ssid); // 设置 Device 的 SSID
	    params.setSoftAPSecurity(security); // 设置 Device 的加密方式：0 为不加密，2 为 WPA，3 为 WPA2，4 为 WPA/WPA2
	    params.setSoftAPPAssword(password); // 若 Security 非 0 则必须设置 
	    params.setSoftAPChannel(channel); // 设置 Device 的信道, 可不设
	    params.setSoftAPMaxConnection(maxConnection); // 设置可连接 Device 的最大个数
	
	} else if (opMode == BlufiParameter.OP_MODE_STASOFTAP) {
	    // 同上两个
	}
	
	client.configure(params);
	```
	
	```java
	// 设置信息发送结果在 BlufiCallback 回调方法内通知
	@Override
	public void onPostConfigureParams(BlufiClient client, int status) {
	    // Status 为 0 表示发送配置信息成功，否则为失败
	}
	
	// 配置后的状态变化回调
	@Override
	public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
	    // 同上方请求获取设备当前状态
	}
	```

- 请求 Device 断开 BLE 连接
	
	```java
	// 有时 Device 端无法获知 app 端已主动断开连接, 此时会导致 app 后续无法重新连上设备
	client.requestCloseConnection();
	```

- 关闭 BlufiClient Close BlufiClient
	
	```java
	// 释放资源
	client.close();
	```

## BlufiCallback 说明

```java
// 当扫描 Gatt 服务结束后调用该方法
// service, writeChar, notifyChar 中有 null 的时候表示扫描失败
public void onGattPrepared(BlufiClient client, BluetoothGatt gatt, BluetoothGattService service, BluetoothGattCharacteristic writeChar, BluetoothGattCharacteristic notifyChar) {
}

// 收到 Device 的通知数据
// 返回 false 表示处理尚未结束，交给 BlufiClient 继续后续处理
// 返回 true 表示处理结束，后续将不再解析该数据，也不会调用回调方法
public boolean onGattNotification(BlufiClient client, int pkgType, int subType, byte[] data) {
	return false;
}

// 收到 Device 发出的错误代码
public void onError(BlufiClient client, int errCode) {
}

// 与 Device 协商加密的结果
public void onNegotiateSecurityResult(BlufiClient client, int status) {
}

// 发送配置信息的结果
public void onPostConfigureParams(BlufiClient client, int status) {
}

// 收到 Device 的版本信息
public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
}

// 收到 Device 的状态信息
public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
}

// 收到 Device 扫描到的 Wi-Fi 信息
public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
}

// 发送自定义数据的结果
public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
}

// 收到 Device 的自定义信息
public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
}
```
