package ng.com.obkm.exquisitor;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.Date;

public class SharedPreferenceUtil {

    protected static void writeToSharedPreferences(Context c){
        Date d = new Date();
        long dateInMilliSec  = d.getTime();
        PreferenceManager.getDefaultSharedPreferences(c)
                .edit()
                .putLong("lastAnalysis", dateInMilliSec).apply();
        System.out.println("Written to shared pref: " + dateInMilliSec);
    }


    protected static long getLastAnalysisInMilliSecSharedPreferences(Context c){
        long lastUpdate = PreferenceManager.getDefaultSharedPreferences(c)
                .getLong("lastAnalysis", 0);
        return lastUpdate;
    }
}
