package gr.nikolasspyr.integritycheck;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private String jsonResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        btn = findViewById(R.id.check_btn);
        deviceIntegrityIcon = findViewById(R.id.device_integrity_icon);
        basicIntegrityIcon = findViewById(R.id.basic_integrity_icon);
        strongIntegrityIcon = findViewById(R.id.strong_integrity_icon);

        btn.setOnClickListener(view -> {
            toggleButtonLoading(true);
            setIcons(-1, -1, -1);
            getToken();
        });
    }

    private void getToken() {
        String nonce = generateNonce();

        // Create an instance of a manager.
        IntegrityManager integrityManager = IntegrityManagerFactory.create(getApplicationContext());

        // Request the integrity token by providing a nonce.
        Task<IntegrityTokenResponse> integrityTokenResponse = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                        .setNonce(nonce)
                        .build());

        integrityTokenResponse.addOnSuccessListener(integrityTokenResponse1 -> {
            String integrityToken = integrityTokenResponse1.token();
            new getTokenResponse().execute(integrityToken);
        });

        integrityTokenResponse.addOnFailureListener(e -> {
            toggleButtonLoading(false);
            showErrorDialog("Error getting token from Google", getErrorText(e));
        });
    }

    private class getTokenResponse extends AsyncTask<String, Integer, String[]> {

        private boolean hasError = false;

        @Override
        protected String[] doInBackground(String token) throws Exception {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .get()
                    .url(BuildConfig.API_URL + "/api/check?token=" + token)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                hasError = true;
                return new String[]{"Api request error", "Error code: " + response.code()};
            }
            ResponseBody responseBody = response.body();

            if (responseBody == null) {
                hasError = true;
                return new String[]{"Api request error", "Empty response"};
            }

            JSONObject json = new JSONObject(responseBody.string());

            if (json.has("error")) {
                hasError = true;
                return new String[]{"Api request error", json.getString("error")};
            }

            if (!json.has("deviceIntegrity")) {
                hasError = true;
                return new String[]{"Api request error", "Response does not contain deviceIntegrity"};
            }

            jsonResponse = json.toString(4);
            return new String[]{json.getJSONObject("deviceIntegrity").toString()};
        }

        @Override
        protected void onBackgroundError(Exception e) {
            hasError = true;
            onPostExecute(new String[]{"Api request error", e.getMessage()});
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (hasError) {
                showErrorDialog(result[0], result[1]);
            } else {
                String json = result[0];
                setIcons(json.contains("MEETS_DEVICE_INTEGRITY") ? 1 : 0, json.contains("MEETS_BASIC_INTEGRITY") ? 1 : 0, json.contains("MEETS_STRONG_INTEGRITY") ? 1 : 0);
            }
            toggleButtonLoading(false);
        }
    }

    private void toggleButtonLoading(boolean isLoading) {
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

    private String generateNonce() {
        int length = 50;
        String nonce = "";
        String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            nonce = nonce.concat(String.valueOf(allowed.charAt((int) Math.floor(Math.random() * allowed.length()))));
        }
        return nonce;
    }

    private void showErrorDialog(String title, String message) {
        new MaterialAlertDialogBuilder(MainActivity.this, R.style.Theme_PlayIntegrityAPIChecker_Dialogs)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {

                })
                .setMessage(message)
                .show();
    }

    private void setIcons(int deviceState, int basicState, int strongState) {
        setIcon(deviceIntegrityIcon, deviceState);
        setIcon(basicIntegrityIcon, basicState);
        setIcon(strongIntegrityIcon, strongState);
    }

    private void setIcon(ImageView img, int state) {
        if (state == -1) {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_unknown));
            img.setContentDescription(getString(R.string.status_unknown));
        } else if (state == 0) {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fail));
            img.setContentDescription(getString(R.string.status_fail));
        } else {
            img.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pass));
            img.setContentDescription(getString(R.string.status_pass));
        }
    }

    private String getErrorText(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "Unknown Error";
        }

        //Pretty junk way of getting the error code but it works
        int errorCode = Integer.parseInt(msg.replaceAll("\n", "").replaceAll(":(.*)", ""));
        switch (errorCode) {
            case IntegrityErrorCode.API_NOT_AVAILABLE:
                return "Integrity API is not available.\n\n" +
                        "The Play Store version might be old, try updating it.";
            case IntegrityErrorCode.APP_NOT_INSTALLED:
                return "The calling app is not installed.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.APP_UID_MISMATCH:
                return "The calling app UID (user id) does not match the one from Package Manager.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE:
                return "Binding to the service in the Play Store has failed.\n\n" +
                        "This can be due to having an old Play Store version installed on the device.";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE:
                return "Unknown internal Google server error.";
            case IntegrityErrorCode.INTERNAL_ERROR:
                return "Unknown internal error.";
            case IntegrityErrorCode.NETWORK_ERROR:
                return "No available network is found.\n\n" +
                        "Please check your connection.";
            case IntegrityErrorCode.NO_ERROR:
                return "No error has occurred.\n\n" +
                        "If you ever get this, congrats, I have no idea what it means.";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64:
                return "Nonce is not encoded as a base64 web-safe no-wrap string.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NONCE_TOO_LONG:
                return "Nonce length is too long.\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NONCE_TOO_SHORT:
                return "Nonce length is too short.\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND:
                return "Play Services is not available or version is too old.\n\n" +
                        "Try updating Google Play Services.";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND:
                return "No Play Store account is found on device.\n\n" +
                        "Try logging into Play Store.";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND:
                return "No Play Store app is found on device or not official version is installed.\n\n" +
                        "This app can't work without Play Store.";
            case IntegrityErrorCode.TOO_MANY_REQUESTS:
                return "The calling app is making too many requests to the API and hence is throttled.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            default:
                return "Unknown Error";
        }
    }

    // Menu stuff
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.about) {
            new AboutDialog(MainActivity.this).show();
            return true;
        } else if (id == R.id.json_response) {
            if (jsonResponse == null) {
                Toast.makeText(this, R.string.check_first, Toast.LENGTH_SHORT).show();
            } else {
                new MaterialAlertDialogBuilder(MainActivity.this, R.style.Theme_PlayIntegrityAPIChecker_Dialogs)
                        .setTitle(R.string.json_response)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {

                        })
                        .setMessage(jsonResponse)
                        .show();
            }
            return true;
        } else if (id == R.id.documentation) {
            Utils.openLink(getString(R.string.docs_link), this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }


}