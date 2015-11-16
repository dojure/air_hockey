package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothDevice;

import org.json.JSONObject;

/**
 * Created by Valentin on 15/11/15.
 */
public interface BluetoothServicesListener {

    void onReceiveBytes(byte[] bytes, int noBytes);

    void onConnected(BluetoothDevice device);
}
