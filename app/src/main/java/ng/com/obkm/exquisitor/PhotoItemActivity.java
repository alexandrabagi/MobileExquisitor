package ng.com.obkm.exquisitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class PhotoItemActivity extends AppCompatActivity {

    private static final String TAG = "singlePhoto";

    private String path = "";
    private PhotoItem mItem;
    private static final String VECTOR_VALUES = "ng.com.obkm.exquisitor.verctor_values";
    private static final String VECTOR_RATING = "ng.com.obkm.exquisitor.verctor_rating";
    private static final String VECTOR_LABELS = "ng.com.obkm.exquisitor.verctor_labels";
    private static final String VECTOR_PATH = "ng.com.obkm.exquisitor.verctor_path";


    // presets for rgb conversion
    private static final int RESULTS_TO_SHOW = 3;

    // activity elements
    private ImageView selectedImage;
    private TextView label1;
    private TextView label2;
    private TextView label3;
    private TextView confidence1;
    private TextView confidence2;
    private TextView confidence3;

    private List<String> labelList;

    private DecimalFormat df = new DecimalFormat();


    // priority queue that will hold the top results from the CNN
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_photo);
        //Log.i(TAG, "OnCreate was called.");

        /*try {
            labelList = loadLabelList();
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/

        // labels that hold top three results of CNN
        label1 = (TextView) findViewById(R.id.label1);
        //label2 = (TextView) findViewById(R.id.label2);
        //label3 = (TextView) findViewById(R.id.label3);
        // displays the probabilities of top labels
        confidence1 = (TextView) findViewById(R.id.confidence1);
        //confidence3 = (TextView) findViewById(R.id.confidence3);
        //initialize imageView that displays selected image to the user
        selectedImage = (ImageView) findViewById(R.id.selectedImage);

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        //Log.i(TAG, "Path received from intent: " + path);

        mItem = new PhotoItem(path);
        Log.i(TAG, "Path: " + path);

        final ImageView myImage = (ImageView) findViewById(R.id.selectedImage);
        updateImageView(path, myImage);

        //printTopKLabels();

        myImage.setOnTouchListener(new OnSwipeTouchListener(PhotoItemActivity.this) {
                public void onSwipeTop() {
                    Toast.makeText(PhotoItemActivity.this, "You rated the image as neutral", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PhotoItemActivity.this, MainActivity.class);
                    setResult(Activity.RESULT_OK, intent);
                }
                public void onSwipeRight() {
                    Toast.makeText(PhotoItemActivity.this, "You rated the image as positive", Toast.LENGTH_SHORT).show();
                    PositiveImageLab.get(getApplicationContext()).addPositiveImage(path);

//                    String vectorValues = VectorLab.get(getBaseContext()).queryEntireFeatures(path);
//                    String vectorLabels = VectorLab.get(getBaseContext()).queryBestFeatures(path);

                    Intent intent = new Intent(PhotoItemActivity.this, MainActivity.class);
                    intent.putExtra(VECTOR_PATH, path);
                    Log.i(TAG, "swipe right path " + path);
                    //intent.putExtra(VECTOR_VALUES, vectorValues);
                    intent.putExtra(VECTOR_RATING, 1);
                    //intent.putExtra(VECTOR_LABELS, vectorLabels);

                    setResult(Activity.RESULT_OK, intent);
                    Log.i("PIA", "Result is set to OK");
//                    VectorLab.get(getBaseContext()).updateRated(path);
                    Log.i("NEW SWIPE", "right swipe");

                    finish();
                }
                public void onSwipeLeft() {
                    Toast.makeText(PhotoItemActivity.this, "You rated the image as negative", Toast.LENGTH_SHORT).show();
                    NegativeImageLab.get(getApplicationContext()).addNegativeImage(path);
//                    Log.i(TAG,"NegativeImageLabSize: " + NegativeImageLab.get(getApplicationContext()).getNegativeImageLabSize());
                    //String vectorValues = VectorLab.get(getBaseContext()).queryEntireFeatures(path);
                    //String vectorLabels = VectorLab.get(getBaseContext()).queryBestFeatures(path);

                    Intent intent = new Intent(PhotoItemActivity.this, MainActivity.class);
                    intent.putExtra(VECTOR_PATH, path);
                    Log.i(TAG, "swipe left path " + path);
                    //intent.putExtra(VECTOR_VALUES, vectorValues);
                    intent.putExtra(VECTOR_RATING, -1);
                    //intent.putExtra(VECTOR_LABELS, vectorLabels);

                    setResult(Activity.RESULT_OK, intent);
//                    VectorLab.get(getBaseContext()).updateRated(path);
                    Log.i("NEW SWIPE", "left swipe");
                    finish();
                }
                public void onSwipeBottom() {
                    Toast.makeText(PhotoItemActivity.this, "Back", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PhotoItemActivity.this, MainActivity.class);
                    setResult(Activity.RESULT_CANCELED, intent);
                    finish();
                }

            });
    }

    public static String getVectorValues(Intent intent) {
        return intent.getStringExtra(VECTOR_VALUES);
    }

    public static String getVectorPath(Intent intent) {
        return intent.getStringExtra(VECTOR_PATH);
    }

    // LOOK
    public static int getVectorRating(Intent intent) {
        return intent.getIntExtra(VECTOR_RATING, 0);
    }

    public static String getVectorLabels(Intent intent) {
        return intent.getStringExtra(VECTOR_LABELS);
    }

//    // Turn String values to Float array
   private float[] getFloatValues(String values) {
        Log.i("tflite", "whats wrong " + values);
        String[] stringValues = values.replace("[", "").replace("]", "").split(", ");
        float[] floatValues = new float[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            floatValues[i] = Float.valueOf(stringValues[i])*100;
        }
        Log.i(TAG, "Float values: " + floatValues);
        return floatValues;
    }

    // appears also in HomeFragment.java
    private void updateImageView(String path, ImageView myImage) {
        File imgFile = new File(path);
        if (imgFile == null || !imgFile.exists()) {
            myImage.setImageDrawable(null);
        } else {
            Bitmap myBitmap = PictureUtils.getScaledBitmap(
                    imgFile.getPath(), this
            );
            myImage.setImageBitmap(myBitmap);
        }
    }

//    private void printTopKLabels() {
//        String labels = VectorLab.get(getApplicationContext()).queryLabels(mItem.getImagePath());
//        String probs = VectorLab.get(getApplicationContext()).queryProbsString(mItem.getImagePath());
//        label1.setText((labels));
//        confidence1.setText(probs);
//    }

}
