package com.espressif.espblufi.constants;

import java.util.UUID;

public final class BlufiConstants {
    public static final String BLUFI_PREFIX = "BLUFI";

    public static final UUID UUID_SERVICE = UUID.fromString("0000ffff-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_WRITE_CHARACTERISTIC = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NOTIFICATION_CHARACTERISTIC = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    public static final String KEY_BLE_DEVICE = "key_ble_device";

    public static final String KEY_CONFIGURE_PARAM = "configure_param";

    public static final int DEFAULT_MTU_LENGTH = 128;
    public static final int MIN_MTU_LENGTH = 15;

    public static final int POST_DATA_LENGTH_LESS = 16;

    public static final String PREF_MESH_IDS_NAME = "espblufi_mesh_ids";
}
