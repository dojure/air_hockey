package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication;

import android.bluetooth.BluetoothDevice;


/**
 * Created by Valentin on 15/11/15.
 *
 * Define callbacks for the BluetoothServicesListener -> Here BluetoothComm
 *
 */
public interface BluetoothServicesListener {

    void onReceiveBytes(byte[] bytes, int noBytes); // Pass on bytes that were received
    void onConnected(String deviceAddr);            // Connected to device with given address
}
