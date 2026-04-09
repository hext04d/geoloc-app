package com.miau.geoloc;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Módulo responsável por gerenciar os marcadores das localizações salvas no mapa.
 */
public class SavedMarkersManager {

    private final MapView map;
    private final List<Marker> savedMarkers = new ArrayList<>();

    public SavedMarkersManager(MapView map) {
        this.map = map;
    }

    /**
     * Atualiza os marcadores das localizações salvas no mapa.
     *
     * @param locations  Lista de localizações salvas.
     * @param isEditMode Define se os marcadores devem aparecer em modo de edição (efeito visual opcional).
     * @param context    Contexto para carregar recursos.
     */
    public void updateMarkers(List<SavedLocation> locations, boolean isEditMode, Context context) {
        // Remover marcadores antigos
        for (Marker m : savedMarkers) {
            map.getOverlays().remove(m);
        }
        savedMarkers.clear();

        if (locations == null) return;

        for (SavedLocation loc : locations) {
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            String title = loc.getNickname().isEmpty() ? loc.getAddress() : loc.getNickname();
            m.setTitle(title);
            m.setSnippet(loc.getTimestamp());

            // Customizar ícone com a cor salva
            Drawable icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default).mutate();
            int markerColor = loc.getColor();
            
            // Se estiver em modo de edição, podemos aplicar um efeito ou manter a cor original
            // Por enquanto, manteremos a cor da localização, mas talvez com uma transparência ou ícone diferente
            if (isEditMode) {
                // Opcional: destaque para edição
                icon.setAlpha(180); 
            }

            icon.setColorFilter(markerColor, PorterDuff.Mode.SRC_IN);
            m.setIcon(icon);

            savedMarkers.add(m);
            map.getOverlays().add(m);
        }
        map.invalidate();
    }
}
