package com.taxi.easy.ua.ui.weather.finish;

public class CityInfo {
    private String weather;
    private double temperature;
    private boolean airAlarm;
    private boolean rebActive;
    private String timeStamp;

    // Getters и Setters
    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public boolean isAirAlarm() { return airAlarm; }
    public void setAirAlarm(boolean airAlarm) { this.airAlarm = airAlarm; }

    public boolean isRebActive() { return rebActive; }
    public void setRebActive(boolean rebActive) { this.rebActive = rebActive; }

    public String getTimeStamp() { return timeStamp; }
    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }
}