package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Valentin on 14/11/15.
 *
 * This is the only class that gets called from without the package. It provides all necessary
 * services to the communication layer clients.
 *
 */

public class BluetoothComm {

    // Constants
    private final String LOGTAG = "BluetoothComm";

    private BluetoothCommListener listener; // Allow only one listener
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private Boolean scanning = false;

    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<BluetoothDevice> pairedDevices = new ArrayList<>();



    public BluetoothComm(BluetoothCommListener lis, Context appContext)
    {
        listener = lis;

        // Get Bluetooth adapter
        context = appContext;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.d(LOGTAG,"Bluetooth not enabled");

            // TODO: Show this in Activity somehow
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Add already paired devices - maybe do more intelligently
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice d : pairedDevices) {
            Log.d(LOGTAG, "Device already paired: " + d.getName());

            // Should we only consider devices with nonnull name??
            if (d != null && d.getName() != null) {
                devices.add(d);
                pairedDevices.add(d);
                listener.onDeviceFound(d.getName());
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver,filter);
        scan();
    }


    // Let client unregister - 'this' is not needed anymore -> do cleanup
    public void unregisterListener(BluetoothCommListener lis)
    {
        if (listener == lis) { // Must be same listener of course
            listener = null;
            context.unregisterReceiver(receiver);
        }
    }


    // Listener to device scan
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(LOGTAG,"Found device " + device.getName());
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED && !devices.contains(device)) {

                    // Consider only devices with nonnull name??
                    if (device.getName() != null) {
                        devices.add(device);
                        listener.onDeviceFound(device.getName());
                    }
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Scan done
                scanning = false;
            }
        }
    };

    // Scan for devices and store into devices field
    private void scan()
    {
        if(!scanning) {
            scanning = true;
            bluetoothAdapter.startDiscovery();
        }
    }


}
