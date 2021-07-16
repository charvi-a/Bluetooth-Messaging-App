
package com.example.bluetoothmessagingapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {
    public static final String DEVICE = "device";
    public static final String TOAST = "toast";
    private BluetoothAdapter blueAdapter;
    private Context context;

    private Conversation converse;
    private String connectedDev;
    private final int RequestCodeLocation = 200;
    private final int DeviceSelected = 300;

    HashMap<String, Object> map;

    private ArrayAdapter<String> chatListAdapter;
    private EditText msg_edit_text;
    private ListView messages;
    private ImageButton sendBtn;
    private Button enableBluetooth;
    private Button connect_dev;

    //types of messages
    public static final int READ_MESSAGE = 1;
    public static final int WRITE_MESSAGE = 2;
    public static final int STATE_MESSAGE = 3;
    public static final int DEVICE_MESSAGE = 4;
    public static final int TOAST_MESSAGE = 5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendBtn = findViewById(R.id.imageButtonSend);
        msg_edit_text = findViewById(R.id.messagesEditText);
        connect_dev = findViewById(R.id.connect_device);
        messages = findViewById(R.id.list_messages);
        enableBluetooth = findViewById(R.id.bluetooth_enable);

        map = new HashMap<>();

        blueAdapter = BluetoothAdapter.getDefaultAdapter();
        context = this;
        chatListAdapter = new ArrayAdapter<String>(context,R.layout.chat_layout);
        messages.setAdapter(chatListAdapter);
        converse = new Conversation(context,handler);

        if(blueAdapter == null){
            Toast.makeText(context, "No bluetooth on device",Toast.LENGTH_LONG).show();
        }

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = msg_edit_text.getText().toString();
                if (msg.length() != 0){
                    //we need to handle sending and receiving messages using Conversation class
                    map.put("Message",msg);
                    map.put("Receiver",connectedDev);
                    map.put("Sender",blueAdapter.getName());
                    FirebaseDatabase.getInstance().getReference().child("Messages").push().setValue(map);
                    msg_edit_text.setText("");
                    converse.write(msg.getBytes());

                }
            }
        });
        findViewById(R.id.bluetooth_enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enable_bluetooth();
            }
        });
        findViewById(R.id.connect_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permission_check();
            }
        });
    }


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == STATE_MESSAGE){
                if (msg.arg1 == Conversation.NONE){
                    setState("Device not connected.");
                }
                else if (msg.arg1 == Conversation.LISTENING){
                    setState("Device not connected.");
                }
                else if (msg.arg1 == Conversation.CONNECTED){
                    setState("Connected" + " " + connectedDev);
                }
                else if(msg.arg1 == Conversation.CONNECTING){
                    setState("Connecting...");
                }
            }
            else if (msg.what == READ_MESSAGE){
                byte[] read_msg = (byte[]) msg.obj;
                //msg.arg1 contains the number of bytes which is the length
                String textmsg = new String(read_msg,0,msg.arg1);
                chatListAdapter.add(connectedDev + ":" + textmsg);
            }
            else if(msg.what == WRITE_MESSAGE){
                byte[] write_msg = (byte[]) msg.obj;
                String textMsg = new String(write_msg);
                chatListAdapter.add("Me: " + textMsg);
            }
            else if (msg.what == DEVICE_MESSAGE){
                connectedDev = msg.getData().getString(DEVICE);
                Toast.makeText(context,connectedDev,Toast.LENGTH_SHORT).show();
            }
            else if (msg.what == TOAST_MESSAGE){
                Toast.makeText(context, msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    });

    private void setState(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
    }

    private void permission_check(){
        //we need to check if access is granted or not
        String[] perm = new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,perm, RequestCodeLocation );
        }
        else{
            Intent intent = new Intent(context, SearchDevices.class);
            startActivityForResult(intent,DeviceSelected);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == DeviceSelected){
            String address = data.getStringExtra("deviceAddress");
            converse.connect(blueAdapter.getRemoteDevice(address));
            int state = Integer.parseInt(data.getExtras().get("state").toString());
            msg_edit_text.setVisibility(state);
            sendBtn.setVisibility(state);
            enableBluetooth.setVisibility(View.INVISIBLE);
            connect_dev.setVisibility(View.INVISIBLE);
            messages.setVisibility(View.VISIBLE);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //checking the response of requesting the permission

        if (requestCode == RequestCodeLocation) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, SearchDevices.class);
                startActivity(intent);
            }
            else{
                //we need to request permission again
                new AlertDialog.Builder(context).setMessage("Permission is needed in order to connect to a device.").setCancelable(false)
                        .setNegativeButton("DENY", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        })
                        .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                permission_check();
                            }
                        }).show();
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void enable_bluetooth(){
        if (!blueAdapter.isEnabled()){
            blueAdapter.enable();
            Toast.makeText(context,"Bluetooth enabled",Toast.LENGTH_SHORT).show();
        }
        if(blueAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(in);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(converse != null){
            converse.end();
        }
    }
}