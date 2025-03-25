package com.group4.weatherapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {
    private static final String STATE_CITY = "saved_city";
    private static final String STATE_WEATHER_DATA = "weather_data";

    private EditText cityInput;
    private Button getWeatherButton;
    private TextView locationView, currentTempView, currentDescView;
    private ImageView weatherIcon, currentWeatherIconBackground;
    private LinearLayout forecastContainer, currentWeatherContainer;
    private ProgressBar progressBar;
    private WeatherData currentWeatherData;
    private int loadingTasks = 0;
    private final String API_KEY = "c1c24a5d23e1c898557bb46d2d9ee2d2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        cityInput = findViewById(R.id.cityInput);
        getWeatherButton = findViewById(R.id.WeatherButton);
        locationView = findViewById(R.id.locationView);
        currentTempView = findViewById(R.id.currentTempView);
        currentDescView = findViewById(R.id.currentDescView);
        weatherIcon = findViewById(R.id.weatherIcon);
        currentWeatherIconBackground = findViewById(R.id.currentWeatherIconBackground);
        forecastContainer = findViewById(R.id.forecastContainer);
        currentWeatherContainer = findViewById(R.id.currentWeatherContainer);
        progressBar = findViewById(R.id.progressBar);

        getWeatherButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                getWeather(city);
            } else {
                Toast.makeText(this, "Enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState != null) {
            cityInput.setText(savedInstanceState.getString(STATE_CITY, ""));
            String weatherJsonString = savedInstanceState.getString(STATE_WEATHER_DATA);
            if (weatherJsonString != null) {
                try {
                    currentWeatherData = WeatherData.fromJsonString(weatherJsonString);
                    updateUI(currentWeatherData);
                } catch (JSONException e) {
                    Toast.makeText(this, "Error restoring weather data", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CITY, cityInput.getText().toString());
        if (currentWeatherData != null) {
            outState.putString(STATE_WEATHER_DATA, currentWeatherData.toJsonString());
        }
    }

    private void getWeather(String city) {
        loadingTasks = 0;
        progressBar.setVisibility(View.VISIBLE);
        currentWeatherContainer.setVisibility(View.GONE);
        forecastContainer.setVisibility(View.GONE);
        new WeatherTask().execute(city);
    }

    private void updateUI(WeatherData weatherData) throws JSONException {
        this.currentWeatherData = weatherData;
        currentWeatherContainer.setVisibility(View.GONE);
        forecastContainer.setVisibility(View.GONE);

        JSONObject weatherJson = weatherData.getWeatherJson();
        String cityName = weatherJson.getString("name");
        JSONObject main = weatherJson.getJSONObject("main");
        double currentTemp = main.getDouble("temp");
        JSONObject weather = weatherJson.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        String iconCode = weather.getString("icon");

        locationView.setText(cityName);
        currentTempView.setText(String.format(Locale.getDefault(), "%.1f°C", currentTemp));
        currentDescView.setText(description);

        currentWeatherIconBackground.setVisibility(View.VISIBLE);
        weatherIcon.setVisibility(View.VISIBLE);

        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        new LoadWeatherIconTask(weatherIcon, this).execute(iconUrl);

        forecastContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        JSONArray forecastList = weatherData.getForecastJson().getJSONArray("list");

        for (int i = 0; i < Math.min(forecastList.length(), 5 * 8); i += 8) {
            JSONObject forecastItem = forecastList.getJSONObject(i);
            View forecastView = inflater.inflate(R.layout.forecast_day_item, forecastContainer, false);

            TextView dateView = forecastView.findViewById(R.id.dateTextView);
            TextView tempView = forecastView.findViewById(R.id.tempTextView);
            TextView descView = forecastView.findViewById(R.id.descTextView);
            ImageView iconView = forecastView.findViewById(R.id.forecastIcon);
            ImageView iconBg = forecastView.findViewById(R.id.iconBackground);

            String date = new SimpleDateFormat("EEE", Locale.getDefault())
                    .format(new Date(forecastItem.getLong("dt") * 1000));
            double forecastTemp = forecastItem.getJSONObject("main").getDouble("temp");
            String desc = forecastItem.getJSONArray("weather")
                    .getJSONObject(0).getString("description");
            String forecastIconCode = forecastItem.getJSONArray("weather")
                    .getJSONObject(0).getString("icon");

            dateView.setText(date);
            tempView.setText(String.format(Locale.getDefault(), "%.0f°C", forecastTemp));
            descView.setText(desc);

            iconBg.setVisibility(View.VISIBLE);
            iconView.setVisibility(View.VISIBLE);

            String forecastIconUrl = "https://openweathermap.org/img/wn/" + forecastIconCode + "@2x.png";
            new LoadWeatherIconTask(iconView, this).execute(forecastIconUrl);

            forecastContainer.addView(forecastView);
        }
    }

    private class WeatherTask extends AsyncTask<String, Void, WeatherData> {
        private Exception exception;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            incrementLoadingTasks();
        }

        @Override
        protected WeatherData doInBackground(String... params) {
            String city = params[0];
            try {
                String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                        "&appid=" + API_KEY + "&units=metric";
                JSONObject weatherJson = getJsonFromUrl(weatherUrl);

                String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + city +
                        "&appid=" + API_KEY + "&units=metric";
                JSONObject forecastJson = getJsonFromUrl(forecastUrl);

                return new WeatherData(weatherJson, forecastJson);
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(WeatherData weatherData) {
            decrementLoadingTasks();

            if (exception != null) {
                Toast.makeText(WeatherActivity.this,
                        "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (weatherData != null) {
                try {
                    updateUI(weatherData);
                } catch (JSONException e) {
                    Toast.makeText(WeatherActivity.this,
                            "Error processing weather data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WeatherActivity.this,
                        "Failed to get weather data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private synchronized void incrementLoadingTasks() {
        loadingTasks++;
    }

    private synchronized void decrementLoadingTasks() {
        loadingTasks--;
        checkAllTasksCompleted();
    }

    private void checkAllTasksCompleted() {
        runOnUiThread(() -> {
            if (loadingTasks == 0) {
                progressBar.setVisibility(View.GONE);
                currentWeatherContainer.setVisibility(View.VISIBLE);
                forecastContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private JSONObject getJsonFromUrl(String urlString) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class LoadWeatherIconTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        private final WeatherActivity activity;

        public LoadWeatherIconTask(ImageView imageView, WeatherActivity activity) {
            this.imageView = imageView;
            this.activity = activity;
            activity.incrementLoadingTasks();
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            HttpURLConnection connection = null;
            InputStream input = null;
            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            activity.decrementLoadingTasks();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private static class WeatherData {
        private final JSONObject weatherJson;
        private final JSONObject forecastJson;

        public WeatherData(JSONObject weatherJson, JSONObject forecastJson) {
            this.weatherJson = weatherJson;
            this.forecastJson = forecastJson;
        }

        public JSONObject getWeatherJson() {
            return weatherJson;
        }

        public JSONObject getForecastJson() {
            return forecastJson;
        }

        public String toJsonString() {
            return weatherJson.toString() + "|||" + forecastJson.toString();
        }

        public static WeatherData fromJsonString(String jsonString) throws JSONException {
            String[] parts = jsonString.split("\\|\\|\\|");
            return new WeatherData(new JSONObject(parts[0]), new JSONObject(parts[1]));
        }
    }
}