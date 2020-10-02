package ng.com.obkm.exquisitor;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

public class PhotoItemFragment extends Fragment {

    private ImageView mPhotoItem;

    private static final String ARG_PHOTO_PATH = "photo_path";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_item, container, false);

        mPhotoItem = (ImageView) v.findViewById(R.id.photo_item);
        String path = getArguments().getString(ARG_PHOTO_PATH);
        updateImageView(path, mPhotoItem);

        return v;
    }

    public static PhotoItemFragment newInstance(String photoPath) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO_PATH, photoPath);
        PhotoItemFragment fragment = new PhotoItemFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // appears also in HomeFragment.java
    private void updateImageView(String path, ImageView myImage) {
        File imgFile = new File(path);
        if (imgFile == null || !imgFile.exists()) {
            myImage.setImageDrawable(null);
        } else {
            Bitmap myBitmap = PictureUtils.getScaledBitmap(
                    imgFile.getPath(), getActivity()
            );
            myImage.setImageBitmap(myBitmap);
        }
    }
}
