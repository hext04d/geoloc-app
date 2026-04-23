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

    public void updateMarkers(List<SavedLocation> locations, boolean isEditMode, boolean showPolygon, Context context) {
        for (Marker m : savedMarkers) {
            map.getOverlays().remove(m);
        }
        savedMarkers.clear();

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
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); // Esferas centralizadas
            
            String title = loc.getNickname().isEmpty() ? loc.getAddress() : loc.getNickname();
            m.setTitle(title);

            // Usa o novo drawable PIN para os marcadores do mapa
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.marker_location_pin).mutate();
            icon.setColorFilter(loc.getColor(), PorterDuff.Mode.SRC_IN);
            
            if (isEditMode) {
                icon.setAlpha(180); 
            }

            m.setIcon(icon);
            savedMarkers.add(m);
            map.getOverlays().add(m);
        }

        if (showPolygon && points.size() >= 3) {
            areaPolygon = new Polygon(map);
            areaPolygon.getFillPaint().setColor(Color.argb(30, 0, 85, 170)); // Azul Aero transparente
            areaPolygon.getOutlinePaint().setColor(Color.argb(150, 0, 85, 170));
            areaPolygon.getOutlinePaint().setStrokeWidth(5.0f);
            areaPolygon.setPoints(points);
            map.getOverlays().add(0, areaPolygon);
        }

        map.invalidate();
    }
}
