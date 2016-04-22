package com.bcgtgjyb.imageloader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.mylibrary.BwCallback;
import com.mylibrary.ImageLoader;
import com.mylibrary.Images;

/**
 * Created by bigwen on 2016/4/21.
 */
public class MainActivity extends Activity {


    private String url = "http://img.my.csdn.net/uploads/201407/26/1406383299_1976.jpg";
    private ImageLoader imageLoader;
    private ListView mListView;
    private String TAG = MainActivity.class.getName();
    private ImgAdapter mImgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_imgloader_activity);

        imageLoader = ImageLoader.getInstance(this);
        mListView = (ListView) findViewById(R.id.listview);
        mImgAdapter = new ImgAdapter(this);
        mListView.setAdapter(mImgAdapter);
    }

    private class ImgAdapter extends BaseAdapter {

        private Context mContext;

        public ImgAdapter(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return Images.imageThumbUrls.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.itme_img, null);
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String url = Images.imageThumbUrls[position];
            imageLoader.loadAsyn(url, new BwCallback() {
                @Override
                public void onFinished(Bitmap bitmap) {
                    viewHolder.imageView.setImageBitmap(bitmap);
                }
            });
            return convertView;
        }

        private class ViewHolder {
            ImageView imageView;
        }
    }
}

