// ForecastData.java
package com.group4.weatherapp;

public class ForecastData {
    private String date;
    private String temperature;
    private String description;
    private String iconCode;

    public ForecastData(String date, String temperature, String description, String iconCode) {
        this.date = date;
        this.temperature = temperature;
        this.description = description;
        this.iconCode = iconCode;
    }

    public String getDate() { return date; }
    public String getTemperature() { return temperature; }
    public String getDescription() { return description; }
    public String getIconCode() { return iconCode; }
}