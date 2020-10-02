package ng.com.obkm.exquisitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PhotoPagerActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private List<String> mPhotoPaths = new ArrayList<>();
    private String path; // chosen photo path
    private Intent intent;

    private static final String EXTRA_PHOTO_PATH = "ng.com.obkm.exquisitor.photo_path";
    private static final String VECTOR_PATH = "ng.com.obkm.exquisitor.verctor_path";
    private static final String VECTOR_RATING = "ng.com.obkm.exquisitor.verctor_rating";

    public static Intent newIntent(Context packageContext, String path) {
        Intent intent = new Intent(packageContext, PhotoPagerActivity.class);
        intent.putExtra(EXTRA_PHOTO_PATH, path);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_pager);

        mPhotoPaths.add("blank");
        path = getIntent().getStringExtra(EXTRA_PHOTO_PATH);
        mPhotoPaths.add(path);
        mPhotoPaths.add("blank");
        System.out.println("Photo path in PhotoPager: " + path);

        intent = new Intent(PhotoPagerActivity.this, MainActivity.class);
        intent.putExtra(VECTOR_PATH, path);

        mViewPager = (ViewPager) findViewById(R.id.photo_view_pager);

        FragmentManager fragmentManager = getSupportFragmentManager();

        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                String path = mPhotoPaths.get(position);
                return PhotoItemFragment.newInstance(path);
            }

            @Override
            public int getCount() {
                return mPhotoPaths.size(); }
        });
        int currentPosition = 1;
        mViewPager.setCurrentItem(currentPosition);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 2) { // left swipe, TODO: doesn't work with right swipe
                    Toast.makeText(PhotoPagerActivity.this, "You rated the image as negative", Toast.LENGTH_SHORT).show();

                    NegativeImageLab.get(getApplicationContext()).addNegativeImage(path);


                    intent.putExtra(VECTOR_RATING, -1);

                    setResult(Activity.RESULT_OK, intent);
                    System.out.println("Neg Activity will be finished");
                    finish();
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) { // right swipe
                    System.out.println("Position is 0? " + position);
                    Toast.makeText(PhotoPagerActivity.this, "You rated the image as positive", Toast.LENGTH_SHORT).show();

                    PositiveImageLab.get(getApplicationContext()).addPositiveImage(path);

//                    Intent intent = new Intent(PhotoPagerActivity.this, MainActivity.class);
//                    intent.putExtra(VECTOR_PATH, path);
                    intent.putExtra(VECTOR_RATING, 1);

                    setResult(Activity.RESULT_OK, intent);
                    System.out.println("Pos Activity will be finished");
                    finish();
                }
//                else if (position == 2) { // left swipe
//                    Toast.makeText(PhotoPagerActivity.this, "You rated the image as negative", Toast.LENGTH_SHORT).show();
//
//                    NegativeImageLab.get(getApplicationContext()).addNegativeImage(path);
//
//                    Intent intent = new Intent(PhotoPagerActivity.this, MainActivity.class);
//                    intent.putExtra(VECTOR_PATH, path);
//                    intent.putExtra(VECTOR_RATING, -1);
//
//                    setResult(Activity.RESULT_OK, intent);
//                    System.out.println("Neg Activity will be finished");
//                    finish();
//                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    public static String getVectorPath(Intent intent) {
        return intent.getStringExtra(VECTOR_PATH);
    }

    // LOOK
    public static int getVectorRating(Intent intent) {
        return intent.getIntExtra(VECTOR_RATING, 0);
    }
}

