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
 * TODO: Make it possible to connect to multiple devices because it is too slow to always
 * establish a connection before sending a message. The BluetoothClientSocket in Listen thread
 * can apparently .accept() multiple times. Check links and internet. This would however mean that
 * the player which waits on MainActivity gets to be the server because he has the ServerSocket and
 * not the player which is in the setup.
 *
 * -> What to do? After all players know each other -> setup phase is essentially done. We select one player
 * to be the server and he then stays the server. The other players must probably then initiate the
 * connection to this player and we must sometimes forward messages.
 *
 * Another idea would have been that the player which has the puck is the server and establishes all connections
 * to the others and then just sends as soon he knows to whom (this goes then fast). However i think
 * this is not possible because the other players must request the connection to the server player
 * and not the server player his clients. And the other player have no way to know who is now the
 * server player (has gotten the puck) out of the blue. Unless we just broadcast all messages, i.e.
 * when player A plays puck to player B, A informs B and C about that so that A and C know that they
 * now connect to B because he is the next server player.
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
    private byte[] mQueuedBytes = null;
    private int mState = STATE_NONE;

    // Current connection state
    // TODO: This changes also when have multiple connections
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
     * Cancel all threads - Careful also the listening gets stopped
     */
    public void disconnect()
    {
        Log.d(LOGTAG,"Disconnect - Stop all threads");

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
     * Listen for incoming connections - This could be extended to listen for multiple connections
     * -> Consult links & internet
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
        } else Log.d(LOGTAG,"Listen thread already running");

        mState = STATE_LISTEN;
    }

    /**
     * Send bytes to remote device
     * If receiver is null, send to connected device if any, if receiver is not null,
     * establish connection then send.
     * @param receiver  receiver of message
     * @param bytes       Bytes to send
     */
    public void send(BluetoothDevice receiver, byte[] bytes)
    {
        TransmissionThread r;
        synchronized (this) {
            if(mState != STATE_CONNECTED) {
                Log.d(LOGTAG,"Cant send bytes because not connected yet -> connect first");
                connect(receiver);
                mQueuedBytes = bytes;
                return;
            }
            r = mTransmissionThread;
        }
        Log.d(LOGTAG, "Sending bytes");
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

        // Cancel all listen threads
        if (mListenThread != null) {
            mListenThread.cancel();
            mListenThread = null;
        }

        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }

        mTransmissionThread = new TransmissionThread(socket);
        mTransmissionThread.start();
        mState = STATE_CONNECTED;

        // Send queued message if any
        if (mQueuedBytes != null) {
            Log.d(LOGTAG, "Sending queued bytes");
            mTransmissionThread.send(mQueuedBytes);
            mQueuedBytes = null;
            // TODO: Want to disconnect now up until now we just close the app and restart it
        }

        // TODO: This should only be done while pairing
        mListener.onConnected(device);
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
     *
     * TODO: Let this thread listen for multiple connections or have multiple of this threads
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
            Log.d(LOGTAG,"End ListenThread");
        }

        public void cancel()
        {
            Log.d(LOGTAG,"Cancel ListenThread - closing socket");
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
            Log.d(LOGTAG,"Cancel ConnectThread - closing socket");
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
                Log.d(LOGTAG, "Connection lost - Start listening again for incoming connections");
                mState = STATE_LISTEN;
                listen();
                e.printStackTrace();
                return;
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
            Log.d(LOGTAG,"Cancel TransmissionThread - closing socket.");
            try {
                mSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

}
