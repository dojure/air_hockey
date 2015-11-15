package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Game;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener, BluetoothCommListener {

    final static String LOGTAG = "SetupActivity";

    private Game game;
    private BluetoothComm bc;
    private ListView devicesListView;
    private ArrayAdapter<String> adapter;
    private ImageButton[] imageButtons = new ImageButton[3];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ImageButton b1 = (ImageButton) findViewById(R.id.player1_btn);
        imageButtons[0] = b1;
        b1.setOnClickListener(this);
        ImageButton b2 = (ImageButton) findViewById(R.id.player2_btn);
        imageButtons[1] = b2;
        b2.setOnClickListener(this);
        ImageButton b3 = (ImageButton) findViewById(R.id.player3_btn);
        imageButtons[2] = b3;
        b3.setOnClickListener(this);

        showDialog();

        devicesListView = (ListView) findViewById(R.id.devices_list);
        setEnableListView(false);

        bc = new BluetoothComm(this, getApplicationContext());

        // Create Game
        game = Game.getInstance();
        game.setNrPlayer(3); // TODO: Extraxt from dialog and put in here
    }


    /**
     * The idea is that the user clicks on one of the other players squares and that he can then
     * select one of the devices on the list. We need then to establish the connection the the other
     * etc..
     */
    public void onClick(View b)
    {
        // Deselect all others
        for (ImageButton ib : imageButtons) {
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
        for (ImageButton ib : imageButtons) {
            if(ib.isSelected()) sel = true;
        }
        if (!sel) setEnableListView(false);
    }

    // Populate listview as soon as devices found or changed
    public void onDeviceFound(String name)
    {
        if (adapter == null) {
            List<String> names = new ArrayList<>();
            names.add(name);
            adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,names);
            devicesListView.setAdapter(adapter);
        } else {
            adapter.add(name);
            adapter.notifyDataSetChanged();
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

    // Enable/disable devicesListView. Need to do the fading manually
    private void setEnableListView(boolean enable)
    {
        devicesListView.setEnabled(enable);
        if (enable) devicesListView.setAlpha((float) 1);
        else devicesListView.setAlpha((float) 0.5);

    }

}
