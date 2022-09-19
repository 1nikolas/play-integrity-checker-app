package gr.nikolasspyr.integritycheck;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class Utils {
    public static void openLink(String url, Context context) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) { //For devices that have no browsers
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show();
        }
    }
}
