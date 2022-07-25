package gr.nikolasspyr.integritycheck.dialogs;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import gr.nikolasspyr.integritycheck.BuildConfig;
import gr.nikolasspyr.integritycheck.R;


public class AboutDialog {

    private final AlertDialog dialog;
    private final Context context;

    public AboutDialog(Context context){
        this.context = context;

        dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_PlayIntergrityAPIChecker_Dialogs)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();

        View dialogView = View.inflate(context, R.layout.dialog_about, null);

        dialog.setView(dialogView);

        TextView aboutText = dialogView.findViewById(R.id.about_text);

        aboutText.setText(context.getResources().getString(R.string.about_text).replace("{name}", BuildConfig.VERSION_NAME).replace("{code}", String.valueOf(BuildConfig.VERSION_CODE)));

        MaterialButton githubBtn = dialogView.findViewById(R.id.about_github);
        MaterialButton licensesBtn = dialogView.findViewById(R.id.about_licenses);


        githubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLink(context.getString(R.string.about_github_link));
                dialog.dismiss();
            }
        });


        licensesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LicensesDialog(context).show();
                dialog.dismiss();
            }
        });

    }

    public void show(){
        dialog.show();
    }

    private void openLink(String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e){ //For devices that have no browsers
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show();
        }
    }
}
