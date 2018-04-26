package com.example.mis.polygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashSet;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapLongClickListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean locationPermissioned;
    private static final int LOCATION_REQUEST_ACCESS = 1;
    EditText info;

    // Default Location is Sydney, in case of null location
    LatLng finalLatLng = new LatLng(-34, 151);

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Reference: https://developers.google.com/maps/documentation/android-api/location?authuser=2
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissioned = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_ACCESS);
        }
    }

    /**
     *
     * reference: https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap.OnMapLongClickListener
     * Creates a marker when clicked long.
     * Text Input Above will be saved as a custom message.
     * Tapping the marker will show the message.
     */
    @Override
    public void onMapLongClick(LatLng point) {
        info = findViewById(R.id.text);
        String infostr = info.getText().toString();
        mMap.addMarker(new MarkerOptions().position(point).title(infostr));

        // Message & Location saved as key/value set in shared preferences.
        // reference1: https://developer.android.com/training/data-storage/shared-preferences
        // reference2: https://androidforums.com/threads/help-with-putstringset-and-getstringset-method.616410/
        Set<String> values = new HashSet<String>();
        values.add(String.valueOf(point.latitude));
        values.add(String.valueOf(point.longitude));
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(infostr, values);
        Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_LONG).show();
        editor.commit();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     *
     * The map initially shows the current location of the device.
     * If the location is not available, it shows the default location, which is Sydney, Austrailia.
     * We referenced the following git page from google:
     * https://github.com/googlemaps/android-samples/tree/master/tutorials/CurrentPlaceDetailsOnMap
     *
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();

        // Add a marker at current location and move the camera
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(locationPermissioned) {

                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        // Set the map's camera position to the current location of the device.
                        if(task.isSuccessful()){
                            Location current = task.getResult();
                            finalLatLng = new LatLng(current.getLatitude(), current.getLongitude());
                        } else {
                            Toast.makeText(getApplicationContext(), "Current Location not Available", Toast.LENGTH_SHORT).show();
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(finalLatLng, 18));
                    }
                });
                mMap.setOnMapLongClickListener(this);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
