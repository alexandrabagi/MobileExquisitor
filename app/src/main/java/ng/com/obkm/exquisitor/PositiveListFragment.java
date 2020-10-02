package ng.com.obkm.exquisitor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.List;

public class PositiveListFragment extends Fragment {

    private static String TAG = "PositiveListFragment";

    private RecyclerView mImageRecyclerView;
    private ImageAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("home", "OnCreateView was called");
        View view = inflater.inflate(R.layout.fragment_positive_list, container, false);
        mImageRecyclerView = (RecyclerView) view
                .findViewById(R.id.positive_recycler_view);
        mImageRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        ItemSpacesDecoration itemDecoration = new ItemSpacesDecoration(getActivity(), R.dimen.item_space);
        mImageRecyclerView.addItemDecoration(itemDecoration);

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.i("home", "PositiveListFragment OnResume() is called");
    }

    @Override
    public void onPause() {
        super.onPause();
//        Log.i("home", "PositiveListFragment OnPause is called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        Log.i("home", "PositiveListFragment OnDestroy is called");
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        Log.i("home", "PositiveListFragment OnDetach is called");
    }

    @Override
    public void onStop() {
        super.onStop();
//        Log.i("home", "PositiveFragment onStop called");
    }

    private void updateUI() {
        PositiveImageLab positiveLab = PositiveImageLab.get(getActivity());
//        List<PhotoItem> images = positiveLab.getPositiveImages();
        List<String> imagePaths = positiveLab.getPositiveImagePaths();
        Log.i(TAG, "PositiveImagesSize in PositiveListFragment: " + imagePaths.size());
        for(int i = 0; i <imagePaths.size(); i++)
        {
            Log.i( "lifecycle","images in pos list " + imagePaths.get(i));
        }
        mAdapter = new ImageAdapter(imagePaths);
        mImageRecyclerView.setAdapter(mAdapter);
    }

    public class ImageHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;
        private PhotoItem mPhotoItem;

        public void bind(String path) {
            mPhotoItem  = new PhotoItem(path);

//            Bitmap imageBitmap = mPhotoItem.getBitmap();
            Bitmap imageBitmap = PictureUtils.getThunbnail(path);
            mImageView.setImageBitmap(imageBitmap);
        }

        private ImageHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.grid_item_image, parent, false));

            mImageView = (ImageView) itemView.findViewById(R.id.grid_image);
        }
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageHolder> {

        private List<String> mImagePaths;

        public ImageAdapter(List<String> paths) {
            mImagePaths = paths;
        }

        @Override
        public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new ImageHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(ImageHolder holder, int position) {
            final String path = mImagePaths.get(position);
            holder.bind(path);
         /*   holder.mImageView.setOnLongClickListener(new View.OnLongClickListener() {

                // based on: https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
                @Override
                public boolean onLongClick(View view) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            getContext());

                    // set title
                    alertDialogBuilder.setTitle("Remove rating");
                    // set dialog message
                    alertDialogBuilder
                            .setMessage("Do you want to remove the image from the list of rated images?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, close
                                    // current activity
                                    PositiveImageLab.get(getActivity()).removePositiveImage(path);
                                    String shortPath = HomeFragment.getShortPath(path);
                                    int imageID = VectorLab.getIDFromPath(shortPath);
                                    VectorLab.removeSeenForID(imageID);
                                    HomeFragment.numberOfFeedback--;
                                    HomeFragment.removeTrainingExample(imageID);
                                    dialog.cancel();
                                    updateUI();
                                }
                            })
                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel();
                                }
                            });
                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                    return true;
                }
            });*/

            holder.mImageView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            getContext());

                    // set title
                    alertDialogBuilder.setTitle("Remove rating");
                    // set dialog message
                    alertDialogBuilder
                            .setMessage("Do you want to remove the image from the list of rated images?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, close
                                    // current activity
                                    PositiveImageLab.get(getActivity()).removePositiveImage(path);
                                    String shortPath = HomeFragment.getShortPath(path);
                                    int imageID = VectorLab.getIDFromPath(shortPath);
                                    VectorLab.removeSeenForID(imageID);
                                    HomeFragment.numberOfFeedback--;
                                    HomeFragment.removeTrainingExample(imageID);
                                    dialog.cancel();
                                    updateUI();
                                }
                            })
                            .setNegativeButton("No",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel();
                                }
                            });
                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                    /*File file = new File(path);
                    Intent photoIntent = new Intent();
                    photoIntent.setAction(android.content.Intent.ACTION_VIEW);
                    photoIntent.setType("image/*");
                    photoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    photoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(MainActivity.getContext(),  BuildConfig.APPLICATION_ID + ".provider", file);
                    photoIntent.setDataAndType(contentUri, "image/*");
                    startActivity(photoIntent);*/
                }
            });
        }

        @Override
        public int getItemCount() {
            return mImagePaths.size();
        }
    }

    public class ItemSpacesDecoration extends RecyclerView.ItemDecoration {

        private int mSpace;

        public ItemSpacesDecoration(int space) {
            this.mSpace = space;
        }

        public ItemSpacesDecoration(@NonNull Context context, @DimenRes int itemSpace) {
            this(context.getResources().getDimensionPixelSize(itemSpace));
        }

        // credit: https://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing
        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(mSpace/2, mSpace, mSpace/2, mSpace);

            // Add top margin only for the first item to avoid double mSpace between items
            if (parent.getChildLayoutPosition(view) == 0) {
                outRect.top = mSpace;
            } else {
                outRect.top = 0;
            }
        }
    }
}
