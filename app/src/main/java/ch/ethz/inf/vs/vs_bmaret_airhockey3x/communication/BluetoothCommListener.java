package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;


import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.Message;

/**
 * Created by Valentin on 14/11/15.
 *
 * Define callbacks for BluetoothComm -> In our case the Activities
 */

public interface BluetoothCommListener {

    void onDeviceFound(String name,String address); // Found new device
    void onReceiveMessage(Message msg);     // Received Message msg
    void onPlayerConnected(int pos, String name);        // Player at pos is now connected
    void onPlayerDisconnected(int pos);     // Player at pos is now disconnected
    void onStartConnecting();               // As soon as start connecting
    void onScanDone();                      // When bluetooth discovery is done
    void onNotDiscoverable();               // When discoverability expired

}
