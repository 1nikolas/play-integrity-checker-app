package gr.nikolasspyr.integritycheck;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;

import org.json.JSONObject;

import gr.nikolasspyr.integritycheck.async.AsyncTask;
import gr.nikolasspyr.integritycheck.dialogs.AboutDialog;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btn;
    private ImageView deviceIntegrityIcon;
    private ImageView basicIntegrityIcon;
    private ImageView strongIntegrityIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(R.string.app_name);
        }

        btn = findViewById(R.id.check_btn);
        deviceIntegrityIcon = findViewById(R.id.device_integrity_icon);
        basicIntegrityIcon = findViewById(R.id.basic_integrity_icon);
        strongIntegrityIcon = findViewById(R.id.strong_integrity_icon);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButtonLoading(true);
                setIcons(-1, -1, -1);
                getToken();
            }
        });
    }

    private void getToken(){
        String nonce = generateNonce(50);

        // Create an instance of a manager.
        IntegrityManager integrityManager = IntegrityManagerFactory.create(getApplicationContext());

        // Request the integrity token by providing a nonce.
        Task<IntegrityTokenResponse> integrityTokenResponse = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                        .setNonce(nonce)
                        .build());

        integrityTokenResponse.addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
            @Override
            public void onSuccess(IntegrityTokenResponse integrityTokenResponse) {
                String integrityToken = integrityTokenResponse.token();
                new getTokenResponse().execute(integrityToken);
            }
        });

        integrityTokenResponse.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                toggleButtonLoading(false);
                showErrorDialog("Error getting token from Google. Google said: " + getErrorText(e));

            }
        });
    }

    private class getTokenResponse extends AsyncTask<String, Integer, String>{

        private boolean hasError = false;

        @Override
        protected String doInBackground(String token) throws Exception {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .get()
                    .url(BuildConfig.API_URL + "/api/check?token=" + token)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()){
                hasError = true;
                return "Api request error. Code: " + response.code();
            }
            ResponseBody responseBody = response.body();

            if (responseBody == null){
                hasError = true;
                return "Api request error. Empty response";
            }

            JSONObject json = new JSONObject(responseBody.string());

            if (json.has("error")){
                hasError = true;
                return "Api request error: " + json.getString("error");
            }

            if (!json.has("deviceIntegrity")){
                hasError = true;
                return "Api request error: Response does not contain deviceIntegrity";
            }

            return json.getJSONObject("deviceIntegrity").toString();
        }

        @Override
        protected void onBackgroundError(Exception e) {
            hasError = true;
            onPostExecute("Api request error: " + e.getMessage());
        }

        protected void onPostExecute(String result){
            if (hasError){
                showErrorDialog(result);
            } else {
                setIcons(result.contains("MEETS_DEVICE_INTEGRITY")? 1 : 0, result.contains("MEETS_BASIC_INTEGRITY")? 1 : 0, result.contains("MEETS_STRONG_INTEGRITY")? 1 : 0);
            }
            toggleButtonLoading(false);
        }
    }

    private void toggleButtonLoading(boolean isLoading){
        setButtonLoading(btn, isLoading);
        btn.setEnabled(!isLoading);
    }

    private static Drawable getProgressBarDrawable(final Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.progressBarStyleSmall, value, false);
        int progressBarStyle = value.data;
        int[] attributes = new int[]{android.R.attr.indeterminateDrawable};
        TypedArray typedArray = context.obtainStyledAttributes(progressBarStyle, attributes);
        Drawable drawable = typedArray.getDrawable(0);
        typedArray.recycle();

        return drawable;
    }

    private static void setButtonLoading(MaterialButton button, boolean loading) {
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_START);

        if (loading) {
            Drawable drawable = button.getIcon();
            if (!(drawable instanceof Animatable)) {
                drawable = getProgressBarDrawable(button.getContext());

                button.setIcon(drawable);
            }
            ((Animatable) drawable).start();
        } else {
            button.setIcon(null);
        }
    }

    private String generateNonce(int length){
        String nonce = "";
        String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for(int i = 0; i < length; i++) {
            nonce = nonce.concat(String.valueOf(allowed.charAt((int) Math.floor(Math.random() * allowed.length()))));
        }
        return nonce;
    }

    private void showErrorDialog(String text){
        new MaterialAlertDialogBuilder(MainActivity.this, R.style.Theme_PlayIntergrityAPIChecker_Dialogs)
                .setTitle(R.string.error)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setMessage(text)
                .show();
    }

    private void setIcons(int deviceState, int basicState, int strongState){
        setIcon(deviceIntegrityIcon, deviceState);
        setIcon(basicIntegrityIcon, basicState);
        setIcon(strongIntegrityIcon, strongState);
    }

    private void setIcon(ImageView img, int state){
        if (state == -1){
            img.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_none));
        } else if (state == 0){
            img.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_fail));
        } else {
            img.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_check));
        }
    }

    private String getErrorText(Exception e){
        String msg = e.getMessage();
        if (msg == null){
            return "Unknown Error";
        }

        //Pretty junk way of getting the error code but it works
        int errorCode = Integer.parseInt(msg.replaceAll("\n", "").replaceAll(":(.*)", ""));
        switch(errorCode){
            case IntegrityErrorCode.API_NOT_AVAILABLE:
                return "API_NOT_AVAILABLE";
            case IntegrityErrorCode.NO_ERROR:
                return "NO_ERROR";
            case IntegrityErrorCode.INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            case IntegrityErrorCode.NETWORK_ERROR:
                return "NETWORK_ERROR";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND:
                return "PLAY_STORE_NOT_FOUND";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND:
                return "PLAY_STORE_ACCOUNT_NOT_FOUND";
            case IntegrityErrorCode.APP_NOT_INSTALLED:
                return "APP_NOT_INSTALLED";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND:
                return "PLAY_SERVICES_NOT_FOUND";
            case IntegrityErrorCode.APP_UID_MISMATCH:
                return "APP_UID_MISMATCH";
            case IntegrityErrorCode.TOO_MANY_REQUESTS:
                return "TOO_MANY_REQUESTS";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE:
                return "CANNOT_BIND_TO_SERVICE";
            case IntegrityErrorCode.NONCE_TOO_SHORT:
                return "NONCE_TOO_SHORT";
            case IntegrityErrorCode.NONCE_TOO_LONG:
                return "NONCE_TOO_LONG";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE:
                return "GOOGLE_SERVER_UNAVAILABLE";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64:
                return "NONCE_IS_NOT_BASE64";
            default:
                return "Unknown Error";
        }
    }

    // Menu stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        //toolbarMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.about) {
            new AboutDialog(MainActivity.this).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }


}