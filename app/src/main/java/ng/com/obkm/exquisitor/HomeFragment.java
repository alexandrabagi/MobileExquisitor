package ng.com.obkm.exquisitor;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ng.com.obkm.exquisitor.LocalSVM.svm_model;

import static ng.com.obkm.exquisitor.NegativeImageLab.resetNegList;
import static ng.com.obkm.exquisitor.PositiveImageLab.resetList;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private List<String> mImagesOnScreenList = new ArrayList<>(); // used for populateMainScreen
    private List<String> mImagesOnScreenListSaved = new ArrayList<>(); // used for saving image list between ratings
    // TODO: mark all the images on the screen as rated?

    final String TAG = "home";
    final int REQUEST_CODE = 100;

    // Training data
    protected static ArrayList<Integer> ratings = new ArrayList<>();
    private static List<float[]> trainingDataValues = new ArrayList<>();
    private static List<int[]> trainingDataLabels = new ArrayList<>();

    public static int NUMBEROFHIGHESTPROBS = 5;
    protected static int numberOfFeedback = 0;
    private int numberOfUnseenImages = -1;

    private String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
    private Cursor cursor ;
    private VectorLab vectorLab;
    private MediaStoreCheck mSC;

    private Context mContext;
    //private boolean serachStart = false; //comment in for UI  testing to start with the same 6 images


    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.i("lifecycle", "onCreate called");
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        VectorLab.get(getActivity()).removeSeen();
        setHasOptionsMenu(true);
        mSC = new MediaStoreCheck(mContext);
//        mSC.analyseNewImages();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.i("lifecycle", "onCreateView called");
        vectorLab = VectorLab.get(getActivity());
        cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media._ID);
        View v = inflater.inflate(R.layout.fragment_home_const, container, false);
        createPagerDialog();
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        //Log.i("lifecycle", "OnStart called");

        /*if(searchStart)
        {
            Log.i("lifecycle", "in if searchStart statement.");
            populateMainScreenList(makeStartScreenListForTesting());
        }
        else
        {*/
            if (numberOfFeedback < 1) { // before the first swipe, random 6 images, TODO: imageClicked
                //Log.i("lifecycle", "fix test set loaded.");
                mImagesOnScreenList = getRandomSixList();
            }
            if (!mImagesOnScreenListSaved.isEmpty()) {
                mImagesOnScreenList = mImagesOnScreenListSaved;
            }
            populateMainScreenList(mImagesOnScreenList);

      //  }
    }

    // method to return always same 6 images on screen
    /*private List<String> makeStartScreenListForTesting()
    {
        Log.i("lifecycle", "makeStartScreenListForTesting: called ");
        List<String> testScreenList = new ArrayList<>();
        testScreenList.add(getFullPath("113200.jpg"));
        addToSeenImagesFromLongPath(getFullPath("113200.jpg"));
        testScreenList.add(getFullPath("102303.jpg"));
        addToSeenImagesFromLongPath(getFullPath("102303.jpg"));
        testScreenList.add(getFullPath("106704.jpg"));
        addToSeenImagesFromLongPath(getFullPath("106704.jpg"));
        testScreenList.add(getFullPath("117701.jpg"));
        addToSeenImagesFromLongPath(getFullPath("117701.jpg"));
        testScreenList.add(getFullPath("125400.jpg"));
        addToSeenImagesFromLongPath(getFullPath("125400.jpg"));
        testScreenList.add(getFullPath("b00001353_21i6bq_20150223_213912e.jpg"));
        addToSeenImagesFromLongPath(getFullPath("b00001353_21i6bq_20150223_213912e.jpg"));

        for(int i = 0; i < testScreenList.size(); i++){
            Log.i("lifecycle", "added to start test screen images " + testScreenList.get(i));
        }
        //searchStart = false;
        return testScreenList;
    }*/

    @Override
    public void onPause() {
        super.onPause();
        //Log.i("lifecycle", "OnPause is called");
        mImagesOnScreenListSaved = mImagesOnScreenList;
        //Log.i(TAG, "Images on screen list saved");
    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.i("lifecycle","OnStop is called");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.i("lifecycle", "onActivityResult was called");
        //Log.i(TAG, "Is data null? " + (data == null));
        if (resultCode != getActivity().RESULT_OK) {
            Log.i(TAG, "Result code: " + resultCode);
            Log.i(TAG, "Abort activity1");
            return;
        }
        if (requestCode == REQUEST_CODE) {
            if (data == null) {
                Log.i(TAG, "Abort activity2");
                return;
            }
        }

        buildTrainingData(data);
        // Setting on screen image set based on trained model results
        setImagesOnScreenList(data);
    }

    protected void buildTrainingData(Intent data) {
        //Log.i(TAG, "Build training data was called");
        //long startTime0 = System.nanoTime();
        String path = PhotoItemActivity.getVectorPath(data);
        path = getShortPath(path);
        //Log.i(TAG, "Short path: " + path);
        //long elapsedTime0 = System.nanoTime() - startTime0;
        int imageID = VectorLab.getIDFromPath(path);
        //Log.i(TAG, "imageID from DB: " + imageID);
        //System.out.println("Elapsed time getVectorPath nanosec: " + elapsedTime0);

        //long startTime1 = System.nanoTime();
        float[] vectorValues = VectorLab.queryProbsAsFloats(imageID);
        //Log.i(TAG, "VectorValues: " + Arrays.toString(vectorValues));
        //long elapsedTime1 = System.nanoTime() - startTime1;
        //System.out.println("Elapsed time vectorValues nanosec: " + elapsedTime1);

        //long startTime2 = System.nanoTime();
        int[] vectorLabels = VectorLab.queryLabelsAsInts(imageID);
        Log.i(TAG, Arrays.toString(vectorLabels));
        //long elapsedTime2 = System.nanoTime() - startTime2;
        //System.out.println("Elapsed time vectorLabels nanosec: " + elapsedTime2);

        int vectorRating = PhotoItemActivity.getVectorRating(data);
        //Log.i(TAG, "Rating: " + vectorRating);

        trainingDataValues.add(vectorValues);
        //Log.i(TAG, "training data size: " + trainingDataValues.size());

        trainingDataLabels.add(vectorLabels);
        ratings.add(vectorRating);
        numberOfFeedback++;
    }

    // Load mImagesOnScreenListSaved, change the clicked image to the bestCandidate
    private void setImagesOnScreenList(Intent data) {
        String path = PhotoPagerActivity.getVectorPath(data); // path received from intent
        System.out.println("Received path in HomeFragment: " + path);
        path = getFullPath(path);
        //Log.i(TAG, "in setImagesOnScreen received path: " + path);

        int index = -1;
        //TODO: can this be deleted now?
        if (mImagesOnScreenListSaved.contains(path))
            {
            // it should contain the path, otherwise there is an error
            index = mImagesOnScreenListSaved.indexOf(path); // check what is the index of the clicked image
                //Log.i(TAG, "index from clicked image " + index);
                //Log.i(TAG, "size before removal " + mImagesOnScreenList.size());
            mImagesOnScreenListSaved.remove(path); // remove it from the images on screen
                //Log.i(TAG, "size after removal " + mImagesOnScreenList.size());

            }
        else
            {
                Log.e(TAG, "imagesOnScreen doesn't contain path"); // => ERROR -> TODO: handle error
                Log.e(TAG, "index is -1!!!!!!!!!!!!!!!!!! " + index);
            }

        if (numberOfFeedback < 2)
        {
            // we don't run SVM after the first rating (the distance would be NaN), replace clicked with random image
            String randomPath = getRandomImageFromDB();
            String longRandomPath = getFullPath(randomPath);
            System.out.println("Path in setImagesOnScreen: " + longRandomPath);
            mImagesOnScreenListSaved.add(index, randomPath);
            addToSeenImagesFromLongPath(randomPath);
        }
        else
            {
                // we run SVM after the second rating, replace clicked with bestCandidate
                svm_model model = SVM.buildModel(ratings, trainingDataValues, trainingDataLabels);
                String bestPath = getOneBestCandidateDistanceBased(model);
                String fullPathToAdd = getFullPath(bestPath);
                for(String pathtorping: mImagesOnScreenList){
                    //Log.i("here", "paths on the screen " + pathtorping);
                }
                //Log.i("here", "paths to add " + fullPathToAdd);

                //if(!mImagesOnScreenListSaved.contains(fullPathToAdd))
                   // {
                        mImagesOnScreenListSaved.add(index, fullPathToAdd);
                        //Log.i("lifecycle", "succuessfully added to images on screen saved");
                        addToSeenImagesFromLongPath(fullPathToAdd);
                    //}
                    //else Log.e(TAG, "path is already in images on screen!");
            }
    }

    private void addToSeenImagesFromLongPath(String longPath){
        //add to db as seen
        Log.i("here", "added to seen images: " + longPath);
        String shortPath = getShortPath(longPath);
       //Log.i(TAG, "path to update image short: " + shortPath);
        int imageID = VectorLab.getIDFromPath(shortPath);
        //Log.i(TAG, "path to update image short: " + imageID);
        vectorLab.updateSeen(imageID);
    }

    private List<String> getBestCandidateDistanceBased(svm_model model) {
        Log.i("bestCandidate", "getBestCandidateDistanceBased was called");

        Map<String, Double> bestCandidatePaths = new HashMap<>();

        // Map <imageID, labelList>
        Map<Integer, List<Integer>> unseenLabels = vectorLab.queryUnseenFeatures();
        // Map <imageID, probsList>
        Map<Integer, List<Float>> unseenProbs = vectorLab.queryUnseenProbs();
        Log.i(TAG, "UnseenLabels size: " + unseenLabels.keySet().size());
        Log.i(TAG, "UnseenProbs size: " + unseenProbs.keySet().size());

        // Setting up a map for the best candidates ids and distances
        Map<Integer, Double> bestCandidates = new HashMap<>(); // 6 best distances

        List<String> bestPaths;

        Iterator<Map.Entry<Integer, List<Integer>>> it = unseenLabels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<Integer>> entry = it.next();
            Integer key = entry.getKey();
            List<Float> oneProbsVectorList = unseenProbs.get(key);
            List<Integer> labelsVectorList = unseenLabels.get(key);

            //convert to arrays
            int[] testLabels = new int[labelsVectorList.size()];
            float[] testProbs = new float[oneProbsVectorList.size()];
            for(int i = 0; i < labelsVectorList.size(); i++ ) {
                testProbs[i] = oneProbsVectorList.get(i);
                testLabels[i] = labelsVectorList.get(i);
            }

            // get distance for each unseen image
             double distance = SVM.doPredictionDistanceBased(model, testProbs, testLabels);
           Log.i(TAG, "distance for bestCand: " + distance);

            if(bestCandidates.size() < 6 ) {
                //Log.i(TAG, "added to first 6 " + pairUnseenImage.getKey());
                bestCandidates.put(key, distance);
                Log.i("bestCandidate", "home: add this candidate to best 6 " + key + " distance: " + distance);

            }
            else {
                Comparator<Double> c = new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) { // -1 if o1 < o2
                        if (o1 < o2) return -1;
                        else if (o1.equals(o2)) return 0;
                        else return 1; // o1 > o2
                    }
                };
                Map.Entry<Integer, Double> min = null;
                // Getting the minimum distance value in bestCandidates
                for(Map.Entry<Integer, Double> bestEntry : bestCandidates.entrySet()) {
                    if(min == null || (c.compare(min.getValue(), bestEntry.getValue()) > 0)) {
                        min = bestEntry;
                    }
                }

                if(distance > min.getValue()) {
                    bestCandidates.remove(min.getKey());
                    bestCandidates.put(key, distance);
                }
            }
        }
        Log.i("bestCandidate", "bestCandidates size: " + bestCandidates.keySet().size());
        for (int key : bestCandidates.keySet()) {
            String path = vectorLab.getPathFromID(key);
            bestCandidatePaths.put(path, bestCandidates.get(key));
        }
        Log.i("bestCandidate", "getbestcand should be 6 " + bestCandidatePaths.size());

        Log.i("order", "not ordered best paths when feedback is " + numberOfFeedback + " : " + Arrays.toString(bestCandidatePaths.keySet().toArray()));
        bestPaths = orderBestCandidatePaths(bestCandidatePaths);
        return bestPaths;
    }

    private String getOneBestCandidateDistanceBased(svm_model model) {
        Log.i("getOneBestCandidateDistanceBased", "getBestCandidateDistanceBased was called");

        Candidate bestCandidate = new Candidate(0, 0.0);
        // Map <imageID, labelList>
        Map<Integer, List<Integer>> unseenLabels = vectorLab.queryUnseenFeatures();
        // Map <imageID, probsList>
        Map<Integer, List<Float>> unseenProbs = vectorLab.queryUnseenProbs();

        Iterator<Map.Entry<Integer, List<Integer>>> it = unseenLabels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<Integer>> entry = it.next();
            Integer key = entry.getKey();
            List<Float> oneProbsVectorList = unseenProbs.get(key);
            List<Integer> labelsVectorList = unseenLabels.get(key);

            //convert to arrays
            int[] testLabels = new int[labelsVectorList.size()];
            float[] testProbs = new float[oneProbsVectorList.size()];

            for(int i = 0; i < labelsVectorList.size(); i++ ) {
                testProbs[i] = oneProbsVectorList.get(i);
                testLabels[i] = labelsVectorList.get(i);
            }
            // get distance for each unseen image
            double distance = SVM.doPredictionDistanceBased(model, testProbs, testLabels);
//            Log.i(TAG, "distance for bestCand: " + distance);
//            Log.i("bestCandidate", "imageID of candidate" + key);
//            Log.i("bestCandidate", "old distance " + bestCandidate.distance);
//            Log.i("bestCandidate", "new distance " + distance + " old " + bestCandidate.distance);
            if(distance>bestCandidate.distance)
            {
                bestCandidate.imageID = key;
                bestCandidate.distance = distance;
                Log.i("bestCandidate", "updated distance " + bestCandidate.distance);
            }

        }
        // in case we only had negative examples and no image vector is on the positive side of the decision boundary
        if(bestCandidate.distance == 0.0)
        {
            Log.i("here", "we get random from db");
            return getRandomImageFromDB();}

        else
        {
            Log.i("here", "we use the imageID from best Candidate " + bestCandidate.imageID);
            return vectorLab.getPathFromID(bestCandidate.imageID);}

    }

    private void populateMainScreenList(List<String> imagesList) {

       try{ for(int i=0; i<imagesList.size(); i++){
            Log.i("lifecycle", "image in list: " + imagesList.get(i));
        }
        Log.i(TAG, "mImagesOnScreen size: " + imagesList.size());
        Log.i(TAG, "mImagesOnScreenSaved size: " + mImagesOnScreenListSaved.size());

        for (int i = 0; i < 6; i++) {
            String pathFromList = imagesList.get(i);
            //Log.i("lifecycle", "path in second loop " + pathFromList);
            String number = String.valueOf(i + 1);
            String name = "galleryImage".concat(number);
//            Log.i(TAG, "is the name correct " + name);
            int resID = getResources().getIdentifier(name, "id", getActivity().getPackageName());
//            Log.i(TAG, "resID " + resID);
            final ImageView myImage = (ImageView) getView().findViewById(resID);

//            Log.i(TAG, "path to update image long: " + pathFromList);

            pathFromList = getFullPath(pathFromList);
            final String path = pathFromList;
            System.out.println("Path in populateMainScreen: " + path);
            updateImageView(path, myImage);
            myImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = PhotoPagerActivity.newIntent(getActivity(), path);
                    intent.putExtra("path", path);
                    startActivityForResult(intent, REQUEST_CODE);
                }
            });
        }
       }
       catch (Exception e)
        {
            Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();

        }
    }

    private void updateImageView(String path, ImageView myImage) {
        File imgFile = new File(path);
        Log.i(TAG, "updateImageView updated path: " + path);
//        Log.i(TAG, "updateImageView imagFile exists? " + imgFile.exists());
        if (!imgFile.exists()) {
            Drawable noImageDrawable = getResources().getDrawable(R.drawable.no_photo_small);
            myImage.setImageDrawable(noImageDrawable);
            System.out.println("Delete image: " + getShortPath(path));
            vectorLab.deleteEntryFromDB(getShortPath(path));
        } else {
            Bitmap myBitmap = PictureUtils.getThunbnail(imgFile.getPath());
            myImage.setImageBitmap(myBitmap);
        }
    }

    private List<String> getRandomSixList() {
        Set<String> pathSet = new HashSet<>();
        String path = "";
        while (pathSet.size() < 6) {
            path = getRandomImageFromDB();
            pathSet.add(path);
            // mark as seen in db
            //Log.i("updateRated", "setImagesOnScreenList: path to add to unseen " + path);
            addToSeenImagesFromLongPath(path);
        }
        return new ArrayList<>(pathSet);
    }

    private void putNextSixBestIntoScreenListSaved(){
        //Log.i(TAG, "putNextSixBestINtoScreen was called");
        //Log.i(TAG, "mImagesOnScreenListSaved size " + mImagesOnScreenList.size());

            if (numberOfFeedback < 2) { // we don't run SVM after the first rating (the distance would be NaN), replace clicked with random image
                Toast.makeText(getActivity(), "Please rate at least twice", Toast.LENGTH_SHORT).show();
            } else { // we run SVM after the second rating, replace clicked with bestCandidate
                svm_model model = SVM.buildModel(ratings, trainingDataValues, trainingDataLabels);
                List<String> bestPaths = getBestCandidateDistanceBased(model);
                    // add the next best from bestCandidatePaths
                    boolean added = false;
                    //Log.i(TAG, "Initial mImagesOnScreenList size " + mImagesOnScreenList.size());
                    mImagesOnScreenList.clear();
                    for (int index = 0; index < 6; index++) {
                        String pathToAdd = bestPaths.get(index);
                        String fullPathToAdd = getFullPath(pathToAdd);
                        Log.i("bestCandidate", "PathToAdd: " + fullPathToAdd);
                        //Log.i(TAG, "mImagesOnScreenList size " + mImagesOnScreenList.size());
                        if (!mImagesOnScreenList.contains(fullPathToAdd)) {
                            mImagesOnScreenList.add(index, fullPathToAdd);
                            //Log.i(TAG, "index " + index + " path " + fullPathToAdd);
                            addToSeenImagesFromLongPath(fullPathToAdd);
                            added = true;
                            //Log.i("bestCandidate", "One best candidate path was added");
                        }
                    }
                    if (!added)
                        Log.e("bestCandidate", "No path was added from orderedBestPaths list");
            }

        mImagesOnScreenListSaved = mImagesOnScreenList;
    }



    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private String getRandomImageFromDB() {
        //Log.i("here", "getRandomImageFromDB called");
//        Map<Integer, List<Float>> unseenProbs = vectorLab.queryUnseenProbs();
        String path = "";
        if (numberOfFeedback == 0) { // very first launch
            // get random image from the phone storage
            numberOfUnseenImages = cursor.getCount();
            int image_path_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            final Random random = new Random();
//             int rnd = random.nextInt(numberOfImageVectorsInDB);
            cursor.moveToPosition(random.nextInt(numberOfUnseenImages));
            // get the path of the random image
            path = getFullPath(cursor.getString(image_path_index));
            numberOfUnseenImages--;
        } else {
//            List<Integer> allUnseenImages = new ArrayList<>(unseenProbs.keySet());
            int randomID = vectorLab.queryRandomUnseen();
            //Log.i(TAG, "Random id from query " + randomID);
//            Log.i(TAG, "how many unseen images do we have? "  + allUnseenImages.size());
            //Log.i(TAG, "how many unseen images do we have? (numberOfUnseenImages) "  + numberOfUnseenImages);
//            Random random = new Random();
//            int id = allUnseenImages.get(random.nextInt(allUnseenImages.size()));
//            path = getFullPath(vectorLab.getPathFromID(id));
            path = getFullPath(vectorLab.getPathFromID(randomID));
            numberOfUnseenImages--;
        }
        return path;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                    //Log.i("limitTest", "remaining unseen images " + vectorLab.queryUnseenProbs().size());
                try{
                    mImagesOnScreenList = getRandomSixList();
                    populateMainScreenList(mImagesOnScreenList);
                    mImagesOnScreenListSaved = mImagesOnScreenList;
                    return true;
                }
                catch (Exception e)
                {
                Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
                }
            case R.id.startOver:
                //Log.i(TAG, "startOver was pressed");
                cleanAllForStartOver();
                return true;
            case R.id.next_Best:
                //Log.i(TAG, "nextBest was pressed");

                   try{ Log.i("limitTest", "remaining unseen images " + vectorLab.queryUnseenProbs().size());
                    putNextSixBestIntoScreenListSaved();
                    populateMainScreenList(mImagesOnScreenList);
                    mImagesOnScreenListSaved = mImagesOnScreenList;}
                   catch (Exception e)
                   {
                       Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
                   }
                    return true;
               /* else {
                    Log.i("limitTest", "no unseen images " + vectorLab.queryUnseenProbs().size());
                    Toast.makeText(getActivity(), "You have seen a all images. Please start a new search. To continue", Toast.LENGTH_SHORT).show();
                    return false;
                }*/
            case R.id.helpButton:
                //Log.i(TAG, "help was pressed");
                openHelpDialog();
                return true;
            case R.id.updatepButton:
                System.out.println("Last id: " + vectorLab.getLastImageID());
                mSC.analyseNewImages();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void cleanAllForStartOver() {
        //Log.i(TAG, "cleanAll was called");
        vectorLab.removeSeen();
        resetNegList();
        resetList();
        trainingDataValues.clear();
        //Log.i(TAG, "training values size " + trainingDataValues.size());
        trainingDataLabels.clear();
        ratings.clear();
        List<String> randomPathList = getRandomSixList();
        populateMainScreenList(randomPathList);
        mImagesOnScreenList = randomPathList;
        mImagesOnScreenListSaved = mImagesOnScreenList;
        numberOfFeedback = 0;
    }

    protected static void removeTrainingExample(int imageID) {
        //Log.i("lifecycle","RemoveTrainingExample was called");
        //Log.i("lifecycle","training data size1: " + trainingDataValues.size());
        int[] labelsToRemove = VectorLab.queryLabelsAsInts(imageID);
        float[] valuesToRemove = VectorLab.queryProbsAsFloats(imageID);
        for (int i = 0; i < trainingDataLabels.size(); i++) {
            if (Arrays.equals(labelsToRemove, trainingDataLabels.get(i))) {
                trainingDataLabels.remove(trainingDataLabels.get(i));
                ratings.remove(i);
            }
        }
        for (int i = 0; i < trainingDataValues.size(); i++) {
            if (Arrays.equals(valuesToRemove, trainingDataValues.get(i))) {
                trainingDataValues.remove(trainingDataValues.get(i));
            }
        }
//        System.out.println("training data size2: " + trainingDataValues.size());
    }


    ////// HELPERS ////////
    protected static String getFullPath(String shortPath) {
        String fullPath = "";
        /*if(shortPath.startsWith("Screen")) shortPath = "Screenshots/" + shortPath;
        else shortPath = "Camera/" + shortPath;*/
        if (!shortPath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString())) {
            fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/" + shortPath;
            return fullPath;
        } else return shortPath;
    }

    protected static String getShortPath(String fullPath) {
        String shortPath = "";
        //String directoryPath;
        /*if(fullPath.contains("Camera"))  directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/";
        else directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Screenshots/";*/
        if(fullPath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString())) {
            shortPath = fullPath.replace( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/", "");
            return shortPath;
        }
        return fullPath;
    }

    // From https://stackoverflow.com/questions/28163279/sort-map-by-value-in-java
    private List<String> orderBestCandidatePaths(Map<String, Double> bestCandPaths) {
        //Log.i("order", "orderBestCandidatePaths was called");
//        Log.i(TAG, "Not ordered best paths: " + Arrays.toString(bestCandPaths.keySet().toArray()));
//        Log.i(TAG, "Not ordered best distances: " + Arrays.toString(bestCandPaths.values().toArray()));

        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(bestCandPaths.entrySet());
        List<String> orderedBestPaths = new ArrayList<>();

        Comparator<Map.Entry<String, Double>> comp = new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2 ) {
                return ( o1.getValue()).compareTo( o2.getValue() );
            }
        };

        Collections.sort(list, comp );
        for (Map.Entry<String, Double> entry : list) {
            orderedBestPaths.add(entry.getKey());
        }
//        Log.i(TAG, "Ordered best paths: " + Arrays.toString(orderedBestPaths.toArray()));
        return orderedBestPaths;
    }

    private void createPagerDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.intro_dialog);

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.70);

        dialog.getWindow().setLayout(width, height);

        final MyPagerAdapter adapter = new MyPagerAdapter(getActivity(), dialog);
        final ViewPager pager = (ViewPager) dialog.findViewById(R.id.dialog_pager);
        // Credit: https://stackoverflow.com/questions/20586619/android-viewpager-with-bottom-dots
        TabLayout tabLayout = (TabLayout) dialog.findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(pager, true);
        pager.setAdapter(adapter);

        dialog.show();
    }

    private void openHelpDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.help);

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.70);

        dialog.getWindow().setLayout(width, height);

        Button closeButton = dialog.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
