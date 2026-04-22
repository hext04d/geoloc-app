package com.miau.geoloc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Módulo responsável por gerenciar os marcadores das localizações salvas no mapa.
 */
public class SavedMarkersManager {

    private final MapView map;
    private final List<Marker> savedMarkers = new ArrayList<>();
    private Polygon areaPolygon;

    public SavedMarkersManager(MapView map) {
        this.map = map;
    }

    /**
     * Atualiza os marcadores das localizações salvas no mapa.
     *
     * @param locations  Lista de localizações salvas.
     * @param isEditMode Define se os marcadores devem aparecer em modo de edição.
     * @param context    Contexto para carregar recursos.
     */
    public void updateMarkers(List<SavedLocation> locations, boolean isEditMode, Context context) {
        updateMarkers(locations, isEditMode, false, context);
    }

    /**
     * Atualiza os marcadores e opcionalmente desenha um polígono.
     */
    public void updateMarkers(List<SavedLocation> locations, boolean isEditMode, boolean showPolygon, Context context) {
        // Remover marcadores antigos
        for (Marker m : savedMarkers) {
            map.getOverlays().remove(m);
        }
        savedMarkers.clear();

        // Remover polígono antigo
        if (areaPolygon != null) {
            map.getOverlays().remove(areaPolygon);
            areaPolygon = null;
        }

        if (locations == null) return;

        List<GeoPoint> points = new ArrayList<>();

        for (SavedLocation loc : locations) {
            GeoPoint point = new GeoPoint(loc.getLatitude(), loc.getLongitude());
            points.add(point);

            Marker m = new Marker(map);
            m.setPosition(point);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            String title = loc.getNickname().isEmpty() ? loc.getAddress() : loc.getNickname();
            m.setTitle(title);
            m.setSnippet(loc.getTimestamp());

            Drawable icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default).mutate();
            int markerColor = loc.getColor();
            
            if (isEditMode) {
                icon.setAlpha(180); 
            }

            icon.setColorFilter(markerColor, PorterDuff.Mode.SRC_IN);
            m.setIcon(icon);

            savedMarkers.add(m);
            map.getOverlays().add(m);
        }

        // Criar polígono se solicitado e houver 3 ou mais pontos
        if (showPolygon && points.size() >= 3) {
            areaPolygon = new Polygon(map);
            areaPolygon.getFillPaint().setColor(Color.TRANSPARENT); // Apenas outline
            areaPolygon.getOutlinePaint().setColor(Color.RED);
            areaPolygon.getOutlinePaint().setStrokeWidth(5.0f);
            areaPolygon.setPoints(points);
            map.getOverlays().add(0, areaPolygon); // Adiciona ao fundo
        }

        map.invalidate();
    }
}
