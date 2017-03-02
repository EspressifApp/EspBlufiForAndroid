package com.espressif.espblufi;


import com.afunx.ble.blelitelib.proxy.BleGattClientProxy;
import com.espressif.espblufi.communication.BlufiCommunicator;

public final class BlufiBridge {
    static final String KEY_CONFIGURE_DATA = "configure_data";
    static final String KEY_DEAUTHENTICATE_DATA = "deauthenticate_data";

    static BleGattClientProxy sBleGattClientProxy;
    static BlufiCommunicator sCommunicator;

    static void release() {
        sBleGattClientProxy = null;
        sCommunicator = null;
    }
}
