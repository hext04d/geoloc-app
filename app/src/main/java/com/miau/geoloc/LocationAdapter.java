package com.miau.geoloc;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Adapter para exibir a lista de localizações salvas com suporte a modo de edição.
 */
public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    public interface OnLocationActionListener {
        void onDelete(int position);
        void onEdit(int position);
        void onItemClick(int position);
    }

    private final List<SavedLocation> locations;
    private final OnLocationActionListener listener;
    private boolean isEditMode = false;

    public LocationAdapter(List<SavedLocation> locations, OnLocationActionListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedLocation location = locations.get(position);
        Context context = holder.itemView.getContext();
        SharedPreferences prefs = context.getSharedPreferences("geoloc_prefs", Context.MODE_PRIVATE);
        String units = prefs.getString("units_system", "metric");
        boolean isMetric = units.equals("metric");

        holder.tvNumber.setText(String.valueOf(position + 1));
        
        // Aplicar a cor do marcador ao círculo do número na lista
        if (holder.tvNumber.getBackground() != null) {
            holder.tvNumber.getBackground().setColorFilter(location.getColor(), PorterDuff.Mode.SRC_IN);
        }
        
        String title = location.getNickname().isEmpty() ? location.getAddress() : location.getNickname();
        holder.tvAddress.setText(title);
        
        String accuracyStr;
        if (isMetric) {
            accuracyStr = String.format(Locale.getDefault(), "%.1fm", location.getAccuracy());
        } else {
            double accuracyFeet = location.getAccuracy() * 3.28084;
            accuracyStr = String.format(Locale.getDefault(), "%.1fft", accuracyFeet);
        }

        String details = String.format(Locale.getDefault(),
                "Lat: %.6f, Lon: %.6f | Precisão: %s\n%s",
                location.getLatitude(), location.getLongitude(), 
                accuracyStr, location.getTimestamp());
        
        if (!location.getNickname().isEmpty()) {
            details = location.getAddress() + "\n" + details;
        }
        holder.tvDetails.setText(details);

        // Mostrar/Esconder botões baseado no modo de edição
        int visibility = isEditMode ? View.VISIBLE : View.GONE;
        holder.btnEdit.setVisibility(visibility);
        holder.btnDelete.setVisibility(visibility);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(holder.getAdapterPosition());
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(holder.getAdapterPosition());
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvNumber;
        public final TextView tvAddress;
        public final TextView tvDetails;
        public final ImageButton btnEdit;
        public final ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvAddress = itemView.findViewById(R.id.tvSavedAddress);
            tvDetails = itemView.findViewById(R.id.tvSavedDetails);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
