package com.example.bluetoothmessagingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter blueAdapter;
    private Context context;
    private int RequestCodeLocation = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        blueAdapter = BluetoothAdapter.getDefaultAdapter();
        context = getApplicationContext();
        if(blueAdapter == null){
            Toast.makeText(context, "No bluetooth on device",Toast.LENGTH_LONG).show();
        }
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

    private void permission_check(){
        //we need to check if access is granted or not
        String[] perm = new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,perm, RequestCodeLocation );
        }
        else{
            Intent intent = new Intent(context, SearchDevices.class);
            startActivity(intent);
        }
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
        if (blueAdapter.isEnabled()){
            Toast.makeText(context,"Bluetooth already enabled",Toast.LENGTH_SHORT).show();
        }
        else{
            blueAdapter.enable();
            Toast.makeText(context,"Bluetooth enabled",Toast.LENGTH_SHORT).show();
        }


    }



}