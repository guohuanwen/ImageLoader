package com.mylibrary;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by bigwen on 2016/4/20.
 */
public class ImageLoader {

    private DiskLruCache mDiskLruCache;
    private LruCache<String, Bitmap> mLruCache;
    private Context mContext;
    private static final int DISKCACHESIZE = 10 * 1024 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private static final int BUFFER_SIZE = 8 * 1024;
    private BwThreadPool bwThreadPool;
    private String TAG = ImageLoader.class.getName();
    private Handler handler;
    private static volatile ImageLoader imageLoader;

    public static ImageLoader getInstance(Context context) {
        if (imageLoader == null) {
            synchronized (ImageLoader.class) {
                if (imageLoader == null) {
                    imageLoader = new ImageLoader(context);
                }
            }
        }
        return imageLoader;
    }

    private ImageLoader(Context context) {
        mContext = context;
        handler = new Handler(Looper.getMainLooper());
        bwThreadPool = BwThreadPool.getInstance();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
        mLruCache = new LruCache<>(maxMemory);

        try {
            File diskFile = getDiskFile(context, "image1");
            if (!diskFile.exists()) {
                diskFile.mkdirs();
            }
            mDiskLruCache = DiskLruCache.open(diskFile, getVersionCode(context), 1, DISKCACHESIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Bitmap load(String ul) {
        Log.i(TAG, "load: loadFromMemoryCache");
        Bitmap bitmap = null;
        bitmap = loadFromMemoryCache(ul);
        if (bitmap == null) {
            Log.i(TAG, "load: loadFromDiskCache");
            bitmap = loadFromDiskCache(ul);
            if (bitmap == null) {
                Log.i(TAG, "load: loadFromDiskCache = null");
            }
            if (bitmap == null) {
                Log.i(TAG, "load: Net");
                if (loadFromNet(ul)) {
                    return loadFromDiskCache(ul);
                } else {
                    return null;
                }
            } else {
                Log.i(TAG, "load: Disk");
                return bitmap;
            }
        } else {
            Log.i(TAG, "load: Memory");
            return bitmap;
        }
    }

    public void loadAsyn(final String url, final BwCallback bwCallback) {
        Log.i(TAG, "loadAsyn: ");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: Runnable");
                final Bitmap bitmap = load(url);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: handler");
                        bwCallback.onFinished(bitmap);
                    }
                });
            }
        };
        bwThreadPool.add(runnable);
    }

    /**
     * 从网络加载
     *
     * @param url
     * @return
     */
    private boolean loadFromNet(String url) {
        boolean r = loadNetAndSaveToDisk(url);
        Log.i(TAG, "loadFromNet: " + r);
        return r;
    }

    /**
     * 保存到磁盘
     *
     * @param url
     * @param bitmap
     */
    private void saveToDisk(String url, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        String hashCode = getHashKeyFromUrl(url);
        DiskLruCache.Editor editor = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashCode);
            //已存在则不保存
            if (snapshot != null) {
                return;
            }
            editor = mDiskLruCache.edit(hashCode);
            if (editor != null) {
                outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
                bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER_SIZE);
                int b;
                while ((b = inputStream.read()) != -1) {
                    bufferedOutputStream.write(b);
                }
                editor.commit();
                mDiskLruCache.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(outputStream);
            IOUtil.close(inputStream);
            IOUtil.close(bufferedOutputStream);
        }
    }

    /**
     * 保存到磁盘
     */
    private boolean loadNetAndSaveToDisk(String url) {
        boolean r = false;
        String hashCode = getHashKeyFromUrl(url);
        DiskLruCache.Editor editor = null;
        OutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        URL u = null;
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashCode);
            //已存在则不保存
            if (snapshot != null) {
                Log.i(TAG, "loadNetAndSaveToDisk: snapshot");
                return false;
            }
            editor = mDiskLruCache.edit(hashCode);
            if (editor != null) {
                outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
                Log.i(TAG, "loadNetAndSaveToDisk: editor");
                u = new URL(url);
                httpURLConnection = (HttpURLConnection) u.openConnection();
                inputStream = httpURLConnection.getInputStream();
                if (inputStream != null) {
                    bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
                    Log.i(TAG, "loadNetAndSaveToDisk: inputStream");
                    bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER_SIZE);
                    int b;
                    while ((b = bufferedInputStream.read()) != -1) {
                        bufferedOutputStream.write(b);
                    }
                    bufferedOutputStream.flush();
                    editor.commit();
                    r = true;
                } else {
                    editor.abort();
                }
                mDiskLruCache.flush();
            }

        } catch (IOException e) {
            try {
                mDiskLruCache.remove(hashCode);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null)
            httpURLConnection.disconnect();
            IOUtil.close(bufferedOutputStream);
            IOUtil.close(bufferedInputStream);
        }
        return r;
    }


    /**
     * 保存到内存
     *
     * @param url
     * @param bitmap
     */
    private void saveToMemoryCache(String url, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        String hashCode = getHashKeyFromUrl(url);
        if (mLruCache.get(hashCode) == null) {
            mLruCache.put(hashCode, bitmap);
        }
    }

    /**
     * 从内存读取，可能为空
     *
     * @param url
     * @return
     */
    private Bitmap loadFromMemoryCache(String url) {
        String key = getHashKeyFromUrl(url);
        return mLruCache.get(key);
    }

    /**
     * 从磁盘读取，可能为空
     *
     * @param url
     * @return
     */
    private Bitmap loadFromDiskCache(String url) {
        Log.i(TAG, "loadFromDiskCache: ");
        String key = getHashKeyFromUrl(url);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                Log.i(TAG, "loadFromDiskCache: ");
                fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                fileDescriptor = fileInputStream.getFD();
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                    Log.i(TAG, "loadFromDiskCache: fileDescriptor = " + fileDescriptor.toString());
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                }
                if (bitmap == null) {
                    Log.i(TAG, "loadFromDiskCache: bitmap=null");
                } else {
                    saveToMemoryCache(url, bitmap);
                }
                return bitmap;
            }
        } catch (IOException e) {
            Log.e(TAG, "loadFromDiskCache: ", e);
        } finally {
            IOUtil.close(fileInputStream);
        }
        return null;
    }

    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * getCacheDir()方法用于获取/data/data/<application package>/cache目录
     * getFilesDir()方法用于获取/data/data/<application package>/files目录
     * Context.getExternalFilesDir()方法可以获取到 SDCard/Android/data/你的应用的包名/files/ 目录，一般放一些长时间保存的数据
     * Context.getExternalCacheDir()方法可以获取到 SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
     *
     * @param context
     * @param name
     * @return
     */
    private File getDiskFile(Context context, String name) {
        boolean external = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String path;
        if (external) {
            path = context.getExternalCacheDir().getPath();
        } else {
            path = context.getCacheDir().getPath();
        }
        return new File(path + File.separator + name);
    }

    private int getVersionCode(Context context) {
        int version = 1;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    private String getHashKeyFromUrl(String url) {
        String cacheKey = "";
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            cacheKey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private Bitmap decodeBitmap(FileDescriptor fileDescriptor, int w, int h) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inSampleSize = 1;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }
}
