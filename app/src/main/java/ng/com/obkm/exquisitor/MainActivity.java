package ng.com.obkm.exquisitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.facebook.stetho.Stetho;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ng.com.obkm.exquisitor.database.VectorBaseHelper;

public class MainActivity extends AppCompatActivity {

    public static String PACKAGE_NAME;
    private static Context mContext;

    private static final String TAG = "main";
    final FragmentManager fm = getSupportFragmentManager();
    Fragment fragment1;
    Fragment fragment2;
    Fragment fragment3;
    Fragment active;
    MediaStoreCheck mObserver;
    // PERMISSIONS
    private static final int REQUEST_CODE = 404;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("tflite", "Main Acitivty onCreate called");
        setContentView(R.layout.activity_main);

        PACKAGE_NAME = getApplicationContext().getPackageName();
        mContext = getApplicationContext();
        Stetho.initializeWithDefaults(this);
        if (savedInstanceState == null) {

            tryToOpenFragments();
        }
//        SharedPreferenceUtil.writeToSharedPreferences(mContext);

    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            switch (item.getItemId()) {
                case R.id.navigation_negatives:
                    ft.detach(fragment1);
                    ft.attach(fragment1);
                    ft.commit();
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    active = fragment1;
                    return true;

                case R.id.navigation_home:
                    fm.beginTransaction().hide(active).show(fragment2).commit();
                    active = fragment2;
                    return true;

                case R.id.navigation_positives:
                    ft.detach(fragment3);
                    ft.attach(fragment3);
                    ft.commit();
                    //fm.beginTransaction().detach(fragment3).commit();
                    fm.beginTransaction().hide(active).show(fragment3).commit();
                    active = fragment3;
                    return true;
            }
            return false;
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // inspired by https://stackoverflow.com/questions/50067149/start-a-fragment-from-upon-getting-permission
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("tflite", "onRequestPermission was claled");
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                /**
                 * case 1 - we open the app for the first time, no file in internal storage, no file in external storage
                 * connect to server
                 * case 2 - we reinstalled the app, no file in internal storage, DB file in external storage
                 * importDB
                 */

                // check if we have a backup in the external storage
                // external DB file
                File backUpDir = new File(Environment.getExternalStorageDirectory(), "DBBackup2");
                Log.i(TAG, "backupdir " + backUpDir);
                File backupDB2 = new File(backUpDir, "vectorDB.db");
                Log.i(TAG, "backup file name " + backupDB2);
                Log.i(TAG, "does backup exist? " + backupDB2.exists());
                if (backupDB2.exists()) {
                    Log.i(TAG, "BackupDB exists, copying it from external storage");
                    // Creating internal storage databases folder
                    String internalPath = "";
                    if(android.os.Build.VERSION.SDK_INT >= 17) {
                        internalPath = this.getApplicationInfo().dataDir + "/databases/";
                        Log.i(TAG, "this is internalPath " + internalPath);
                        File internalDir = new File(internalPath);
                        Log.i(TAG, "this is internal dir " + internalDir);
                        internalDir.mkdir();
                    } else {
                        internalPath = "/data/data/" + this.getPackageName() + "/databases/";
                        Log.i(TAG, "this is internalPath " + internalPath);

                    }
                    VectorBaseHelper.importDB(backupDB2, internalPath, this);
                } else {

                    Log.i(TAG, "BackupDB doesn't exist, calling classify images");
                    ServerClient serverClient = new ServerClient();
                    serverClient.connectServer(mContext, getAllImages());
                }

                showFragments();
            }
            else {
                // TODO: Permission was not granted
            }
        }
    }

    //used to send images to server
    //protected String[] getAllImages() {
    protected List<String> getAllImages() {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media._ID);

        final int numberOfPicsOnPhone = cursor.getCount();
        int image_path_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

        //String[] paths = new String[numberOfPicsOnPhone];
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < numberOfPicsOnPhone; i++) {
            cursor.moveToNext();
            final String imagePath = cursor.getString(image_path_index);
            Log.i(TAG, "one path " + imagePath);

            paths.add(imagePath);
        }
        cursor.close();
        return paths;
    }

    private void tryToOpenFragments() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            showFragments();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    private void showFragments() {
        fragment1 = new NegativeListFragment();
        fragment2 = new HomeFragment();
        fragment3 = new PositiveListFragment();
        active = fragment2;

        fm.beginTransaction().add(R.id.main_container, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.main_container, fragment1, "1").hide(fragment1).commit();
        fm.beginTransaction().add(R.id.main_container, fragment2, "2").commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop is called");
//        updateBackupDB();

    }

    @Override
    protected void onDestroy() {
        updateBackupDB();
        super.onDestroy();
        Log.d(TAG, "MainActivity is destroyed");
        // update db in external storage

    }

    private void updateBackupDB() {
        System.out.println("updateBackupDB was called");
        File backUpDir = new File(Environment.getExternalStorageDirectory(), "DBBackup2");
        Log.i(TAG, "backupdir " + backUpDir);
        File backupDB2 = new File(backUpDir, "vectorDB.db");
        Log.i(TAG, "backup file name " + backupDB2);
        Log.i(TAG, "does backup exist? " + backupDB2.exists());
        if (backupDB2.exists()) {
            Log.i(TAG, "BackupDB exists, copying it from external storage");
            // Creating internal storage databases folder
            String internalPath = "";
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                internalPath = this.getApplicationInfo().dataDir + "/databases/";
                Log.i(TAG, "this is internalPath " + internalPath);
                File internalDir = new File(internalPath);
                Log.i(TAG, "this is internal dir " + internalDir);
            } else {
                internalPath = "/data/data/" + this.getPackageName() + "/databases/";
                Log.i(TAG, "this is internalPath " + internalPath);

            }
            VectorBaseHelper.exportDB();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult overridden");
//        Log.i("home", "MainActivity: is data retured from on activity result" + (data == null));
    }

    public static String getPACKAGE_NAME() {
        return PACKAGE_NAME;
    }

    public static Context getContext() {
        return mContext;
    }

    private void getDCIMUpdate(){
    }
}
