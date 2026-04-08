package com.miau.geoloc;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.List;

/**
 * Classe utilitária para gerenciar as operações do mapa usando OSMDroid.
 * Responsável por inicializar o mapa, gerenciar marcadores, círculos de precisão e temas.
 */
public class MapHelper {

    private final MapView map;
    private final Marker userMarker;
    private final Polygon accuracyCircle;
    private boolean isFirstUpdate = true;

    /**
     * Inicializa o MapHelper com uma instância de MapView.
     * Configura o marcador do usuário e o círculo de precisão inicial.
     *
     * @param map O componente MapView do layout.
     */
    public MapHelper(MapView map) {
        this.map = map;
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.map.setMultiTouchControls(true);
        // Definindo um zoom inicial mais próximo (18.5)
        this.map.getController().setZoom(18.5);

        // Inicializar marcador
        userMarker = new Marker(map);
        userMarker.setTitle("Você está aqui");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(userMarker);

        // Inicializar círculo de precisão
        accuracyCircle = new Polygon(map);
        accuracyCircle.getFillPaint().setColor(Color.argb(50, 0, 150, 255));
        accuracyCircle.getOutlinePaint().setColor(Color.argb(100, 0, 150, 255));
        accuracyCircle.getOutlinePaint().setStrokeWidth(2.0f);
        map.getOverlays().add(accuracyCircle);
    }

    /**
     * Atualiza a posição do marcador do usuário e o círculo de precisão no mapa.
     *
     * @param lat      Latitude atual.
     * @param lon      Longitude atual.
     * @param accuracy Precisão da localização em metros.
     */
    public void updatePosition(double lat, double lon, float accuracy) {
        GeoPoint currentPoint = new GeoPoint(lat, lon);
        userMarker.setPosition(currentPoint);

        List<GeoPoint> circlePoints = Polygon.pointsAsCircle(currentPoint, accuracy);
        accuracyCircle.setPoints(circlePoints);

        if (isFirstUpdate) {
            map.getController().setCenter(currentPoint);
            isFirstUpdate = false;
        } else {
            map.getController().animateTo(currentPoint);
        }
        map.invalidate();
    }

    /**
     * Aplica um filtro de cores ao mapa dependendo se o modo noturno está ativo.
     *
     * @param context Contexto da aplicação para verificar a configuração de UI.
     */
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

    /**
     * Gerencia o ciclo de vida onResume do componente de mapa.
     *
     * @param context Contexto da aplicação.
     */
    public void onResume(Context context) {
        map.onResume();
        applyTheme(context);
    }

    /**
     * Gerencia o ciclo de vida onPause do componente de mapa.
     */
    public void onPause() {
        map.onPause();
    }
}
