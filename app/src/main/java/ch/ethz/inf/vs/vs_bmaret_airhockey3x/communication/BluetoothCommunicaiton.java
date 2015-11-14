package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Valentin on 14/11/15.
 *
 * This is the only class that gets called from without the package. It provides all necessary
 * services to the communication layer clients.
 *
 */

public class BluetoothCommunicaiton {

    // Allow only one listener
    private BluetoothCommunicationListener listener;

    private List<String> devices = new ArrayList<>(); // Change later to BluetoothDevice


    public BluetoothCommunicaiton(BluetoothCommunicationListener lis)
    {
        listener = lis;

        fetchDevices();
    }

    // Scan for devicdes and store into devices field
    private void fetchDevices()
    {
        // TODO: implement properly; this is only a stub

        devices.add("Dev0");
        devices.add("Dev1");
        devices.add("Dev2");
        devices.add("Dev3");
        devices.add("Dev4");
        devices.add("Dev5");
        devices.add("Dev6");
        listener.onDeviceListChanged(devices);
    }

}
