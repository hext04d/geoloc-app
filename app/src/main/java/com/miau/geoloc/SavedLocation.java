package com.miau.geoloc;

import android.graphics.Color;

/**
 * Modelo de dados para representar uma localização salva pelo usuário.
 */
public class SavedLocation {
    private String nickname;
    private String address;
    private double latitude;
    private double longitude;
    private float accuracy;
    private String timestamp;
    private int color; // Nova propriedade para a cor do marcador

    /**
     * Construtor padrão necessário para o GSON.
     */
    public SavedLocation() {
        this.nickname = "";
        this.address = "";
        this.timestamp = "";
        this.color = Color.parseColor("#E91E63"); // Rosa padrão
    }

    public SavedLocation(String address, double latitude, double longitude, float accuracy, String timestamp) {
        this.nickname = "";
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.color = Color.parseColor("#E91E63"); // Rosa padrão
    }

    public String getNickname() { return nickname != null ? nickname : ""; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
    
    public String getTimestamp() { return timestamp != null ? timestamp : ""; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
