package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import org.json.JSONException;
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

        // Test message button - remove later
        Button b = (Button) findViewById(R.id.test_msg_btn);
        b.setOnClickListener(this);

        showDialog();

        mDevicesListView = (ListView) findViewById(R.id.devices_list);
        mDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
                String entry = (String) parent.getItemAtPosition(position);
                mBC.connectTo(entry);
            }
        });
        setEnableListView(false);

        mBC = new BluetoothComm(this, getApplicationContext());
        mBC.scan();

        // Create Game
        mGame = Game.getInstance();
        mGame.setNrPlayer(3); // TODO: Extraxt from dialog and put in here
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mBC.unregisterListener(this);
    }

    /**
     * The idea is that the user clicks on one of the other players squares and that he can then
     * select one of the devices on the list. We need then to establish the connection the the other
     * etc..
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
                b.setSelected(!b.isSelected());
                if (b.isSelected()) mBC.setCurrentPlayer(1);
                break;
            case R.id.player2_btn:
                b.setSelected(!b.isSelected());
                if (b.isSelected()) mBC.setCurrentPlayer(2);
                break;
            case R.id.player3_btn:
                b.setSelected(!b.isSelected());
                if (b.isSelected()) mBC.setCurrentPlayer(3);
                break;
            case R.id.test_msg_btn:
                // Send test message
                JSONObject msg = (new MessageFactory()).createMessage(MessageFactory.MOCK_MESSAGE, -2, null);
                mBC.sendMessageToPlayer(msg,1);
        }
        // Check if no button is selected -> need to disable list
        boolean sel = false;
        for (ImageButton ib : mImageButtons) {
            if(ib.isSelected()) sel = true;
        }
        if (!sel) {
            setEnableListView(false);
            mBC.setCurrentPlayer(-1);
        }
    }

    // Populate listview as soon as devices found or changed
    public void onDeviceFound(String name)
    {
        if (mAdapter == null) {
            List<String> names = new ArrayList<>();
            names.add(name);
            mAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);
            mDevicesListView.setAdapter(mAdapter);
        } else {
            mAdapter.add(name);
            mAdapter.notifyDataSetChanged();
        }
    }


    /**
     *
     * Helper functions
     *
     */

    // Shows dialog where user can decide how many players want to participate
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

}
