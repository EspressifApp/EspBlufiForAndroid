package com.espressif.espblufi.constants;

import java.util.UUID;

public final class BlufiConstants {
    public static final String BLUFI_PREFIX = "BLUFI";

    public static final UUID UUID_WIFI_SERVICE = UUID.fromString("0000ffff-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SEND_CHARACTERISTIC = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_READ_CHARACTERISTIC = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    public static final String KEY_PROXY = "key_proxy";
    public static final String KEY_COMMUNICATOR = "key_communicator";
    public static final String KEY_BLE_DEVICES = "key_ble_devices";

    public static final String KEY_CONNECT_TIME = "connect_time";
    public static final String KEY_CONFIGURE_TIME = "configure_time";

    public static final String KEY_CONFIGURE_DATA = "configure_data";
    public static final String KEY_DEAUTHENTICATE_DATA = "deauthenticate_data";

    public static final String KEY_IS_BATCH_CONFIGURE = "is_batch_configure";
    public static final String KEY_CONFIGURE_PARAM = "configure_param";

    public static final String PREF_SETTINGS_NAME = "espblufi_settings";
    public static final String PREF_SETTINGS_KEY_MTU_LENGTH = "espblufi_settings_mtu_length";

    public static final String PREF_MESH_IDS_NAME = "espblufi_mesh_ids";

    /**
     * Some phones such as Xiaomi have some errors if set DEFAULT_MTU_LENGTH too less
     */
    public static final int DEFAULT_MTU_LENGTH = 128;
    public static final int MIN_MTU_LENGTH = 30;

    public static final int POST_DATA_LENGTH_LESS = 16;
}
