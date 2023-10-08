package gr.nikolasspyr.integritycheck.dialogs.licenses;

import android.app.Application;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LicensesViewModel extends AndroidViewModel {

    private final MutableLiveData<List<License>> mLicenses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mAreLicensesLoading = new MutableLiveData<>();

    public LicensesViewModel(@NonNull Application application) {
        super(application);

        mLicenses.setValue(Collections.emptyList());
        mAreLicensesLoading.setValue(false);

        loadLicences();
    }

    public LiveData<List<License>> getLicenses() {
        return mLicenses;
    }

    private void loadLicences() {
        if (Boolean.TRUE.equals(mAreLicensesLoading.getValue()))
            return;

        mAreLicensesLoading.setValue(true);

        new Thread(() -> {
            try {
                AssetManager assetManager = getApplication().getAssets();
                String licensesDir = "licenses";

                String[] rawLicenses = assetManager.list(licensesDir);
                assert rawLicenses != null;
                ArrayList<License> licenses = new ArrayList<>(rawLicenses.length);

                for (String rawLicense : rawLicenses)
                    licenses.add(new License(rawLicense, readStream(assetManager.open(licensesDir + "/" + rawLicense), "UTF-8")));

                Collections.sort(licenses, (license1, license2) -> license1.subject.compareToIgnoreCase(license2.subject));

                mLicenses.postValue(licenses);
                mAreLicensesLoading.postValue(false);
            } catch (Exception e) {
                mAreLicensesLoading.postValue(false);
            }
        }).start();
    }

    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }

    public static byte[] readStream(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            return readStreamNoClose(in);
        }
    }

    public static String readStream(InputStream inputStream, String charset) throws IOException {
        return new String(readStream(inputStream), charset);
    }

    public static byte[] readStreamNoClose(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copyStream(inputStream, buffer);
        return buffer.toByteArray();
    }


}

