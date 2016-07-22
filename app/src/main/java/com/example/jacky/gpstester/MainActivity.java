package com.example.jacky.gpstester;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    private static final String CONN_INFO = "Connection information";

    Button locateBtn;
    TextView coorXTv, coorYTv, accurTv, timeTv;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coorXTv = (TextView) findViewById(R.id.coorXTv);
        coorYTv = (TextView) findViewById(R.id.coorYTv);
        timeTv = (TextView) findViewById(R.id.timeTv);
        accurTv = (TextView) findViewById(R.id.accurTv);
        locateBtn = (Button) findViewById(R.id.locateBtn);


        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gps = new GPSTracker(MainActivity.this);
                if(gps.canGetLocation()){
                    gps.updateUI();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permission denied, the functionality cannot performed", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case 2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permission denied, the functionality cannot performed", Toast.LENGTH_LONG).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause(){
        Log.w("MainActivity", "onPause()");
        super.onPause();
        gps.stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        Log.w("MainActivity", "onResume()");
        super.onResume();
        if(gps != null){
            gps.startLocationUpdates();
        }
    }

    //**************************************************************************************************************

    public class GPSTracker extends Service implements LocationListener {

        private static final String CONN_INFO = "Connection information";
        private final Context context;

        boolean isGPSEnabled = false;
        boolean isNetworkEnabled = false;
        boolean canGetLocation = false;

        public Location location;

        double latitude = 0.0;
        double longitude = 0.0;
        float accuracy = 0;
        String time = null;

        //The minimum distance to change updates in meters
        private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; //10 meters
        //The minimum time beetwen updates in milliseconds
        private static final long MIN_TIME_BW_UPDATES = 1000 * 1 *1; //ms * second * mins = n minutes / 1 minutes

        protected LocationManager locationManager;

        public GPSTracker(Context context) {
            Log.w(CONN_INFO, "Location Service created");
            this.context = context;
            this.getLocation();
        }



        public Location getLocation() {

            /**
             * Sets up location service after permissions is granted
             */
            try {
                locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

                //Get GPS & Network status
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                Log.w(CONN_INFO, "isGPSEnabled: " + isGPSEnabled + " / isGPSEnabled: " + isNetworkEnabled);

                if (!isGPSEnabled && !isNetworkEnabled) {
                    showSettingDialog();
                } else {

                    this.canGetLocation = true;

                    if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }

                    if (isGPSEnabled) {
                        if (location == null) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                            if (locationManager != null) {
                                location = locationManager
                                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }
                        }
                    }else {showSettingDialog();}
                }

            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.w(CONN_INFO, "longitude: " + longitude + " / latitude: " + latitude);
            return location;
        }


        public void stopLocationUpdates() {
            try {
                if (locationManager != null) {
                    locationManager.removeUpdates(GPSTracker.this);
                    canGetLocation = false;
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        public void startLocationUpdates() {
            if(location != null && canGetLocation == false){
                getLocation();
            }

        }

        public void showSettingDialog() {
            AlertDialog.Builder gpsAlert = new AlertDialog.Builder(context);
            gpsAlert.setTitle("GPS Setting");
            gpsAlert.setMessage("Your GPS service is not enabled. Do you want to enable it?");
            gpsAlert.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.getApplicationContext().startActivity(intent);
                }
            });
            gpsAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            gpsAlert.show();
        }

        public double getLongitude() {
            if (location != null) {
                longitude = location.getLongitude();
            }
            return longitude;
        }

        public double getLatitude() {
            if (location != null) {
                latitude = location.getLatitude();
            }
            return latitude;
        }

        public float getAccuracy() {
            if (location != null) {
                accuracy = location.getAccuracy();
            }
            return accuracy;
        }

        public String getCurrentTime(){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd / HH:mm:ss");
            time = dateFormat.format(new Date()); // Find todays date
            return time;
        }

        public boolean canGetLocation() {
            return this.canGetLocation;
        }

        @Override
        public void onLocationChanged(Location location) {
            updateUI();
            Log.i(CONN_INFO, "Location Changed to " + longitude + " and " + latitude);
        }

        public void updateUI(){
            longitude = getLongitude();
            latitude = getLatitude();
            accuracy = getAccuracy();
            time = getCurrentTime();
            coorXTv.setText("Longitude: " + String.valueOf(longitude));
            coorYTv.setText("Latitude: " + String.valueOf(latitude));
            accurTv.setText("Accuracy: " + String.valueOf((int)accuracy) + " meter(s)");
            timeTv.setText("Time: " + String.valueOf(time));
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }





    }



}


