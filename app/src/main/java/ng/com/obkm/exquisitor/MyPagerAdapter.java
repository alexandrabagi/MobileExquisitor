package ng.com.obkm.exquisitor;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

// From: https://stackoverflow.com/questions/41326209/viewpager-in-dialog
public class MyPagerAdapter extends PagerAdapter {

    private Context mContext;
    private int resId = 0;
    private Dialog mDialog;

    public MyPagerAdapter(Context context, Dialog dialog) {

        mContext = context;
        mDialog = dialog;
    }


    public Object instantiateItem(ViewGroup collection, int position) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.intro_dialog_start, collection, false);
        switch (position) {
            case 0:
                resId = R.layout.intro_dialog_start;
                view = inflater.inflate(resId, collection, false);
                break;
            case 1:
                resId = R.layout.intro_dialog_rating;
                view = inflater.inflate(resId, collection, false);
                break;
            case 2:
                resId = R.layout.intro_dialog_bottom_bar;
                view = inflater.inflate(resId, collection, false);
                break;
            case 3:
                resId = R.layout.intro_dialog_reload;
                view = inflater.inflate(resId, collection, false);
                break;
            case 4:
                resId = R.layout.intro_dialog_fast_foward;
                view = inflater.inflate(resId, collection, false);
                break;
            case 5:
                resId = R.layout.intro_dialog_start_over;
                view = inflater.inflate(resId, collection, false);
                break;
            case 6:
                resId = R.layout.intro_dialog_help;
                view = inflater.inflate(resId, collection, false);
                Button closeButton = view.findViewById(R.id.close_btn);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });
                break;
        }
//        ViewGroup layout = (ViewGroup) inflater.inflate(resId, collection, false);
        collection.addView(view);

        return view;
    }

    @Override
    public int getCount() {
        return 7;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        System.out.println("arg0 == arg1: " + (arg0 == arg1));
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup view, int number, Object object) {
    }
}
