package com.mcuhq.simplebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;

import BluetoothManagement.BluetoothBoard;
import BluetoothManagement.SimpleBluetoothDevice;

public class MainActivity extends AppCompatActivity {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<android.bluetooth.BluetoothDevice> mPairedDevices;
    private ArrayAdapter<SimpleBluetoothDevice> mBTArrayAdapter;
    private ListView mDevicesListView;
    private CheckBox mLED1;
    private BroadcastReceiver blReceiver;
    private BluetoothBoard mBluetoothBoard; // bluetooth background worker thread to send and receive data

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        mScanBtn = (Button) findViewById(R.id.scan);
        mOffBtn = (Button) findViewById(R.id.off);
        mDiscoverBtn = (Button) findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button) findViewById(R.id.PairedBtn);
        mLED1 = (CheckBox) findViewById(R.id.checkboxLED1);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: create a toast telling why we're using lcoation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }


        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView) findViewById(R.id.devicesListView);
        assert mDevicesListView != null;
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
            finish();
        } else {

            mLED1.setOnClickListener(v -> {
                if (mBluetoothBoard != null) //First check to make sure thread created
                    mBluetoothBoard.send("1");
            });


            mScanBtn.setOnClickListener(this::bluetoothOn);
            mOffBtn.setOnClickListener(this::bluetoothOff);
            mListPairedDevicesBtn.setOnClickListener(this::listPairedDevices);
            mDiscoverBtn.setOnClickListener(this::discover);
        }
    }

    private void bluetoothOn(View view) {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        if (requestCode != REQUEST_ENABLE_BT) {
            return;
        }

        // Make sure the request was successful
        if (resultCode == RESULT_OK) {
            // The user picked a contact.
            // The Intent's data Uri identifies which contact was selected.
            mBluetoothStatus.setText("Enabled");
        } else
            mBluetoothStatus.setText("Disabled");
    }

    private void bluetoothOff(View view) {
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view) {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();

                this.blReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                            // add the name to the list
                            mBTArrayAdapter.add(new SimpleBluetoothDevice(device));
                            mBTArrayAdapter.notifyDataSetChanged();
                            Toast.makeText(getApplicationContext(), "Device Found", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
                            throw new RuntimeException("No devices found");
                        }

                    }
                };

                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void listPairedDevices(View view) {
        mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (android.bluetooth.BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(new SimpleBluetoothDevice(device));

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(blReceiver);

        // Cleanup
        if (mBTAdapter == null) {
            return;
        }

        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }

        if (mBTAdapter.isEnabled()) {
            mBTAdapter.disable();
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {

            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mBTAdapter.isDiscovering()) {
                // Cancel discovery once the user knows which device to connect to
                mBTAdapter.cancelDiscovery();
            }

            SimpleBluetoothDevice btDev = (SimpleBluetoothDevice) av.getItemAtPosition(position);

            // TODO: this is truly ughhh!!!!
            // Get the device MAC address, which is the last 17 chars in the View
            final String address = btDev.getAddress();


            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                public void run() {
                    android.bluetooth.BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBluetoothBoard = new BluetoothBoard(device);
                        mBluetoothBoard.run();
                    } catch (IOException e) {
                        runOnUiThread(() -> Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show());
                    }

                }
            }.start();
        }
    };
}
