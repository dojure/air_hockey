package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;


/**
 * The most important reason for this Activity up until now is to store the players user name to
 * SharedPreferences. Further settings?
 *
 * TODO: Everything
 */


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    final static String LOGTAG = "SettingsActivity";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private BluetoothComm mBC;

    private static final String DEFAULT_NAME = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btn_save = (Button)findViewById(R.id.btn_save);
        btn_save.setOnClickListener(this);

        mBC = BluetoothComm.getInstance();
        preferences = getSharedPreferences("Settings", MODE_PRIVATE);
        editor = preferences.edit();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();

        String device_name = preferences.getString("DEVICE_NAME", DEFAULT_NAME);
        ((EditText)findViewById(R.id.input_device_name)).setText(device_name);
    }


    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.btn_save:
                Log.d(LOGTAG, "onClickSave");
                EditText device_name = (EditText) findViewById(R.id.input_device_name);
                editor.putString("DEVICE_NAME", device_name.getText().toString());
                editor.apply();
                mBC.changeDeviceName(device_name.getText().toString());
                //TODO: toast to show success/error....
                Intent backToMainIntent = new Intent(this, MainActivity.class);
                this.startActivity(backToMainIntent);
                Log.d(LOGTAG, "back to MainActivity");
                break;
        }
    }

}
