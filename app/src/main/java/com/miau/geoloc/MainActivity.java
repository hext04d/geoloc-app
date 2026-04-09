package com.miau.geoloc;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationHelper.LocationUpdateListener, LocationAdapter.OnLocationActionListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String PREFS_NAME = "geoloc_prefs";
    private static final String PREFS_LOCATIONS = "saved_locations_list";
    
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializa configuração do mapa
        Configuration.getInstance().load(this, getSharedPreferences(PREFS_NAME, MODE_PRIVATE));
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadSavedLocations();
        setupRecyclerView();
        
        locationHelper = new LocationHelper(this, this);
        
        MapView mapView = findViewById(R.id.map);
        if (mapView != null) {
            mapHelper = new MapHelper(mapView);
            markersManager = new SavedMarkersManager(mapView);
            refreshMapMarkers();
        }
        
        checkPermissionsAndStart();

        View btnSave = findViewById(R.id.btnSaveLocation);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveCurrentLocation());
        }
        
        MaterialButton btnToggleEdit = findViewById(R.id.btnToggleEditMode);
        if (btnToggleEdit != null) {
            updateEditButtonUI(btnToggleEdit);
            
            btnToggleEdit.setOnClickListener(v -> {
                isEditMode = !isEditMode;
                if (adapter != null) {
                    adapter.setEditMode(isEditMode);
                }
                updateEditButtonUI(btnToggleEdit);
                refreshMapMarkers();
            });
        }

        FloatingActionButton fabCenter = findViewById(R.id.fabCenter);
        if (fabCenter != null) {
            fabCenter.setOnClickListener(v -> {
                if (mapHelper != null) {
                    mapHelper.centerOnUser();
                }
            });
        }

        FloatingActionButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void updateEditButtonUI(MaterialButton button) {
        if (isEditMode) {
            // Busca a cor primária do tema de forma compatível
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int colorPrimary = typedValue.data;
            
            button.setIconTint(ColorStateList.valueOf(colorPrimary));
            button.setAlpha(1.0f);
        } else {
            // Cor neutra quando inativo
            button.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
            button.setAlpha(0.6f);
        }
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
            rvSavedLocations.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        this.lastLocation = location;
        updateTextUI(location);
        if (mapHelper != null) {
            mapHelper.updatePosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());
        }
        updateAddress(location.getLatitude(), location.getLongitude());
    }

    private void updateTextUI(Location location) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String units = prefs.getString("units_system", "metric");
        boolean isMetric = units.equals("metric");

        if (tvLatitude != null) tvLatitude.setText(String.format(Locale.getDefault(), "Lat: %.6f", location.getLatitude()));
        
        if (tvAltitude != null) {
            if (isMetric) {
                tvAltitude.setText(String.format(Locale.getDefault(), "Alt: %.2f m", location.getAltitude()));
            } else {
                double altitudeFeet = location.getAltitude() * 3.28084;
                tvAltitude.setText(String.format(Locale.getDefault(), "Alt: %.2f ft", altitudeFeet));
            }
        }

        if (tvAccuracy != null) {
            if (isMetric) {
                tvAccuracy.setText(String.format(Locale.getDefault(), "Precisão: %.1f m", location.getAccuracy()));
            } else {
                double accuracyFeet = location.getAccuracy() * 3.28084;
                tvAccuracy.setText(String.format(Locale.getDefault(), "Precisão: %.1f ft", accuracyFeet));
            }
        }

        if (tvStatus != null) tvStatus.setText("Atualizado em: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
    }

    private void updateAddress(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                lastAddressText = addresses.get(0).getAddressLine(0);
                if (tvAddress != null) tvAddress.setText(lastAddressText);
            }
        } catch (IOException e) {
            if (tvAddress != null) tvAddress.setText("Erro ao buscar endereço");
        }
    }

    private void saveCurrentLocation() {
        if (lastLocation == null) {
            Toast.makeText(this, "Aguardando sinal de GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        SavedLocation newLoc = new SavedLocation(
                lastAddressText,
                lastLocation.getLatitude(),
                lastLocation.getLongitude(),
                lastLocation.getAccuracy(),
                timestamp
        );

        savedLocationsList.add(0, newLoc);
        if (adapter != null) {
            adapter.notifyItemInserted(0);
            adapter.notifyItemRangeChanged(1, savedLocationsList.size() - 1);
        }
        if (rvSavedLocations != null) rvSavedLocations.scrollToPosition(0);
        persistLocations();
        refreshMapMarkers();
    }

    @Override
    public void onDelete(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Localização")
                .setMessage("Deseja realmente excluir este registro?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    if (position >= 0 && position < savedLocationsList.size()) {
                        savedLocationsList.remove(position);
                        if (adapter != null) {
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, savedLocationsList.size() - position);
                        }
                        persistLocations();
                        refreshMapMarkers();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

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

        if (etNickname != null) etNickname.setText(loc.getNickname());
        if (etAddress != null) etAddress.setText(loc.getAddress());
        if (etLat != null) etLat.setText(String.valueOf(loc.getLatitude()));
        if (etLon != null) etLon.setText(String.valueOf(loc.getLongitude()));
        
        // Selecionar a cor atual no RadioGroup
        if (rgColors != null) {
            int currentColor = loc.getColor();
            if (currentColor == Color.parseColor("#E91E63")) rgColors.check(R.id.rbColorPink);
            else if (currentColor == Color.parseColor("#2196F3")) rgColors.check(R.id.rbColorBlue);
            else if (currentColor == Color.parseColor("#4CAF50")) rgColors.check(R.id.rbColorGreen);
            else if (currentColor == Color.parseColor("#FF9800")) rgColors.check(R.id.rbColorOrange);
            else if (currentColor == Color.parseColor("#9C27B0")) rgColors.check(R.id.rbColorPurple);
            else if (currentColor == Color.parseColor("#F44336")) rgColors.check(R.id.rbColorRed);
        }

        new AlertDialog.Builder(this)
                .setTitle("Editar Localização")
                .setView(dialogView)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    try {
                        if (etNickname != null) loc.setNickname(etNickname.getText().toString().trim());
                        if (etAddress != null) loc.setAddress(etAddress.getText().toString().trim());
                        if (etLat != null) loc.setLatitude(Double.parseDouble(etLat.getText().toString()));
                        if (etLon != null) loc.setLongitude(Double.parseDouble(etLon.getText().toString()));
                        
                        // Salvar a cor selecionada
                        if (rgColors != null) {
                            int checkedId = rgColors.getCheckedRadioButtonId();
                            if (checkedId == R.id.rbColorPink) loc.setColor(Color.parseColor("#E91E63"));
                            else if (checkedId == R.id.rbColorBlue) loc.setColor(Color.parseColor("#2196F3"));
                            else if (checkedId == R.id.rbColorGreen) loc.setColor(Color.parseColor("#4CAF50"));
                            else if (checkedId == R.id.rbColorOrange) loc.setColor(Color.parseColor("#FF9800"));
                            else if (checkedId == R.id.rbColorPurple) loc.setColor(Color.parseColor("#9C27B0"));
                            else if (checkedId == R.id.rbColorRed) loc.setColor(Color.parseColor("#F44336"));
                        }

                        if (adapter != null) adapter.notifyItemChanged(position);
                        persistLocations();
                        refreshMapMarkers();
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao salvar: verifique os valores", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onItemClick(int position) {
        if (position >= 0 && position < savedLocationsList.size()) {
            SavedLocation loc = savedLocationsList.get(position);
            if (mapHelper != null) {
                mapHelper.centerOnLocation(loc.getLatitude(), loc.getLongitude());
            }
        }
    }

    private void persistLocations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = new Gson().toJson(savedLocationsList);
        prefs.edit().putString(PREFS_LOCATIONS, json).apply();
    }

    private void loadSavedLocations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(PREFS_LOCATIONS, null);
        try {
            if (json != null) {
                Type type = new TypeToken<ArrayList<SavedLocation>>() {}.getType();
                savedLocationsList = new Gson().fromJson(json, type);
            }
        } catch (Exception e) {
            savedLocationsList = new ArrayList<>();
        }
        
        if (savedLocationsList == null) {
            savedLocationsList = new ArrayList<>();
        }
    }

    private void refreshMapMarkers() {
        if (markersManager != null) {
            markersManager.updateMarkers(savedLocationsList, isEditMode, this);
        }
    }

    private void checkPermissionsAndStart() {
        if (locationHelper != null && !locationHelper.hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (locationHelper != null) {
            locationHelper.startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapHelper != null) mapHelper.onResume(this);
        if (locationHelper != null && locationHelper.hasPermissions()) {
            locationHelper.startLocationUpdates();
        }
        refreshMapMarkers();
        
        // Atualiza a UI caso as unidades tenham mudado nas configurações
        if (lastLocation != null) {
            updateTextUI(lastLocation);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapHelper != null) mapHelper.onPause();
        if (locationHelper != null) locationHelper.stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (locationHelper != null) locationHelper.startLocationUpdates();
        }
    }
}
