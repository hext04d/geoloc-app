package com.miau.geoloc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationHelper.LocationUpdateListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    
    private LocationHelper locationHelper;
    private MapHelper mapHelper;
    
    private TextView tvLatitude, tvAltitude, tvStatus, tvAddress, tvAccuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        
        locationHelper = new LocationHelper(this, this);
        mapHelper = new MapHelper(findViewById(R.id.map));
        
        checkPermissionsAndStart();
    }

    private void initViews() {
        tvLatitude = findViewById(R.id.tvLatitude);
        tvAltitude = findViewById(R.id.tvAltitude);
        tvStatus = findViewById(R.id.tvStatus);
        tvAddress = findViewById(R.id.tvAddress);
        tvAccuracy = findViewById(R.id.tvAccuracy);
    }

    @Override
    public void onLocationUpdated(Location location) {
        updateTextUI(location);
        mapHelper.updatePosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());
        updateAddress(location.getLatitude(), location.getLongitude());
    }

    private void updateTextUI(Location location) {
        tvLatitude.setText(String.format(Locale.getDefault(), "Lat: %.6f", location.getLatitude()));
        tvAltitude.setText(String.format(Locale.getDefault(), "Alt: %.2f m", location.getAltitude()));
        tvAccuracy.setText(String.format(Locale.getDefault(), "Precisão: %.1f m", location.getAccuracy()));
        tvStatus.setText("Atualizado em: " + new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
    }

    private void updateAddress(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                tvAddress.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            tvAddress.setText("Erro ao buscar endereço");
        }
    }

    private void checkPermissionsAndStart() {
        if (!locationHelper.hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationHelper.startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapHelper.onResume(this);
        if (locationHelper.hasPermissions()) {
            locationHelper.startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapHelper.onPause();
        locationHelper.stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates();
        }
    }
}
