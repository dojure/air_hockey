package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Valentin on 15/11/15.
 */
public interface BluetoothServicesListener {

    void onReceiveBytes(byte[] bytes);

    void onConnected(BluetoothDevice device);
}
