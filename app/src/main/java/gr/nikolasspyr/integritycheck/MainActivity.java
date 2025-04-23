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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.play.core.integrity.IntegrityServiceException;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import gr.nikolasspyr.integritycheck.dialogs.AboutDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btn;
    private ImageView deviceIntegrityIcon;
    private ImageView basicIntegrityIcon;
    private ImageView strongIntegrityIcon;

    private ImageView virtualIntegrityIcon;
    private TextView virtualIntegrityText;

    private Group legacyLayout;
    private SwitchMaterial legacySwitch;

    private String jsonResponse;
    private Integer[] integrityState = {-1, -1, -1, -1};
    private Integer[] legacyIntegrityState = {-1, -1, -1, -1};

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

        basicIntegrityIcon = findViewById(R.id.basic_integrity_icon);
        deviceIntegrityIcon = findViewById(R.id.device_integrity_icon);
        strongIntegrityIcon = findViewById(R.id.strong_integrity_icon);

        virtualIntegrityIcon = findViewById(R.id.virtual_integrity_icon);
        virtualIntegrityText = findViewById(R.id.virtual_integrity_text);

        legacyLayout = findViewById(R.id.legacy_row);
        legacySwitch = findViewById(R.id.legacy_switch);


        btn.setOnClickListener(view -> {
            toggleButtonLoading(true);

            jsonResponse = null;
            legacyLayout.setVisibility(View.GONE);

            integrityState = new Integer[]{-1, -1, -1, -1};
            legacyIntegrityState = new Integer[]{-1, -1, -1, -1};

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
        Task<IntegrityTokenResponse> integrityTokenResponseTask = integrityManager.requestIntegrityToken(
                IntegrityTokenRequest.builder()
                        .setNonce(nonce)
                        .build());

        integrityTokenResponseTask.addOnSuccessListener(integrityTokenResponse -> sendTokenRequest(integrityTokenResponse.token()));

        integrityTokenResponseTask.addOnFailureListener(e -> {
            toggleButtonLoading(false);

            String errorMessage;
            if (e instanceof IntegrityServiceException) {
                errorMessage = getErrorMessageText((IntegrityServiceException) e);
            } else {
                errorMessage = e.getMessage();
            }

            showErrorDialog(getString(R.string.token_error_title), errorMessage);
        });
    }

    private void sendTokenRequest(String token) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .get()
                .url(BuildConfig.API_URL + "/api/check?token=" + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                onRequestError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onRequestError(String.format(Locale.US, getString(R.string.server_api_error_status_code), response.code()));
                    return;
                }

                ResponseBody responseBody = response.body();

                if (responseBody == null) {
                    onRequestError(getString(R.string.server_api_error_empty_res));
                    return;
                }

                String responseBodyString = responseBody.string();

                try {
                    parseResponseJSON(responseBodyString);
                } catch (Exception e) {
                    onRequestError(e.getMessage());
                }

            }
        });
    }

    private void onRequestError(String error) {
        runOnUiThread(() -> {
            showErrorDialog(getString(R.string.server_api_error_title), error);
            toggleButtonLoading(false);
        });
    }

    private void parseResponseJSON(String apiResponseJson) throws Exception {
        JSONObject json = new JSONObject(apiResponseJson);

        if (json.has("error")) {
            throw new Exception(json.getString("error"));
        }

        JSONObject result;

        if (json.has("deviceIntegrity")) {
            result = json.getJSONObject("deviceIntegrity");
            jsonResponse = json.toString(4);
        } else {
            result = new JSONObject();
        }

        if (!hasCurrent(result) && noLegacy(result)) {
            integrityState = parseValues("");
            legacyIntegrityState = parseValues("");

            runOnUiThread(() -> {
                setIcons(legacyIntegrityState);
                legacyLayout.setVisibility(View.VISIBLE);
                legacySwitch.setChecked(false);
            });
        } else if (noLegacy(result)) {
            integrityState = parseValues(result.get("deviceRecognitionVerdict").toString());
            legacyIntegrityState = parseValues("");

            runOnUiThread(() -> {
                setIcons(integrityState);
                legacyLayout.setVisibility(View.GONE);
            });
        } else if (hasCurrent(result)) {
            integrityState = parseValues(result.get("deviceRecognitionVerdict").toString());
            legacyIntegrityState = parseValues(result.get("legacyDeviceRecognitionVerdict").toString());

            runOnUiThread(() -> {
                setIcons(legacyIntegrityState);
                legacyLayout.setVisibility(View.VISIBLE);
                legacySwitch.setChecked(false);
            });
        } else {
            integrityState = parseValues("");
            legacyIntegrityState = parseValues(result.get("legacyDeviceRecognitionVerdict").toString());

            runOnUiThread(() -> {
                setIcons(legacyIntegrityState);
                legacyLayout.setVisibility(View.VISIBLE);
                legacySwitch.setChecked(false);
            });
        }

        runOnUiThread(() -> toggleButtonLoading(false));
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
        return new Integer[]{integrity.contains("MEETS_BASIC_INTEGRITY") ? 1 : 0, integrity.contains("MEETS_DEVICE_INTEGRITY") ? 1 : 0, integrity.contains("MEETS_STRONG_INTEGRITY") ? 1 : 0, integrity.contains("MEETS_VIRTUAL_INTEGRITY") ? 1 : -1};
    }


    private void setIcons(Integer[] integrityState) {
        setIcon(basicIntegrityIcon, integrityState[0]);
        setIcon(deviceIntegrityIcon, integrityState[1]);
        setIcon(strongIntegrityIcon, integrityState[2]);
        setIcon(virtualIntegrityIcon, integrityState[3]);

        if (integrityState[3] != -1) {
            setVirtualIntegrityVisibility(View.VISIBLE);
        } else {
            setVirtualIntegrityVisibility(View.GONE);
        }
    }

    private void setVirtualIntegrityVisibility(int visibility) {
        virtualIntegrityIcon.setVisibility(visibility);
        virtualIntegrityText.setVisibility(visibility);
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

    private String getErrorCodeName(int errorCode) {
        return switch (errorCode) {
            case IntegrityErrorCode.NO_ERROR -> "NO_ERROR";
            case IntegrityErrorCode.API_NOT_AVAILABLE -> "API_NOT_AVAILABLE";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> "PLAY_STORE_NOT_FOUND";
            case IntegrityErrorCode.NETWORK_ERROR -> "NETWORK_ERROR";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> "PLAY_STORE_ACCOUNT_NOT_FOUND";
            case IntegrityErrorCode.APP_NOT_INSTALLED -> "APP_NOT_INSTALLED";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> "PLAY_SERVICES_NOT_FOUND";
            case IntegrityErrorCode.APP_UID_MISMATCH -> "APP_UID_MISMATCH";
            case IntegrityErrorCode.TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> "CANNOT_BIND_TO_SERVICE";
            case IntegrityErrorCode.NONCE_TOO_SHORT -> "NONCE_TOO_SHORT";
            case IntegrityErrorCode.NONCE_TOO_LONG -> "NONCE_TOO_LONG";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "GOOGLE_SERVER_UNAVAILABLE";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> "NONCE_IS_NOT_BASE64";
            case IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> "PLAY_STORE_VERSION_OUTDATED";
            case IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED ->
                    "PLAY_SERVICES_VERSION_OUTDATED";
            case IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID ->
                    "CLOUD_PROJECT_NUMBER_IS_INVALID";
            case IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> "CLIENT_TRANSIENT_ERROR";
            case IntegrityErrorCode.INTERNAL_ERROR -> "INTERNAL_ERROR";
            default -> "UNKNOWN_ERROR_CODE";
        };
    }

    private String getErrorReason(int errorCode) {
        return switch (errorCode) {
            case IntegrityErrorCode.API_NOT_AVAILABLE ->
                    getString(R.string.error_reason_api_not_available);
            case IntegrityErrorCode.APP_NOT_INSTALLED ->
                    getString(R.string.error_reason_app_not_installed);
            case IntegrityErrorCode.APP_UID_MISMATCH ->
                    getString(R.string.error_reason_app_uid_mismatch);
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE ->
                    getString(R.string.error_reason_cannot_bind_to_service);
            case IntegrityErrorCode.CLIENT_TRANSIENT_ERROR ->
                    getString(R.string.error_reason_client_transient_error);
            case IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID ->
                    getString(R.string.error_reason_cloud_project_number_is_invalid);
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE ->
                    getString(R.string.error_reason_google_server_unavailable);
            case IntegrityErrorCode.INTERNAL_ERROR ->
                    getString(R.string.error_reason_internal_error);
            case IntegrityErrorCode.NETWORK_ERROR -> getString(R.string.error_reason_network_error);
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64 ->
                    getString(R.string.error_reason_nonce_is_not_base64);
            case IntegrityErrorCode.NONCE_TOO_LONG ->
                    getString(R.string.error_reason_nonce_too_long);
            case IntegrityErrorCode.NONCE_TOO_SHORT ->
                    getString(R.string.error_reason_nonce_too_short);
            case IntegrityErrorCode.NO_ERROR -> "";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND ->
                    getString(R.string.error_reason_play_services_not_found);
            case IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED ->
                    getString(R.string.error_reason_play_services_outdated);
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND ->
                    getString(R.string.error_reason_play_store_account_not_found);
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND ->
                    getString(R.string.error_reason_play_store_not_found);
            case IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED ->
                    getString(R.string.error_reason_play_store_version_outdated);
            case IntegrityErrorCode.TOO_MANY_REQUESTS ->
                    getString(R.string.error_reason_too_many_requests);

            default -> getString(R.string.error_reason_unknown);
        };
    }

    private String getErrorSolution(int errorCode) {
        return switch (errorCode) {
            case IntegrityErrorCode.API_NOT_AVAILABLE, IntegrityErrorCode.CANNOT_BIND_TO_SERVICE,
                 IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED ->
                    getString(R.string.error_solution_update_play_store);
            case IntegrityErrorCode.APP_NOT_INSTALLED, IntegrityErrorCode.APP_UID_MISMATCH ->
                    getString(R.string.error_solution_something_wrong_attack);
            case IntegrityErrorCode.CLIENT_TRANSIENT_ERROR,
                 IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE, IntegrityErrorCode.INTERNAL_ERROR ->
                    getString(R.string.error_solution_try_again);
            case IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID,
                 IntegrityErrorCode.NONCE_IS_NOT_BASE64, IntegrityErrorCode.NONCE_TOO_LONG,
                 IntegrityErrorCode.NONCE_TOO_SHORT, IntegrityErrorCode.NO_ERROR ->
                    getString(R.string.error_solution_open_issue);
            case IntegrityErrorCode.NETWORK_ERROR ->
                    getString(R.string.error_solution_check_connection);
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND ->
                    getString(R.string.error_solution_install_update_play_services);
            case IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED ->
                    getString(R.string.error_solution_update_play_services);
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND ->
                    getString(R.string.error_solution_login);
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND ->
                    getString(R.string.error_solution_install_official_play_store);
            case IntegrityErrorCode.TOO_MANY_REQUESTS ->
                    getString(R.string.error_solution_try_again_later);

            default -> "";
        };
    }

    private String getErrorMessageText(IntegrityServiceException integrityServiceException) {
        int errorCode = integrityServiceException.getErrorCode();

        StringBuilder errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append(String.format(Locale.US, "%s (%d)", getErrorCodeName(errorCode), errorCode));

        String errorReason = getErrorReason(errorCode);
        if (!errorReason.isEmpty()) {
            errorMessageBuilder.append("\n");
            errorMessageBuilder.append(errorReason);
        }

        String errorSolution = getErrorSolution(errorCode);
        if (!errorSolution.isEmpty()) {
            errorMessageBuilder.append("\n\n");
            errorMessageBuilder.append(errorSolution);
        }

        return errorMessageBuilder.toString();
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