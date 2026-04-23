package com.miau.geoloc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Helper para abstrair a API de localização do Google Play Services.
 * Gerencia o ciclo de vida de pedidos de localização e as permissões de acesso.
 */
public class LocationHelper {

    /**
     * Interface de retorno para notificações de atualização de localização.
     */
    public interface LocationUpdateListener {
        /**
         * Chamado sempre que uma nova localização GPS for detectada.
         *
         * @param location A nova localização obtida.
         */
        void onLocationUpdated(Location location);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationCallback locationCallback;
    private final LocationUpdateListener listener;

    /**
     * Construtor da classe. Inicializa o cliente de localização e configura o callback.
     *
     * @param context  Contexto da aplicação.
     * @param listener Ouvinte para as atualizações de localização.
     */
    public LocationHelper(Context context, LocationUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null && LocationHelper.this.listener != null) {
                        LocationHelper.this.listener.onLocationUpdated(location);
                    }
                }
            }
        };
    }

    /**
     * Inicia a requisição periódica de atualizações de localização.
     * Requer que as permissões já tenham sido concedidas.
     */
    public void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    /**
     * Interrompe o rastreamento de localização.
     */
    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Verifica se o aplicativo possui a permissão necessária para localização precisa.
     *
     * @return true se a permissão foi concedida.
     */
    public boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
