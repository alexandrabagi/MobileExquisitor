package ng.com.obkm.exquisitor;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *  Singleton for storing the group of images that has been evaluated positively.
 */

public class PositiveImageLab {

    private static PositiveImageLab sPositiveImageLab;

    private Context mContext;
    private static List<String> mPositivePaths;

    public static PositiveImageLab get(Context context) {
        if (sPositiveImageLab == null) {
            sPositiveImageLab = new PositiveImageLab(context);
        }
        return sPositiveImageLab;
    }

    private PositiveImageLab(Context context) {
        mContext = context.getApplicationContext();
        mPositivePaths = new ArrayList<>();
    }

    public List<String> getPositiveImagePaths() {
        return mPositivePaths;
    }

    public void addPositiveImage(String path) {
        mPositivePaths.add(path);
    }

    public static void resetList(){
        mPositivePaths.clear();
    }

    public void removePositiveImage(String path) {
        //Log.i("SWIPE", "Positive image lab: add Pos image was called."  );
        mPositivePaths.remove(path);
    }

    public int getPositiveLabSize() {
        return mPositivePaths.size();
    }
}
