package com.galacticglum.distancetracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Runnable {
    public static final int LOCATION_UPDATE_TICKS = 3;
    public static final int UPDATE_PERIOD = 1000;

    private static final int ALL_PERMISSIONS_RESULT = 1011;

    private View root;
    private long ticks;

    private ArrayList<String> permissions = new ArrayList<>();
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissionsToRequest;

    private boolean isTripActive = false;
    private long startTime;
    private long currentTime;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Location previousLocation = null;
    private float totalDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = findViewById(android.R.id.content);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = permissionsToRequest(permissions);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        toggleTripButtonText((Button)findViewById(R.id.trip_button));
        startTime = currentTime = System.currentTimeMillis() / 1000;
    }

    @Override
    protected void onResume() {
        super.onResume();

        run();
    }

    @Override
    protected void onPause() {
        root.removeCallbacks(this);
        super.onPause();
    }

    @Override
    public void run() {
        root.postDelayed(this, UPDATE_PERIOD);
        ticks += 1;

        updateElapsedTimeLabel();
        if(ticks % LOCATION_UPDATE_TICKS == 0) {
            updateLocation();
        }
    }

    public void tripButtonPressed(View view) {
        isTripActive = !isTripActive;
        toggleTripButtonText((Button)view);

        if(isTripActive) {
            initializeTrip();
        }
    }

    private void initializeTrip() {
        previousLocation = null;
        totalDistance = 0;

        startTime = currentTime = System.currentTimeMillis() / 1000;
        updateElapsedTimeLabel();

        initializeLocation();
    }

    private void initializeLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null) return;
                for(Location location : locationResult.getLocations()) {
                    if (location != null) {
                        if(previousLocation == null) {
                            previousLocation = location;
                        } else {
                            float distance = location.distanceTo(previousLocation);
                            totalDistance += distance;
                            previousLocation = location;
                        }

                        System.out.println(location);

                        if (fusedLocationProviderClient != null) {
                            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };

        getLocation();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    ALL_PERMISSIONS_RESULT);
        } else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void toggleTripButtonText(Button buttonView) {
        buttonView.setText(getString(!isTripActive ? R.string.start_trip_button_text : R.string.stop_trip_button_text));
    }

    private void updateElapsedTimeLabel() {
        TextView elapsedTimeTextView = findViewById(R.id.elapsed_time_text);
        if(isTripActive) {
            currentTime = System.currentTimeMillis() / 1000;
        }

        long elapsedSeconds = currentTime - startTime;
        elapsedTimeTextView.setText(getString(R.string.elapsed_time_label_text, TimeHelpers.getTimeFormatFromSeconds(elapsedSeconds)));
    }

    private void updateLocation() {
        TextView distanceLabelTextView = findViewById(R.id.distance_label);
        if(isTripActive) {
            getLocation();
        }

        String distanceString = String.format("%.2f metres", totalDistance);
        distanceLabelTextView.setText(getString(R.string.distance_label_text, distanceString));
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }

                }

                break;
        }
    }
}
