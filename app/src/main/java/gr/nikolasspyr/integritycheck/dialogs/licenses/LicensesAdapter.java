package gr.nikolasspyr.integritycheck.dialogs.licenses;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import gr.nikolasspyr.integritycheck.R;


public class LicensesAdapter extends RecyclerView.Adapter<LicensesAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private List<License> mLicenses;

    public LicensesAdapter(Context c) {
        mInflater = LayoutInflater.from(c);
    }

    public void setLicenses(List<License> licenses) {
        mLicenses = licenses;
        notifyItemRangeInserted(0, licenses.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_license, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mLicenses.get(position));
    }

    @Override
    public int getItemCount() {
        return mLicenses == null ? 0 : mLicenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView text;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.license_title);
            text = itemView.findViewById(R.id.license_text);
        }

        private void bind(License license) {
            title.setText(license.subject);
            text.setText(license.text);
        }
    }

}

