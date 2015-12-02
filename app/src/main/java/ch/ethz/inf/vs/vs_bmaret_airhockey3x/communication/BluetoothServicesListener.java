package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothDevice;


/**
 * Created by Valentin on 15/11/15.
 *
 * Define callbacks for the BluetoothServicesListener -> Here BluetoothComm
 *
 */
public interface BluetoothServicesListener {

    void onReceiveBytes(byte[] bytes, int noBytes);
    void onConnected(String deviceAddr);
}
