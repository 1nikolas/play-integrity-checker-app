package gr.nikolasspyr.integritycheck.viewmodels;

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

import gr.nikolasspyr.integritycheck.entities.License;
import gr.nikolasspyr.integritycheck.entities.StreamUtils;  // Move Method

public class LicensesViewModel extends AndroidViewModel {
    
    // Rename method/variable Refactoring
    // Changing the variable name to NumberOfLincenses instead of mlicences as the variable doesnt make sense
    private final MutableLiveData<List<License>> NumberOfLincenses = new MutableLiveData<>();  
    // Changing the variable name to AreLicensesLoading instead of AreLicensesLoading as the variable doesnt make sense
    private final MutableLiveData<Boolean> AreLicensesLoading = new MutableLiveData<>();

    public LicensesViewModel(@NonNull Application application) {
        super(application);

        NumberOfLincenses.setValue(Collections.emptyList());
        AreLicensesLoading.setValue(false);

        loadLicences();
    }

    public LiveData<List<License>> getLicenses() {
        return NumberOfLincenses;
    }

    private void loadLicences() {
        if (Boolean.TRUE.equals(AreLicensesLoading.getValue()))
            return;
    
        AreLicensesLoading.setValue(true);
    
        new Thread(() -> {
            try {
                AssetManager assetManager = getApplication().getAssets();
                String licensesDir = "licenses";
    
                ArrayList<License> licenses = readLicenses(assetManager, licensesDir);  // Extract Method Refactoring
                sortLicenses(licenses); // Example of extract Method Refactoring
    
                NumberOfLincenses.postValue(licenses);
                AreLicensesLoading.postValue(false);
            } catch (Exception e) {
                AreLicensesLoading.postValue(false);
            }
        }).start();
    }
    
    private ArrayList<License> readLicenses(AssetManager assetManager, String licensesDir) throws IOException {
        String[] rawLicenses = assetManager.list(licensesDir);
        ArrayList<License> licenses = new ArrayList<>(rawLicenses.length);
    
        for (String rawLicense : rawLicenses)
            licenses.add(new License(rawLicense, readStream(assetManager.open(licensesDir + "/" + rawLicense), "UTF-8")));
    
        return licenses;
    }
    
    private void sortLicenses(ArrayList<License> licenses) {
        Collections.sort(licenses, new LicenseComparator());
    }

    public class LicenseComparator implements Comparator<License> {
        @Override
        public int compare(License license1, License license2) {
            return license1.getSubject().compareToIgnoreCase(license2.getSubject());
        }
    }

    // Move Method Refactoring
    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        StreamUtils.copyStream(from, to); // Move Method
    }

    // Move Method Refactoring
    public static byte[] readStream(InputStream inputStream) throws IOException {
        return StreamUtils.readStream(inputStream); // Move Method
    }

    // Move Method Refactoring
    public static String readStream(InputStream inputStream, String charset) throws IOException {
        return StreamUtils.readStream(inputStream, charset); // Move Method
    }

    // Move Method Refactoring
    public static byte[] readStreamNoClose(InputStream inputStream) throws IOException {
        return StreamUtils.readStreamNoClose(inputStream); // Move Method
    }


}

