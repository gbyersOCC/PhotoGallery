package edu.orangecoastcollege.cs273.gabyers.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gabye on 11/13/2016.
 */

public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "ad428a6be198194ad651bda5e0f356e2";

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            //connect inputStream to HttpURLConnection object pointer
            InputStream in = connection.getInputStream();

             if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                 throw new IOException(connection.getResponseMessage()+": with "+urlSpec);
             }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while((bytesRead = in.read(buffer))>0){
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            return outputStream.toByteArray();

        }finally{connection.disconnect();}
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(){
        List<GalleryItem> galleryItemList = new ArrayList<>();
        try{
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method","flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JsOn: "+ jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);


            parseItems(galleryItemList, jsonBody);
        }catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON data");
        }catch(IOException e){
            Log.e(TAG,"Failed to fetch items", e);
        }
        return galleryItemList;
    }


public void parseItems(List<GalleryItem> items, JSONObject jsonBody)throws IOException, JSONException{
JSONObject jsonObj = jsonBody.getJSONObject("photos");
        JSONArray jsonArray = jsonObj.getJSONArray("photo");

        for(int i = 0; i< jsonArray.length(); i++){
            JSONObject JSONOBJ = jsonArray.getJSONObject(i);

            //create a new GalleryItem
            GalleryItem gItem = new GalleryItem();

            gItem.setId(JSONOBJ.getString("id"));
            gItem.setCaption(JSONOBJ.getString("title"));

            if(!JSONOBJ.has("url_s"))continue;

            gItem.setUrl(JSONOBJ.getString("url_s"));

            //now add that obj to list of Items
            items.add(gItem);
        }
    }
}
