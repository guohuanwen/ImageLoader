package com.mylibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bigwen on 2016/4/20.
 */
public class BwHttpGet {

    private static final int BUFFER_SIZE = 8*1024;
    private static final String TAG = BwHttpGet.class.getName();

    public static Bitmap get(String url){
        Log.i(TAG, "get: ");
        URL u = null;
        HttpURLConnection httpURLConnection = null;
        BufferedInputStream bufferedInputStream = null;
        Bitmap b = null;
        try{
            u = new URL(url);
            httpURLConnection = (HttpURLConnection) u.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(),BUFFER_SIZE);
            b = BitmapFactory.decodeStream(bufferedInputStream);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
            IOUtil.close(bufferedInputStream);
        }
        return b;
    }

    public static InputStream getInputStream(String url){
        Log.i(TAG, "get: ");
        URL u = null;
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try{
            u = new URL(url);
            httpURLConnection = (HttpURLConnection) u.openConnection();
            return httpURLConnection.getInputStream();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
            IOUtil.close(inputStream);
        }
        return inputStream;
    }


}
