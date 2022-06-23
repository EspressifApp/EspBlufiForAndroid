# 修改记录
[English](changelog_en.md)

## 2.3.7
- `BlufiClient` 新增 gatt 写超时接口
  ```java
  public void setGattWriteTimeout(long timeout)
  ```

## 2.3.6
- `targetSdkVersion` 更新至 32
- 更新 gradle 版本
- `sourceCompatibility` 修改为 `JavaVersion.VERSION_1_8`
- `BlufiClient` 增加新接口
  ```java
  public void printDebugLog(boolean enable)
  ```

## 2.3.5
- `targetSdkVersion` 更新至 31

## 2.3.4
- 在 jitpack 上发布库