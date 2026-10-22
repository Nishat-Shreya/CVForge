package com.example.cvforge.ui.cv;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemCertificateBinding;

import java.util.List;

/** Editable list of certificates. */
public class CertificateAdapter extends EditableListAdapter<Cv.Certificate, CertificateAdapter.ViewHolder> {

    public CertificateAdapter(List<Cv.Certificate> items) {
        super(items);
    }

    @Override
    protected Cv.Certificate createItem() {
        return new Cv.Certificate();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCertificateBinding binding = ItemCertificateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cv.Certificate item = items.get(position);
        holder.binding.editName.setText(item.name);
        holder.binding.editIssuer.setText(item.issuer);
        holder.binding.editYear.setText(item.year);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCertificateBinding binding;

        ViewHolder(ItemCertificateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            SimpleTextWatcher.watch(binding.editName, text -> {
                Cv.Certificate item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.name = text;
                }
            });
            SimpleTextWatcher.watch(binding.editIssuer, text -> {
                Cv.Certificate item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.issuer = text;
                }
            });
            SimpleTextWatcher.watch(binding.editYear, text -> {
                Cv.Certificate item = itemAt(getAdapterPosition());
                if (item != null) {
                    item.year = text;
                }
            });
            binding.btnRemove.setOnClickListener(v -> removeAt(getAdapterPosition()));
        }
    }
}
