package gr.nikolasspyr.integritycheck;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
    private Group legacyLayout;
    private SwitchMaterial legacySwitch;

    private String jsonResponse;
    private Integer[] integrityState = {-1, -1, -1};
    private Integer[] legacyIntegrityState = {-1, -1, -1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(
                    findViewById(android.R.id.content),
                    (view, insets) -> {
                        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                        return insets;
                    }
            );
        }

        btn = findViewById(R.id.check_btn);
        deviceIntegrityIcon = findViewById(R.id.device_integrity_icon);
        basicIntegrityIcon = findViewById(R.id.basic_integrity_icon);
        strongIntegrityIcon = findViewById(R.id.strong_integrity_icon);
        legacyLayout = findViewById(R.id.legacy_row);
        legacySwitch = findViewById(R.id.legacy_switch);


        btn.setOnClickListener(view -> {
            toggleButtonLoading(true);

            jsonResponse = null;
            legacyLayout.setVisibility(View.GONE);

            integrityState = new Integer[]{-1, -1, -1};
            legacyIntegrityState = new Integer[]{-1, -1, -1};

            setIcons(integrityState);

            getToken();
        });

        legacySwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                setIcons(integrityState);
            } else {
                setIcons(legacyIntegrityState);
            }
        });


        ImageView newChecksInfo = findViewById(R.id.new_checks_info);
        newChecksInfo.setOnClickListener(view -> Utils.openLink(getString(R.string.new_checks_info), MainActivity.this));
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

    private class getTokenResponse extends AsyncTask<String, Integer, JSONObject> {

        private boolean hasError = false;
        private Pair<String, String> errorTexts;

        @Override
        protected JSONObject doInBackground(String token) throws Exception {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .get()
                    .url(BuildConfig.API_URL + "/api/check?token=" + token)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    hasError = true;
                    errorTexts = new Pair<>("API request error", "Error code: " + response.code());
                    return null;
                }
                ResponseBody responseBody = response.body();

                if (responseBody == null) {
                    hasError = true;
                    errorTexts = new Pair<>("API request error", "Empty response");
                    return null;
                }

                JSONObject json = new JSONObject(responseBody.string());

                if (json.has("error")) {
                    hasError = true;
                    errorTexts = new Pair<>("API request error", json.getString("error"));
                    return null;
                }

                if (!json.has("deviceIntegrity")) {
                    return new JSONObject();
                }

                jsonResponse = json.toString(4);
                return json.getJSONObject("deviceIntegrity");
            }

        }

        @Override
        protected void onBackgroundError(Exception e) {
            hasError = true;
            errorTexts = new Pair<>("Api request error", e.getMessage());
            onPostExecute(null);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (hasError) {
                showErrorDialog(errorTexts.first, errorTexts.second);
            } else {
                try {
                    if (!hasCurrent(result) && noLegacy(result)) {
                        integrityState = parseValues("");
                        legacyIntegrityState = parseValues("");

                        setIcons(integrityState);
                        legacyLayout.setVisibility(View.VISIBLE);
                        legacySwitch.setChecked(true);
                    } else if (noLegacy(result)) {
                        integrityState = parseValues(result.get("deviceRecognitionVerdict").toString());
                        legacyIntegrityState = parseValues("");

                        setIcons(integrityState);
                        legacyLayout.setVisibility(View.GONE);
                    } else if (hasCurrent(result)) {
                        integrityState = parseValues(result.get("deviceRecognitionVerdict").toString());
                        legacyIntegrityState = parseValues(result.get("legacyDeviceRecognitionVerdict").toString());

                        setIcons(integrityState);
                        legacyLayout.setVisibility(View.VISIBLE);
                        legacySwitch.setChecked(true);
                    } else {
                        integrityState = parseValues("");
                        legacyIntegrityState = parseValues(result.get("legacyDeviceRecognitionVerdict").toString());

                        setIcons(integrityState);
                        legacyLayout.setVisibility(View.VISIBLE);
                        legacySwitch.setChecked(true);
                    }

                } catch (Exception e) {
                    onBackgroundError(e);
                }
            }
            toggleButtonLoading(false);
        }
    }

    private void toggleButtonLoading(boolean isLoading) {
        setButtonLoading(btn, isLoading);
        btn.setEnabled(!isLoading);
    }

    private Drawable getProgressBarDrawable(Context context) {
        CircularProgressIndicator drawable = new CircularProgressIndicator(context);
        drawable.setIndicatorSize(48);
        drawable.setTrackThickness(5);
        drawable.setIndicatorColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.BLUE));
        drawable.setIndeterminate(true);
        return drawable.getIndeterminateDrawable();
    }

    private void setButtonLoading(MaterialButton button, boolean loading) {
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_START);

        if (loading) {
            Drawable drawable = button.getIcon();
            if (!(drawable instanceof Animatable)) {
                drawable = getProgressBarDrawable(button.getContext());
                if (drawable instanceof Animatable) {
                    button.setIcon(drawable);
                    ((Animatable) drawable).start();
                }
            }
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

    private boolean noLegacy(JSONObject deviceIntegrity) {
        return !deviceIntegrity.has("legacyDeviceRecognitionVerdict");
    }

    private boolean hasCurrent(JSONObject deviceIntegrity) {
        return deviceIntegrity.has("deviceRecognitionVerdict");
    }

    private Integer[] parseValues(String integrity) {
        if (integrity.isEmpty()) {
            return new Integer[]{0, 0, 0};
        }
        return new Integer[]{integrity.contains("MEETS_DEVICE_INTEGRITY") ? 1 : 0, integrity.contains("MEETS_BASIC_INTEGRITY") ? 1 : 0, integrity.contains("MEETS_STRONG_INTEGRITY") ? 1 : 0};
    }


    private void setIcons(Integer[] integrityState) {
        setIcon(deviceIntegrityIcon, integrityState[0]);
        setIcon(basicIntegrityIcon, integrityState[1]);
        setIcon(strongIntegrityIcon, integrityState[2]);
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
        return switch (errorCode) {
            case IntegrityErrorCode.API_NOT_AVAILABLE -> """
                    Integrity API is not available.
                    
                    The Play Store version might be old, try updating it.""";
            case IntegrityErrorCode.APP_NOT_INSTALLED -> """
                    The calling app is not installed.
                    
                    This shouldn't happen. If it does please open an issue on Github.""";
            case IntegrityErrorCode.APP_UID_MISMATCH -> """
                    The calling app UID (user id) does not match the one from Package Manager.
                    
                    This shouldn't happen. If it does please open an issue on Github.""";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> """
                    Binding to the service in the Play Store has failed.
                    
                    This can be due to having an old Play Store version installed on the device.""";
            case IntegrityErrorCode.CLIENT_TRANSIENT_ERROR ->
                    "There was a transient error in the client device.";
            case IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID ->
                    "The provided cloud project number is invalid.";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE ->
                    "Unknown internal Google server error.";
            case IntegrityErrorCode.INTERNAL_ERROR -> "Unknown internal error.";
            case IntegrityErrorCode.NETWORK_ERROR -> """
                    No available network is found.
                    
                    Please check your connection.""";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> """
                    Nonce is not encoded as a base64 web-safe no-wrap string.
                    
                    This shouldn't happen. If it does please open an issue on Github.""";
            case IntegrityErrorCode.NONCE_TOO_LONG -> "Nonce length is too long.\n" +
                    "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NONCE_TOO_SHORT -> "Nonce length is too short.\n" +
                    "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NO_ERROR -> """
                    No error has occurred.
                    
                    If you ever get this, congrats, I have no idea what it means.""";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> """
                    Play Services is not available or version is too old.
                    
                    Try installing or updating Google Play Services.""";
            case IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> """
                    Play Services needs to be updated.
                    
                    Try updating Google Play Services.""";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> """
                    No Play Store account is found on device.
                    
                    Try logging into Play Store.""";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> """
                    No Play Store app is found on device or not official version is installed.
                    
                    This app can't work without Play Store.""";
            case IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> """
                    The Play Store needs to be updated.
                    
                    Try updating Google Play Store.""";
            case IntegrityErrorCode.TOO_MANY_REQUESTS -> """
                    Google sets a daily limit of checks on all apps that use the Integrity API. That limit has been reached for today.
                    
                    Try again at midnight (12:00am PT).""";
            default -> "Unknown Error";
        };
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
                        .setPositiveButton(R.string.ok, null)
                        .setNeutralButton(R.string.copy_json, (dialogInterface, i) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("", jsonResponse);
                            clipboard.setPrimaryClip(clip);
                            dialogInterface.dismiss();
                            Toast.makeText(MainActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
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