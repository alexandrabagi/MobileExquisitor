package ng.com.obkm.exquisitor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.File;

public class  PhotoItem {

    private String imagePath;




    public PhotoItem(String path){
        this.imagePath = path;
    }

    public String getImagePath(){
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Bitmap getBitmap() {
        File imgFile = new  File(imagePath);

        if(imgFile.exists()){
            Bitmap imageBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            return imageBitmap;
        } else {
            // TODO: handle if Bitmap can't be returned
            return null;
        }
    }
}
