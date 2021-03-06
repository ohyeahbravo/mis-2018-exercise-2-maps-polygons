package com.example.mis.polygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Double.parseDouble;
import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import static java.lang.StrictMath.abs;

public class MapsActivity extends FragmentActivity implements OnMapLongClickListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean locationPermissioned;
    private static final int LOCATION_REQUEST_ACCESS = 1;
    EditText info;
    int marker_count = 0;

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

        // reference: https://developer.android.com/reference/android/widget/Button
        final Button button = findViewById(R.id.polybutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Get the shared preference object of the given name and get all pairs
                // ref: https://stackoverflow.com/questions/22089411/how-to-get-all-keys-of-sharedpreferences-programmatically-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                SharedPreferences prefs = getSharedPreferences("prefs", 0);
                Map<String, ?> prefsMap = prefs.getAll();

                double latitude = 0.0;
                double longitude = 0.0;
                ArrayList<LatLng> latlngs = new ArrayList<>();

                // go over all markers
                for (int i = 0; i < marker_count; i++) {

                    // reference list
                    // https://developer.android.com/reference/android/content/SharedPreferences
                    // https://www.javatpoint.com/substring
                    // https://stackoverflow.com/questions/16311076/how-to-dynamically-add-polylines-from-an-arraylist?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    // https://stackoverflow.com/questions/7283338/getting-an-element-from-a-set?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Set<String> values = (Set<String>) prefs.getStringSet(String.valueOf(i + 1), null);
                    Iterator<String> it = values.iterator();

                    String value = it.next();
                    if (value.startsWith("lat"))
                        latitude = parseDouble(value.substring(3));
                    else if (value.startsWith("lng"))
                        longitude = parseDouble(value.substring(3));

                    value = it.next();
                    if (value.startsWith("lat"))
                        latitude = parseDouble(value.substring(3));
                    else if (value.startsWith("lng"))
                        longitude = parseDouble(value.substring(3));

                    value = it.next();
                    if (value.startsWith("lat"))
                        latitude = parseDouble(value.substring(3));
                    else if (value.startsWith("lng"))
                        longitude = parseDouble(value.substring(3));

                    latlngs.add(new LatLng(latitude, longitude));

                }

                // add points to polygons
                PolygonOptions polygonOptions = new PolygonOptions();
                polygonOptions.addAll(latlngs);
                polygonOptions.strokeColor(Color.RED);
                // reference: https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4
                polygonOptions.fillColor(Color.parseColor("#66000000"));
                Polygon polygon = mMap.addPolygon(polygonOptions);

                // compute the area and the centroid
                double area = getArea(polygon);
                LatLng centroid = getCentroid(polygon);

                // move focus to centroid as a marker with computed area
                Marker marker = mMap.addMarker(new MarkerOptions().position(centroid).title(String.valueOf(area)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centroid, 18));
                marker.showInfoWindow();    // show the area

                // clearing the shared preference for the next use
                // reference: https://stackoverflow.com/questions/3687315/deleting-shared-preferences
                prefs.edit().clear().apply();

                // change the button display
                button.setText("End Polygon");

            }
        });
    }

    /**
     * compute the area from the polygon's points
     * reference: https://en.wikipedia.org/wiki/Polygon
     */
    private double getArea(Polygon polygon) {

        double area = 0.0;

        List<LatLng> points = polygon.getPoints();
        int numPoints = points.size();
        double temp = 0;
        int i = 0;
        int j = 0;

        for (i = 0; i < numPoints; i++) {

            // (xn, yn) = (x0, x0), where n = number of points
            if (i == numPoints - 1)
                j = 0;
            else j = i + 1;

            double lat1 = points.get(i).latitude;
            double lat2 = points.get(j).latitude;
            double lng1 = points.get(i).longitude;
            double lng2 = points.get(j).longitude;
            temp = lat1 * lng2 - lat2 * lng1;
            area += temp;
        }

        area = area * 0.5;

        return Math.abs(area);
    }

    /**
     * compute the centroid from the polygon's points
     * reference: https://en.wikipedia.org/wiki/Centroid
     */
    private LatLng getCentroid(Polygon polygon) {
        double lat = 0.0;
        double lng = 0.0;

        int i = 0;
        int j = 0;

        for (i = 0; i < polygon.getPoints().size(); i++) {

            if (i == polygon.getPoints().size() - 1)
                j = 0;
            else j = i + 1;
            double xi = polygon.getPoints().get(i).latitude;
            double yi1 = polygon.getPoints().get(j).longitude;
            double xi1 = polygon.getPoints().get(j).longitude;
            double yi = polygon.getPoints().get(i).latitude;

            lat = lat + ((xi + xi1) * (xi * yi1 - xi1 * yi));
            lng = lng + ((yi + yi1) * (xi * yi1 - xi1 * yi));
        }

        lat /= (6 * getArea(polygon));
        lng /= (6 * getArea(polygon));

        LatLng latlng = new LatLng(lat, lng);

        return latlng;
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
        values.add("lat" + String.valueOf(point.latitude));
        values.add("lng" + String.valueOf(point.longitude));
        values.add("info" + infostr);
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        marker_count += 1;
        editor.putStringSet(String.valueOf(marker_count), values);
        Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_LONG).show();
        editor.commit();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * <p>
     * The map initially shows the current location of the device.
     * If the location is not available, it shows the default location, which is Sydney, Austrailia.
     * We referenced the following git page from google:
     * https://github.com/googlemaps/android-samples/tree/master/tutorials/CurrentPlaceDetailsOnMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();

        // Add a marker at current location and move the camera
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (locationPermissioned) {

                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

                Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        // Set the map's camera position to the current location of the device.
                        if (task.isSuccessful()) {
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