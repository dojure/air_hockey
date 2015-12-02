package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import org.json.JSONObject;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

/**
 * Created by Valentin on 14/11/15.
 *
 * Define callbacks for BluetoothComm -> In our case the Activities
 *
 */

public interface BluetoothCommListener {

    void onDeviceFound(String name,String address);
    void onReceiveMessage(JSONObject msg);
    void onPlayerConnected(int pos);

}
