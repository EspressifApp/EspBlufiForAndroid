package libs.espressif.location;

import android.content.Context;
import android.location.LocationManager;

public class LocationUtils {
    public static boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        boolean locationGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean locationNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return locationGPS || locationNetwork;
    }
}
