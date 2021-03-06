package edu.orangecoastcollege.cs273.gabyers.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by gabye on 11/15/2016.
 */

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownLoader";

    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private Handler mResponseHandler;

    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    //When method is called in PhotoGallFrag it will be called using a Handler
    // (this constructor allows that to be attached to this ones instance variable)
    public ThumbnailDownloader(Handler responseHandler)
    {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for a url: " + mRequestMap.get(target));

                    handleRequest(target);
                }}
        };

    }
//this method is what actually gets the image from HashMap
    private void handleRequest(final T target){
        try{
            final String url = mRequestMap.get(target);
            if(url == null) return;

            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0,bitmapBytes.length);

            Log.i(TAG, "Bitmap Created!!");

            mResponseHandler.post(new Runnable(){
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url) return;

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

        }catch(IOException e){
            Log.e(TAG, "Error downloading image", e);
        }
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public void clearQueue(){mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);}

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a url:" + url);

        if(url == null)
            mRequestMap.remove(target);
        else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }
}