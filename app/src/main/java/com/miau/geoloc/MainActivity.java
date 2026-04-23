package com.miau.geoloc;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements LocationHelper.LocationUpdateListener, LocationAdapter.OnLocationActionListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String PREFS_NAME = "geoloc_prefs";
    private static final String PREFS_LOCATIONS = "saved_locations_list";
    private static final String KEY_DEBUG = "debug_mode";
    private static final String KEY_UNITS = "units_system";
    
    private LocationHelper locationHelper;
    private MapHelper mapHelper;
    private SavedMarkersManager markersManager;
    private Location lastLocation;
    private String lastAddressText = "Endereço desconhecido";
    
    private TextView tvLatitude, tvAltitude, tvStatus, tvAddress, tvAccuracy;
    private RecyclerView rvSavedLocations;
    private LocationAdapter adapter;
    private List<SavedLocation> savedLocationsList;
    private boolean isEditMode = false;
    private boolean isDebugMode = false;
    private boolean showPolygon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().load(this, getSharedPreferences(PREFS_NAME, MODE_PRIVATE));
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        loadSavedLocations();
        setupRecyclerView();
        
        locationHelper = new LocationHelper(this, this);
        
        MapView mapView = findViewById(R.id.map);
        if (mapView != null) {
            mapHelper = new MapHelper(mapView);
            markersManager = new SavedMarkersManager(mapView);
            
            mapView.addMapListener(new org.osmdroid.events.MapListener() {
                @Override
                public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                    if (isDebugMode) {
                        runOnUiThread(() -> {
                            GeoPoint center = (GeoPoint) mapView.getMapCenter();
                            simulateLocation(center.getLatitude(), center.getLongitude());
                        });
                    }
                    return false;
                }

                @Override
                public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                    if (isDebugMode) {
                        runOnUiThread(() -> {
                            GeoPoint center = (GeoPoint) mapView.getMapCenter();
                            simulateLocation(center.getLatitude(), center.getLongitude());
                        });
                    }
                    return false;
                }
            });

            refreshMapMarkers();
        }
        
        checkPermissionsAndStart();

        Button btnSave = findViewById(R.id.btnSaveLocation);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveCurrentLocation());
        }
        
        ImageButton btnToggleEdit = findViewById(R.id.btnToggleEditMode);
        if (btnToggleEdit != null) {
            updateEditButtonUI(btnToggleEdit);
            btnToggleEdit.setOnClickListener(v -> {
                isEditMode = !isEditMode;
                if (adapter != null) adapter.setEditMode(isEditMode);
                updateEditButtonUI(btnToggleEdit);
                refreshMapMarkers();
            });
        }

        ImageButton fabCenter = findViewById(R.id.fabCenter);
        if (fabCenter != null) {
            fabCenter.setOnClickListener(v -> {
                if (mapHelper != null) mapHelper.centerOnUser();
            });
        }

        ImageButton btnTogglePolygon = findViewById(R.id.btnTogglePolygon);
        if (btnTogglePolygon != null) {
            btnTogglePolygon.setOnClickListener(v -> {
                showPolygon = !showPolygon;
                if (showPolygon && savedLocationsList.size() < 3) {
                    Toast.makeText(this, R.string.loc_saved, Toast.LENGTH_SHORT).show(); // Ajustar para string de erro polígono
                }
                refreshMapMarkers();
            });
        }

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
            btnSettings.setOnLongClickListener(v -> {
                isDebugMode = !isDebugMode;
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_DEBUG, isDebugMode).apply();
                applyDebugModeState();
                String msg = isDebugMode ? getString(R.string.debug_enabled) : getString(R.string.debug_disabled);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    private void applyDebugModeState() {
        if (isDebugMode) {
            if (locationHelper != null) locationHelper.stopLocationUpdates();
            MapView mapView = findViewById(R.id.map);
            if (mapView != null) {
                GeoPoint center = (GeoPoint) mapView.getMapCenter();
                simulateLocation(center.getLatitude(), center.getLongitude());
            }
        } else {
            if (locationHelper != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationHelper.startLocationUpdates();
            }
        }
    }

    private void simulateLocation(double lat, double lon) {
        Location mockLocation = new Location("debug");
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lon);
        mockLocation.setAccuracy(5.0f);
        mockLocation.setAltitude(0.0);
        mockLocation.setTime(System.currentTimeMillis());
        this.lastLocation = mockLocation;
        updateTextUI(mockLocation);
        if (mapHelper != null) mapHelper.updateMarkerOnly(lat, lon, mockLocation.getAccuracy());
        updateAddress(lat, lon);
    }

    private void updateEditButtonUI(ImageButton button) {
        button.setAlpha(isEditMode ? 1.0f : 0.6f);
    }

    private void initViews() {
        tvLatitude = findViewById(R.id.tvLatitude);
        tvAltitude = findViewById(R.id.tvAltitude);
        tvStatus = findViewById(R.id.tvStatus);
        tvAddress = findViewById(R.id.tvAddress);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        rvSavedLocations = findViewById(R.id.rvSavedLocations);
    }

    private void setupRecyclerView() {
        if (savedLocationsList == null) savedLocationsList = new ArrayList<>();
        adapter = new LocationAdapter(savedLocationsList, this);
        if (rvSavedLocations != null) {
            rvSavedLocations.setLayoutManager(new LinearLayoutManager(this));
            rvSavedLocations.setAdapter(adapter);
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (isDebugMode) return;
        this.lastLocation = location;
        updateTextUI(location);
        if (mapHelper != null) {
            mapHelper.updatePosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());
        }
        updateAddress(location.getLatitude(), location.getLongitude());
    }

    private void updateTextUI(Location location) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isImperial = "imperial".equals(prefs.getString(KEY_UNITS, "metric"));

        if (tvLatitude != null) tvLatitude.setText(getString(R.string.lat_label, location.getLatitude()));
        if (tvAltitude != null) {
            double altitude = location.getAltitude();
            tvAltitude.setText(isImperial ? getString(R.string.alt_label_imperial, altitude * 3.28084) : getString(R.string.alt_label, altitude));
        }
        if (tvAccuracy != null) {
            double accuracy = location.getAccuracy();
            tvAccuracy.setText(isImperial ? getString(R.string.acc_label_imperial, accuracy * 3.28084) : getString(R.string.acc_label, accuracy));
        }
        if (tvStatus != null) tvStatus.setText(isDebugMode ? getString(R.string.status_debug) : getString(R.string.status_gps_active));
    }

    private void updateAddress(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                lastAddressText = addresses.get(0).getAddressLine(0);
                if (tvAddress != null) tvAddress.setText(lastAddressText);
            }
        } catch (IOException ignored) {}
    }

    private void saveCurrentLocation() {
        if (lastLocation == null) return;
        String timestamp = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
        SavedLocation newLoc = new SavedLocation(lastAddressText, lastLocation.getLatitude(), lastLocation.getLongitude(), lastLocation.getAccuracy(), timestamp);
        newLoc.setColor(ContextCompat.getColor(this, R.color.aero_marker_blue));
        savedLocationsList.add(0, newLoc);
        if (adapter != null) {
            adapter.notifyItemInserted(0);
            adapter.notifyItemRangeChanged(1, savedLocationsList.size() - 1);
        }
        persistLocations();
        refreshMapMarkers();
        Toast.makeText(this, R.string.loc_saved, Toast.LENGTH_SHORT).show();
    }

    @Override public void onDelete(int position) { /* Lógica de delete */ }

    @Override
    public void onEdit(int position) {
        if (position < 0 || position >= savedLocationsList.size()) return;
        SavedLocation loc = savedLocationsList.get(position);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_location, null);

        EditText etNickname = dialogView.findViewById(R.id.etNickname);
        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        EditText etLat = dialogView.findViewById(R.id.etLatitude);
        EditText etLon = dialogView.findViewById(R.id.etLongitude);
        RadioGroup rgColors = dialogView.findViewById(R.id.rgColors);
        View btnSave = dialogView.findViewById(R.id.btnSaveEdit);
        View btnCancel = dialogView.findViewById(R.id.btnCancelEdit);

        if (etNickname != null) etNickname.setText(loc.getNickname());
        if (etAddress != null) etAddress.setText(loc.getAddress());
        if (etLat != null) etLat.setText(String.valueOf(loc.getLatitude()));
        if (etLon != null) etLon.setText(String.valueOf(loc.getLongitude()));

        if (rgColors != null) {
            int currentColor = loc.getColor();
            if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_pink)) rgColors.check(R.id.rbColorPink);
            else if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_blue)) rgColors.check(R.id.rbColorBlue);
            else if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_green)) rgColors.check(R.id.rbColorGreen);
            else if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_orange)) rgColors.check(R.id.rbColorOrange);
            else if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_purple)) rgColors.check(R.id.rbColorPurple);
            else if (currentColor == ContextCompat.getColor(this, R.color.aero_marker_red)) rgColors.check(R.id.rbColorRed);
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnSave.setOnClickListener(v -> {
            try {
                if (etNickname != null) loc.setNickname(etNickname.getText().toString().trim());
                if (etAddress != null) loc.setAddress(etAddress.getText().toString().trim());
                if (etLat != null) loc.setLatitude(Double.parseDouble(etLat.getText().toString()));
                if (etLon != null) loc.setLongitude(Double.parseDouble(etLon.getText().toString()));
                if (rgColors != null) {
                    int checkedId = rgColors.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbColorPink) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_pink));
                    else if (checkedId == R.id.rbColorBlue) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_blue));
                    else if (checkedId == R.id.rbColorGreen) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_green));
                    else if (checkedId == R.id.rbColorOrange) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_orange));
                    else if (checkedId == R.id.rbColorPurple) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_purple));
                    else if (checkedId == R.id.rbColorRed) loc.setColor(ContextCompat.getColor(this, R.color.aero_marker_red));
                }
                if (adapter != null) adapter.notifyItemChanged(position);
                persistLocations();
                refreshMapMarkers();
                dialog.dismiss();
            } catch (Exception e) { Toast.makeText(this, getString(R.string.error_save), Toast.LENGTH_SHORT).show(); }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override public void onItemClick(int position) { /* Lógica de clique */ }

    private void persistLocations() {
        String json = new Gson().toJson(savedLocationsList);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREFS_LOCATIONS, json).apply();
    }

    private void loadSavedLocations() {
        String json = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREFS_LOCATIONS, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<SavedLocation>>() {}.getType();
            savedLocationsList = new Gson().fromJson(json, type);
        }
        if (savedLocationsList == null) savedLocationsList = new ArrayList<>();
    }

    private void refreshMapMarkers() {
        if (markersManager != null) markersManager.updateMarkers(savedLocationsList, isEditMode, showPolygon, this);
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else if (locationHelper != null && !isDebugMode) {
            locationHelper.startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapHelper != null) mapHelper.onResume(this);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDebugMode = prefs.getBoolean(KEY_DEBUG, false);
        applyDebugModeState();
        refreshMapMarkers();
        if (lastLocation != null) updateTextUI(lastLocation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapHelper != null) mapHelper.onPause();
        if (locationHelper != null) locationHelper.stopLocationUpdates();
    }
}
