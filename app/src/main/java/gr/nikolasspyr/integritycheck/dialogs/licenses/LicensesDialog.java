package gr.nikolasspyr.integritycheck.dialogs.licenses;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import gr.nikolasspyr.integritycheck.R;


public class LicensesDialog {

    private final AlertDialog dialog;
    private final Context context;

    public LicensesDialog(Context context) {
        this.context = context;

        dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_PlayIntegrityAPIChecker_Dialogs)
                .setTitle(R.string.licenses)
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                })
                .create();

        View dialogView = View.inflate(context, R.layout.dialog_licenses, null);

        dialog.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.licenses_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 16);

        LicensesAdapter adapter = new LicensesAdapter(context);
        recyclerView.setAdapter(adapter);

        AppCompatActivity activity = (AppCompatActivity) context;

        LicensesViewModel viewModel = new LicensesViewModel(activity.getApplication());
        viewModel.getLicenses().observe(activity, adapter::setLicenses);

    }

    public void show() {
        dialog.show();

        MaterialButton button = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        button.setTextColor(context.getResources().getColor(R.color.blueSecondary));

        button.setRippleColorResource(R.color.blueSecondary);
    }
}
