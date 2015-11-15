package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Game;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener, BluetoothCommListener {

    final static String LOGTAG = "SetupActivity";

    private Game mGame;
    private BluetoothComm mBc;
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

        showDialog();

        mDevicesListView = (ListView) findViewById(R.id.devices_list);
        setEnableListView(false);

        mBc = new BluetoothComm(this, getApplicationContext());

        // Create Game
        mGame = Game.getInstance();
        mGame.setNrPlayer(3); // TODO: Extraxt from dialog and put in here
    }


    /**
     * The idea is that the user clicks on one of the other players squares and that he can then
     * select one of the devices on the list. We need then to establish the connection the the other
     * etc..
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
                break;
            case R.id.player2_btn:
                b.setSelected(!b.isSelected());
                break;
            case R.id.player3_btn:
                b.setSelected(!b.isSelected());
                break;
        }
        // Check if no button is selected -> need to disable list
        boolean sel = false;
        for (ImageButton ib : mImageButtons) {
            if(ib.isSelected()) sel = true;
        }
        if (!sel) setEnableListView(false);
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
