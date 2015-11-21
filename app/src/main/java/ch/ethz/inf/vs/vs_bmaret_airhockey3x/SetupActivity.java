package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.MessageFactory;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Game;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener, BluetoothCommListener {

    final static String LOGTAG = "SetupActivity";

    private Game mGame;
    private BluetoothComm mBC;
    private ListView mDevicesListView;
    private ArrayAdapter<String> mAdapter;
    private ImageButton[] mImageButtons = new ImageButton[3];
    private Player mCurrentPlayer = null;
    private MessageFactory mMF = new MessageFactory();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ImageButton b1 = (ImageButton) findViewById(R.id.player1_btn);
        mImageButtons[0] = b1;
        b1.setOnClickListener(this);
        ImageButton b2 = (ImageButton) findViewById(R.id.player2_btn);
        mImageButtons[1] = b2;
        b2.setOnClickListener(this);
        ImageButton b3 = (ImageButton) findViewById(R.id.player3_btn);
        mImageButtons[2] = b3;
        b3.setOnClickListener(this);

        // DEBUG - Test message button - remove later
        Button b = (Button) findViewById(R.id.test_msg_btn1);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.test_msg_btn3);
        b.setOnClickListener(this);

        // TODO: Ask user how many players
        //showDialog();

        mBC = new BluetoothComm(this, getApplicationContext());
        mBC.scan();

        // Create Game
        // TODO: Do according to what the user wants 2,3,4 players -> Do in the end when everything works for 3
        initGame(3);

        // Initialize the ListView
        // Callback for clicking on ListView
        // Set mCurrentPlayer to the right (the one we clicked on) Bluetooth device
        mDevicesListView = (ListView) findViewById(R.id.devices_list);
        mDevicesListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {

                        String entry = (String) parent.getItemAtPosition(position);
                        if (mCurrentPlayer != null) {
                            mBC.invite(mCurrentPlayer, entry);
                        } else {
                            Log.d(LOGTAG, "mCurrentPlayer is null - cannot request paired device");
                        }

                        // TODO: Somewhere in here it must be checked whether all seats have been filled.
                        // TODO: If this is the case the rest of the connection must be set between non host players
                    }
                });

        setEnableListView(false);
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mBC.unregisterListener(this);
    }

    /**
     * The idea is that the user clicks on one of the other players squares and that he can then
     * select one of the devices on the list. We need then to establish the connection to the other
     * etc..
     *
     * On click on button a new player is initialized and added to the game at the respective position.
     * As long as the button stays selected the corresponding player is stored in mCurrentPlayer.
     * If the user then selects a BluetoothDevice from the now enabled list; The Bluetooth device
     * will be added to the player.
     *
     * To send a test message:
     * 1. click on player you want
     * 2. click on device for the player
     * 3. wait a bit
     * 4. click again on player s.t. he gets unselected
     * 5. press sent message button (If press before it crashes)
     * -> sometimes it crashes for newly paired devices -> try again when already paired
     *
     * Players sit like this
     *    2
     *  1   3
     *    0
     */
    public void onClick(View b)
    {
        // Deselect all others
        for (ImageButton ib : mImageButtons) {
            if(!b.equals(ib)) ib.setSelected(false);
        }
        setEnableListView(true);
        switch (b.getId()) {
            case R.id.player1_btn:
                if (!b.isSelected()) {
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(1));
                } else b.setSelected(false);
                break;
            case R.id.player2_btn:
                if (!b.isSelected()) {
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(2));
                } else b.setSelected(false);
                break;
            case R.id.player3_btn:
                if (!b.isSelected()) {
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(3));
                } else b.setSelected(false);
                break;

            // DEBUG
            case R.id.test_msg_btn1:
                // Send test message to player at position 1
                JSONObject msg = (new MessageFactory()).createMessage(MessageFactory.MOCK_MESSAGE, 0, null);
                mBC.sendMessageToPlayer(msg,mGame.getPlayer(1));
                break;
            case R.id.test_msg_btn3:
                // Send test message to player at position 3
                JSONObject msg1 = (new MessageFactory()).createMessage(MessageFactory.MOCK_MESSAGE, 0, null);
                mBC.sendMessageToPlayer(msg1, mGame.getPlayer(3));
                break;
        }
        // Check if no button is selected -> need to disable list if none is and invalidate current player
        boolean sel = false;
        for (ImageButton ib : mImageButtons) {
            if(ib.isSelected()) sel = true;
        }
        if (!sel) {
            setEnableListView(false);
            setCurrentPlayer(null);
        }
    }

    /**
     *
     * BluetoothComm callbacks
     *
     */


    // Populate listview as soon as devices found or changed
    public void onDeviceFound(String name)
    {
        if (mAdapter == null) {
            // First call -> initialize Listadapter
            List<String> names = new ArrayList<>();
            names.add(name);
            mAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);
            mDevicesListView.setAdapter(mAdapter);
        } else {
            // Use Listadapter which is already initialized
            mAdapter.add(name);
            mAdapter.notifyDataSetChanged();
        }
    }


    public void onReceiveMessage(JSONObject msg)
    {
        // For DEBUG purposes just display an alert saying that we got a message
        final JSONObject finalMsg = msg;
        if (msg != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(SetupActivity.this).create();
                    alertDialog.setTitle("DEBUG");
                    alertDialog.setMessage("Got a message !! Receiver at pos: "
                            + Integer.toString(mMF.getSender(finalMsg)) + " Message type: " +
                            mMF.getType(finalMsg));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });

        } else Log.d(LOGTAG, "Message was null");
    }


    /**
     *
     * Helper functions
     *
     */

    // TODO: Shows dialog where user can decide how many players want to participate
    private void showDialog()
    {
        AlertDialog.Builder dialog  = new AlertDialog.Builder(this);
        dialog.setMessage(getString(R.string.pl_holder_no_participants));
        dialog.setTitle("PLACEHODLER");
        dialog.setPositiveButton("OK", null);
        dialog.setCancelable(true);
        dialog.create().show();
    }

    // Enable/disable mDevicesListView. Need to do the fading manually
    private void setEnableListView(boolean enable)
    {
        mDevicesListView.setEnabled(enable);
        if (enable) mDevicesListView.setAlpha((float) 1);
        else mDevicesListView.setAlpha((float) 0.5);
    }

    /**
     * Sets current player. A player is considered 'current' iff the corresponding button is selected
     * Disconnect devices of all when none is current.
     * @param p     Current player
     */
    private void setCurrentPlayer(Player p)
    {
        if (p != null) Log.d(LOGTAG,"Setting current player: " + Integer.toString(p.getPosition()));
        else Log.d(LOGTAG, "Set current player to null");
        if (mCurrentPlayer != null && !mCurrentPlayer.equals(p)) mBC.disconnect();
        mCurrentPlayer = p; // Even if null
    }

    /**
     * Initializes game with given number of players
     * @param nrPlayers Number of players that take part in game
     */
    private void initGame(int nrPlayers)
    {
        mGame = Game.getInstance();
        mGame.setNrPlayer(nrPlayers);
        mGame.addPlayer(new Player(0)); // Self
        switch (nrPlayers) {
            case 2:
                mGame.addPlayer(new Player(2));
                break;
            case 3:
                mGame.addPlayer(new Player(1));
                mGame.addPlayer(new Player(3));
                break;
            case 4:
                mGame.addPlayer(new Player(1));
                mGame.addPlayer(new Player(2));
                mGame.addPlayer(new Player(3));
                break;
        }
    }

}
