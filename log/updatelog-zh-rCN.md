[[English]](updatelog-en.md)

# 更新日志

## v1.4.3
- 将 blufilibrary 模块改为完全独立的模块, 删除 apptools 模块
- 将 targetSdkVersion 改为 Android Q(29)

# v1.4.2
- 设置界面内增加 APP 检查更新功能

## v1.4.1
- BluFi 回调方法中返回具体的错误码，可参考类 BlufiCallback

## v1.4.0
- 增加中文
- 修复设备端MTU过小的情况下可能出现丢包的Bug
- 优化通信过程中数据的组包和解包效率

## v1.3.2
- 修复无法配网至使用非UTF-8字符编码SSID的路由器的问题

## 2018/6/25
- BluFi通信库开源, 见模块blufilibrary
- 删除BlufiCommunicator

## v1.3.1
- 修复一些bug

## v1.3.0
- 新交互UI
- 重构BluFi接口, 设备信息推送实用监听回调
- 类BlufiClient的用法详见类BlufiActivity

## v1.2.7
- ESP-IDF不支持SoftAP WEP加密, 去除相关选项

## v1.2.6
- 类BlufiCommunicator增加接口
- 获取Blufi设备扫描到的wifi列表，调用 getWifiList()
- APP向设备发送自定义数据，调用 postCustomData(byte[] data)
- APP获取设备推送的自定义数据，调用 receiveCustomdata(long timeout)

## v1.2.5
- 类BlufiCommunicator增加通知Blufi断开连接的接口

## v1.2.4
- 优化BLE设备扫描
- 可在设置界面中设置BLE设备扫描前缀过滤
- 配网过程可查看详细配网过程打印
- 储存配过网的路由器密码并在下次配网设置时自动填充密码
- 类BlufiCommunicator增加获取Blufi当前状态接口

## v1.2.3
- 取消在BLE扫描界面点击后直接连接设备
- 增加配置Station选项：是否等待设备连上wifi的反馈消息

## v1.2.2
- 重构BLE库
- 增加配网选项：多线程批量配网

## v1.1.1
- 修正配网速度过慢的问题

## v1.1.0
- 增加批量配网功能
