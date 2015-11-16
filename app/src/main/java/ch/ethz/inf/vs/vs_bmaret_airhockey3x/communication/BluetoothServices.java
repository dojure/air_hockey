package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Valentin on 15/11/15.
 *
 * This class handles all the connections with Bluetooth devices. This includes
 * connecting to devices, listening for incoming connections and transmitting data.
 *
 */
public class BluetoothServices {

    private final String LOGTAG = "BluetoothServices";

    // Unique UUID for this application - ??
    private static final UUID MUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final String NAME = "AirHockey3X";

    private BluetoothServicesListener mListener; // Allow only one mListener
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private TransmissionThread mTransmissionThread;
    private ListenThread mListenThread;
    private int mState = STATE_NONE;

    // Current connection state
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothServices(BluetoothServicesListener listener)
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mListener = listener;
    }

    /**
     * Cancel all threads
     */
    public void stopAll()
    {
        Log.d(LOGTAG,"Stop all threads");

        // Cancel any threads currently trying to establish connection to other
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }
        // Cancel all threads that are listening
        if (mListenThread != null) {
            mListenThread.cancel();
            mListenThread = null;
        }
        mState = STATE_NONE;
    }

    // Let client unregister
    public void unregisterListener(BluetoothServicesListener listener)
    {
        if (mListener == listener) { // Must be same mListener of course
            mListener = null;
        }
    }

    /**
     * Listen for incoming connections
     */
    public void listen()
    {
        Log.d(LOGTAG,"Start listening for connections");

        // Cancel any threads currently trying to establish connection to other
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }

        if (mListenThread == null) {
            mListenThread = new ListenThread();
            mListenThread.start();
        }
        mState = STATE_LISTEN;
    }

    /**
     * Send bytes to connected device
     * @param bytes Bytes to send
     */
    public void send(byte[] bytes)
    {
        TransmissionThread r;
        synchronized (this) {
            if(mState != STATE_CONNECTED) return;
            r = mTransmissionThread;
        }
        r.send(bytes); // Send unsnchronized

    }

    /**
     * Initiate new connection to remote device.
     * @param device    Device to connect to
     */
    public synchronized void connect(BluetoothDevice device)
    {
        Log.d(LOGTAG,"Attempting to connect to " + device.getName());

        // Cancel any threads currently trying to establish connection to other
        if (mState == STATE_CONNECTING && mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
        }

        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        mState = STATE_CONNECTING;
    }

    /**
     * Start handling connection
     * @param socket    Socket on which the connection was made
     * @param device    Device to wich to send
     */
    public synchronized void transmit(BluetoothSocket socket, BluetoothDevice device)
    {
        Log.d(LOGTAG,"Connected");

        // Cancel any threads currently trying to establish connection to other
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }

        mTransmissionThread = new TransmissionThread(socket);
        mTransmissionThread.start();
        mState = STATE_CONNECTED;

        mListener.onConnected(device); // Notify listener about connected device
    }

    private void connectionFailed()
    {
        Log.d(LOGTAG,"Connection failed");
    }


    /**
     *
     * Threads
     *
     */


    /**
     * Thread that listens for incoming connections
     */
    private class ListenThread extends Thread {

        private final BluetoothServerSocket mServerSocket;

        public ListenThread()
        {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MUUID);
            } catch (IOException e) {e.printStackTrace();}
            mServerSocket = tmp;
        }

        public void run()
        {
            Log.d(LOGTAG,"Begin ListenThread");

            BluetoothSocket socket = null;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }


                if(socket != null) {
                    Log.d(LOGTAG, "Successfully connected to " + socket.getRemoteDevice().getName());

                    synchronized (BluetoothServices.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // All ok
                                transmit(socket,socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Not ready or already connected
                                try {
                                    socket.close();
                                } catch (IOException e) {e.printStackTrace();}
                        }
                    }
                }

            }
        }

        public void cancel()
        {
            try {
                mServerSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }


    /**
     * Thread to make connection with remote device.
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device)
        {
            mDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with device
            try {
                tmp = device.createRfcommSocketToServiceRecord(MUUID);
            } catch (IOException e) {e.printStackTrace();}
            mSocket = tmp;
        }

        public void run()
        {
            Log.d(LOGTAG,"Begin ConnectThread");

            // Cancel discovery if still going on
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                connectionFailed();
                // Close the socket
                try {
                    mSocket.close();
                } catch (IOException e2) {e.printStackTrace();}
                // Start the service over to restart listening mode
                //BluetoothServices.this.start();


                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothServices.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            transmit(mSocket, mDevice);
        }

        public void cancel()
        {
            Log.d(LOGTAG,"Cancel ConnectThread");
            try {
                mSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }

    }

    /**
     * This thread handles all incoming and outgoing transmissions
     */
    private class TransmissionThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream mIn;
        private final OutputStream mOut;



        public TransmissionThread(BluetoothSocket socket)
        {
            mSocket = socket;

            // Set in and output stream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {e.printStackTrace();}

            mIn = tmpIn;
            mOut = tmpOut;
        }

        public void run()
        {
            Log.d(LOGTAG,"Start TransmissionThread");

            byte[] buf = new byte[1024]; // How much??

            // Receiving
            while(true) {
            try {
                int n = mIn.read(buf);
                synchronized (mListener) {
                    mListener.onReceiveBytes(buf, n); // Tell listener about received bytes
                }
            } catch (IOException e) {
                // TODO: Handle error while receiving like connection loss
                e.printStackTrace();
            }

            }

        }

        /**
         * Send bytes to remote device
         * @param buf   Bytes to send
         */
        public void send(byte[] buf)
        {
            Log.d(LOGTAG, "Sending bytes to remote device");
            try {
                mOut.write(buf);
            } catch (IOException e) {e.printStackTrace();}
        }

        public void cancel()
        {
            Log.d(LOGTAG,"Cancel TransmissionThread");
            try {
                mSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

}
