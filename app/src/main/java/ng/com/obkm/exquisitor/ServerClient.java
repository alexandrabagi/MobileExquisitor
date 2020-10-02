package ng.com.obkm.exquisitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ng.com.obkm.exquisitor.database.VectorBaseHelper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ServerClient {

    private static final int BUFFER = 65536; // 524288
    private final int SIZEOFBATCH = 100;
//    private static final String posturl = "http://192.168.1.200:5000";
    private static final String posturl = "http://192.168.1.202:5000";
    String TAG = "Server";
    private int id = 0;
    int imagesinDBBeforeStart = 0;



    //https://heartbeat.fritz.ai/uploading-images-from-android-to-a-python-based-flask-server-691e4092a95e
    //https://github.com/ahmedfgad/AndroidFlask/blob/master/Part%201/FlaskServer/flask_server.py
    public void connectServer(final Context c, final List<String> paths) {
        Log.i(TAG, "connect to server was called.");
        List<JSONObject> jsonObjectsList = new ArrayList<>();
        // add method to retrieve highest index

        Thread thread = new Thread(new Runnable() {
            public void run() {

                try {
                    String[] imagesToZipArray = new String[SIZEOFBATCH];
                    int numberOfImages = paths.size();
                    int iterations = numberOfImages / SIZEOFBATCH;
                    int startSecondIf = iterations * SIZEOFBATCH;
                    int rest = numberOfImages % SIZEOFBATCH;

                    for (int i = 0; i < numberOfImages; i += SIZEOFBATCH) {
                        if (iterations > 0) {
                            for (int j = 0; j < SIZEOFBATCH; j++) {
                                imagesToZipArray[j] = paths.get(i + j);
                            }
                            File oneZipped = zip(imagesToZipArray, ("zipped_" + i));

                            //create request body
                            //here replace "file" with your parameter name
                            //Change media type according to your file type
                            RequestBody fileRequestBody = new MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    //.addFormDataPart("numberOfImages", String.valueOf(numberOfImages))
                                    .addFormDataPart("file", ("zipped_" + i),
                                            RequestBody.create(MediaType.parse("application/zip"), oneZipped))
                                    .build();
                            Log.i(TAG, "requestbody " + fileRequestBody);
                            postRequest(c, fileRequestBody);
                            iterations--;
                            Log.i(TAG, "one batch posted.");

                        }
                    }
                    if (rest > 0 || numberOfImages < SIZEOFBATCH) {
                        int y = 0;
                        String[] restImagesArray;

                        if(rest == 0 ) restImagesArray = new String[numberOfImages];
                            else {restImagesArray = new String[rest]; }
                        Log.i(TAG, "paths size " + paths.size());

                        for (int z = startSecondIf; z < numberOfImages; z++) {
                            Log.i(TAG, "we are in loop");
                            Log.i(TAG, "path to add " + paths.get(z));
                            restImagesArray[y] = paths.get(z);
                            y++;
                        }
                        File oneZipped = zip(restImagesArray, ("zipped_" + "rest"));

                        //create request body
                        //here replace "file" with your parameter name
                        //Change media type according to your file type
                        RequestBody fileRequestBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                //.addFormDataPart("numberOfImages", String.valueOf(numberOfImages))
                                .addFormDataPart("file", ("zipped_" + "rest"),
                                        RequestBody.create(MediaType.parse("application/zip"), oneZipped))
                                .build();
                        postRequest(c, fileRequestBody);
                        Log.i(TAG, "last batch posted.");
                    }

                    VectorBaseHelper.exportDB();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }


    public File zip(String[] _files, String zipFileName) {
        String internalPath = "/data/data/" + MainActivity.getPACKAGE_NAME();
        File zippedFile = new File(internalPath, zipFileName);

        try {
            //boolean fileCreation = zippedFile.createNewFile();
            Log.i(TAG, "One batch is being zipped.");

            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zippedFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte data[] = new byte[BUFFER];

            for (int idx = 0; idx < _files.length; idx++) {
                FileInputStream fi = new FileInputStream(_files[idx]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(_files[idx].substring(_files[idx].lastIndexOf("/") + 1));
                //ZipEntry entry = new ZipEntry(_files[idx].substring(_files[idx].lastIndexOf("DCIM/")));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "zip successful");
        return zippedFile;
    }

    void postRequest(Context c, RequestBody fileRequestBody) throws IOException {
        Log.i(TAG, "postRequest was called.");

        //to debug http posts
        //HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        //logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        final OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(360, TimeUnit.SECONDS)
                .connectTimeout(360, TimeUnit.SECONDS)
                //.addInterceptor(logging)
                .build();
        //create the request
        Request fileRequest = new Request.Builder().url(posturl)
                .post(fileRequestBody)
                .build();
        //send the request
        getResponse(client, fileRequest, id, c);
        id++;
    }

    private void getResponse(OkHttpClient client, Request fileRequest, final int id, Context c) {
        //SYNCHRONOUS
        try
        {
            VectorLab vl = VectorLab.get(MainActivity.getContext());

            Response response = client.newCall(fileRequest).execute();
            String responseText = response.body().string();
            JSONObject Jobject = new JSONObject(responseText);
            JSONArray vector_collection = Jobject.getJSONArray("vector_collection");

            for (int i = 0; i < vector_collection.length(); i++) {
                JSONObject obj = vector_collection.getJSONObject(i);
                String path = obj.getString("path");
//                int imageID = (id*100) + i;
                int imageID = vl.getLastImageID() + 1;
                vl.addIDPathPairs(imageID, path);
                String labels = obj.getString("labels");
                String probabilities = obj.getString("probabilities");
                int[] labelsIntArray = getIntLabels(labels);
                float[] probsFloatArray = getFloatProbs(probabilities);
                for (int j = 0; j < HomeFragment.NUMBEROFHIGHESTPROBS; j++) {
                   vl.addLabelProbToVectors(imageID, labelsIntArray[j], probsFloatArray[j]);
//                    Log.i(TAG, "Vector " + j + " added to DB");
                }
            }
            SharedPreferenceUtil.writeToSharedPreferences(c);

        }
        catch(IOException | JSONException e)
        {
            Log.i(TAG, "postRequest exception: " + e);
        }

    }


    private int[] getIntLabels(String labels) {
        String[] labelsS = labels.replace("[", "").replace("]", "").split(",");
        //Log.i(TAG, "Split labels: " + Arrays.toString(labelsS));
        int[] labelsIntArray = new int[labelsS.length];
        for (int i = 0; i < labelsS.length; i++) labelsIntArray[i] = Integer.parseInt(labelsS[i]);
        return labelsIntArray;

    }

    private float[] getFloatProbs(String probs) {
        String[] probsS = probs.replace("[", "").replace("]", "").split(",");
        float[] probsFloatArray = new float[probsS.length];
        for (int i = 0; i < probsS.length; i++) probsFloatArray[i] = Float.parseFloat(probsS[i]);
        return probsFloatArray;
    }

}