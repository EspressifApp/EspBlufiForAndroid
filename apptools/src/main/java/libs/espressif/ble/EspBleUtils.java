package libs.espressif.ble;

import java.util.UUID;

public class EspBleUtils {
    private static final String UUID_INDICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String BASE_UUID_FORMAT = "0000%s-0000-1000-8000-00805f9b34fb";

    public static UUID newUUID(String address) {
        String string = String.format(BASE_UUID_FORMAT, address);
        return UUID.fromString(string);
    }

    public static UUID getIndicationDescriptorUUID() {
        return UUID.fromString(UUID_INDICATION_DESCRIPTOR);
    }
}
