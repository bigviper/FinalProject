package patryk.zmijewski.polsl.pl.finalproject.model.sensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.neurosky.connection.TgStreamReader;

/**
 * Created by Patryk Żmijewski on 26.12.2016.
 */

public class SensorConnector {
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private String address = null;
}
