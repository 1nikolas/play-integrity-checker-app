package gr.nikolasspyr.integritycheck.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import gr.nikolasspyr.integritycheck.BuildConfig;
import gr.nikolasspyr.integritycheck.R;
import gr.nikolasspyr.integritycheck.Utils;
import gr.nikolasspyr.integritycheck.dialogs.licenses.LicensesDialog;


public class AboutDialog {

    private final AlertDialog dialog;

    public AboutDialog(Context context) {

        dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_PlayIntegrityAPIChecker_Dialogs)
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                })
                .create();

        View dialogView = View.inflate(context, R.layout.dialog_about, null);

        dialog.setView(dialogView);

        TextView aboutText = dialogView.findViewById(R.id.about_text);

        aboutText.setText(String.format(Locale.US, context.getResources().getString(R.string.about_text), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        MaterialButton githubBtn = dialogView.findViewById(R.id.about_github);
        MaterialButton licensesBtn = dialogView.findViewById(R.id.about_licenses);


        githubBtn.setOnClickListener(v -> {
            Utils.openLink(context.getString(R.string.about_github_link), context);
            dialog.dismiss();
        });


        licensesBtn.setOnClickListener(v -> {
            new LicensesDialog(context).show();
            dialog.dismiss();
        });

    }

    public void show() {
        dialog.show();
    }
}
