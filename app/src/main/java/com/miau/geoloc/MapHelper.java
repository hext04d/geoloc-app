package com.miau.geoloc;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.List;

/**
 * Classe utilitária para gerenciar as operações do mapa usando OSMDroid.
 */
public class MapHelper {

    private final MapView map;
    private final Marker userMarker;
    private final Polygon accuracyCircle;
    private boolean isFirstUpdate = true;

    public MapHelper(MapView map) {
        this.map = map;
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.map.setMultiTouchControls(true);
        
        // Remove os botões de zoom (+ e -) nativos
        this.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        
        this.map.getController().setZoom(18.5);

        userMarker = new Marker(map);
        userMarker.setTitle("Você está aqui");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(userMarker);

        accuracyCircle = new Polygon(map);
        accuracyCircle.getFillPaint().setColor(Color.argb(50, 0, 150, 255));
        accuracyCircle.getOutlinePaint().setColor(Color.argb(100, 0, 150, 255));
        accuracyCircle.getOutlinePaint().setStrokeWidth(2.0f);
        map.getOverlays().add(accuracyCircle);
    }

    public void updateMarkerOnly(double lat, double lon, float accuracy) {
        GeoPoint currentPoint = new GeoPoint(lat, lon);
        userMarker.setPosition(currentPoint);

        List<GeoPoint> circlePoints = Polygon.pointsAsCircle(currentPoint, accuracy);
        accuracyCircle.setPoints(circlePoints);
        map.invalidate();
    }

    public void updatePosition(double lat, double lon, float accuracy) {
        updateMarkerOnly(lat, lon, accuracy);
        GeoPoint currentPoint = new GeoPoint(lat, lon);

        if (isFirstUpdate) {
            map.getController().setCenter(currentPoint);
            isFirstUpdate = false;
        } else {
            map.getController().animateTo(currentPoint);
        }
    }

    public void centerOnUser() {
        if (userMarker.getPosition() != null) {
            map.getController().animateTo(userMarker.getPosition());
        }
    }

    public void centerOnLocation(double lat, double lon) {
        GeoPoint point = new GeoPoint(lat, lon);
        map.getController().animateTo(point);
    }

    public void applyTheme(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            ColorMatrix matrix = new ColorMatrix(new float[]{
                    -1.0f, 0, 0, 0, 255,
                    0, -1.0f, 0, 0, 255,
                    0, 0, -1.0f, 0, 255,
                    0, 0, 0, 1.0f, 0
            });
            map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(matrix));
        } else {
            map.getOverlayManager().getTilesOverlay().setColorFilter(null);
        }
    }

    public void onResume(Context context) {
        map.onResume();
        applyTheme(context);
    }

    public void onPause() {
        map.onPause();
    }
}
