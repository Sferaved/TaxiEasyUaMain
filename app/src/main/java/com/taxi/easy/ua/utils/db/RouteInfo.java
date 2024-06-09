package com.taxi.easy.ua.utils.db;

import androidx.annotation.NonNull;

public class RouteInfo {
    private String startLat;
    private String startLan;
    private String toLat;
    private String toLng;
    private String start;
    private String finish;

    public RouteInfo(String startLat, String startLan, String toLat, String toLng, String start, String finish) {
        this.startLat = startLat;
        this.startLan = startLan;
        this.toLat = toLat;
        this.toLng = toLng;
        this.start = start;
        this.finish = finish;
    }

    @NonNull
    @Override
    public String toString() {
        return "RouteInfo{" +
                "startLat=" + startLat +
                ", startLan=" + startLan +
                ", toLat=" + toLat +
                ", toLng=" + toLng +
                ", start='" + start + '\'' +
                ", finish='" + finish + '\'' +
                '}';
    }

    // Добавьте геттеры и сеттеры для всех полей
    // Пример геттера
    public String getStartLat() {
        return startLat;
    }

    public String getStartLan() {
        return startLan;
    }

    public String getToLat() {
        return toLat;
    }

    public String getToLng() {
        return toLng;
    }

    public String getStart() {
        return start;
    }

    public String getFinish() {
        return finish;
    }

    // Пример сеттера
    public void setStartLat(String startLat) {
        this.startLat = startLat;
    }

    public void setStartLan(String startLan) {
        this.startLan = startLan;
    }

    public void setToLat(String toLat) {
        this.toLat = toLat;
    }

    public void setToLng(String toLng) {
        this.toLng = toLng;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }
}
