package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import java.util.List;

/**
 * Created by Valentin on 14/11/15.
 *
 */

public interface BluetoothCommunicationListener {

    public void onDeviceListChanged(List<String> deviceNames);

}
