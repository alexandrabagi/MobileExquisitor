package ng.com.obkm.exquisitor;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NegativeImageLab {

    private static NegativeImageLab sNegativeImageLab;

    private Context mContext;
    private static List<String> mNegativePaths;

    public static NegativeImageLab get(Context context) {
        if (sNegativeImageLab == null) {
            sNegativeImageLab = new NegativeImageLab(context);
        }
        return sNegativeImageLab;
    }

    private NegativeImageLab(Context context) {
        mContext = context.getApplicationContext();
        mNegativePaths = new ArrayList<>();
    }

    public List<String> getNegativeImagePaths() {
        return mNegativePaths;
    }

    public void addNegativeImage(String path) {
        //Log.i("SWIPE", "Positive image lab: add Pos image was called."  );
        mNegativePaths.add(path);
    }

    public void removeNegativeImage(String path) {
        //Log.i("SWIPE", "Positive image lab: add Pos image was called."  );
        Log.i("ImageLab", "path to remove " + path);
        Log.i("ImageLab", "first path in list " + mNegativePaths.get(0));

        mNegativePaths.remove(path);
    }

    public static void resetNegList(){
        mNegativePaths.clear();
    }

    public int getNegativeLabSize() {
        return mNegativePaths.size();
    }
}
