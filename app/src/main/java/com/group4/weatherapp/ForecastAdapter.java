package com.group4.weatherapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {
    private final List<ForecastData> forecastDataList;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ForecastAdapter(List<ForecastData> forecastDataList) {
        this.forecastDataList = forecastDataList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.forecast_day_item, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastData data = forecastDataList.get(position);
        holder.dateTextView.setText(data.getDate());
        holder.tempTextView.setText(data.getTemperature());
        holder.descTextView.setText(data.getDescription());

        String iconUrl = "https://openweathermap.org/img/wn/" + data.getIconCode() + "@2x.png";
        loadWeatherIcon(iconUrl, holder.forecastIcon);
    }

    @Override
    public int getItemCount() {
        return forecastDataList.size();
    }

    private void loadWeatherIcon(String iconUrl, ImageView imageView) {
        executorService.execute(() -> {
            try {
                URL url = new URL(iconUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                imageView.post(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView tempTextView;
        TextView descTextView;
        ImageView forecastIcon;
        ImageView iconBackground;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            tempTextView = itemView.findViewById(R.id.tempTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            forecastIcon = itemView.findViewById(R.id.forecastIcon);
            iconBackground = itemView.findViewById(R.id.iconBackground);
        }
    }
}