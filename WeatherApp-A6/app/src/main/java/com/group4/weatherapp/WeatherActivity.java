package com.group4.weatherapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class WeatherActivity extends AppCompatActivity {
    private EditText cityInput;
    private Button getWeatherButton;
    private TextView resultView;
    private ProgressBar progressBar;

    private final String API_KEY = "c1c24a5d23e1c898557bb46d2d9ee2d2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        cityInput = findViewById(R.id.cityInput);
        getWeatherButton = findViewById(R.id.WeatherButton);
        resultView = findViewById(R.id.ResultView);
        progressBar = findViewById(R.id.ProgressBar);

        progressBar.setVisibility(View.GONE);

        // Button to start searching for weather
        getWeatherButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                getWeather(city);
            } else {
                Toast.makeText(this, "Enter a city name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to find weather
    private void getWeather(String city) {
        progressBar.setVisibility(View.VISIBLE);  // Show progress bar
        resultView.setText("");  // Reset result text, avoid having old text displayed

        new Thread(() -> {
            try {
                // full link with city name and api key
                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                        "&appid=" + API_KEY + "&units=metric";

                // Create a URL object with the link
                URL url = new URL(apiUrl);
                // url.openConnection() returns a generic URLConnection object, and we cast it to HttpURLConnection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                // "Get" to fetch data from the connection
                connection.setRequestMethod("GET");

                // BufferedReader to read and interpret the HTTP responses
                // It provides the .readLine() method to read one full line of text each call
                BufferedReader reader = new BufferedReader(
                        // Opens a stream to read raw response
                        new InputStreamReader(connection.getInputStream()));

                // Growable string container
                StringBuilder response = new StringBuilder();
                String line;

                // Assign each line to variable "line" and check if it is null, no then keep iterating
                while((line = reader.readLine()) != null) {
                    // Append each line to the string container
                    response.append(line);
                }
                reader.close();

                // Convert the raw response string into a JSON object
                JSONObject json = new JSONObject(response.toString());
                // Get the "main" section of the JSON (contains temperature, humidity, etc)
                JSONObject main = json.getJSONObject("main");
                // Extract the temperature value rom the "main" section
                double temp = main.getDouble("temp");
                // Get the "weather" array from the JSON (contains at least one object with description, icon, etc.)
                String description = json.getJSONArray("weather")
                        // Extract the first object in the array (main weather condition)
                        .getJSONObject(0)
                        // Get the "description" field (e.g., "clear sky", "light rain", etc)
                        .getString("description");

                // Assign the parsed JSON text to output
                String output = "Temp: " + temp + "°C\nDescription: " + description;

                runOnUiThread(() -> {
                    // Remove progress bar from view
                    progressBar.setVisibility(View.GONE);
                    // Show result
                    resultView.setText(output);
                });
                System.out.println("API Response: " + response.toString());

            } catch (Exception e) {
                // print full error stack trace to the logcat for debugging
                e.printStackTrace();

                // Remove progress bar and notify user about failure
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resultView.setText("Failed to get weather infomation. Please try again later.");
                });
            }
        }).start();
    }
}
