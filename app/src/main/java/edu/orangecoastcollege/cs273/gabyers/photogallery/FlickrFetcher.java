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
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";

    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();



//Method used by PhotoGalleryFragment's Private inner class to append query from flicker with special method (flikr.photos.getRecent)
//method includes helper methods getUrlString and getUrlBytes
    public List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> galleryItemList = new ArrayList<>();
        try{
            //getUrlString calls getUrlBytes (which returns a byte array, to which can be used by String). getUrlBytes
            //opens a connection with an inputStream and writes to an ByteArrayOutoutStream(all the data received (inBytes))
            String jsonString = getUrlString(url);

//            Log.i(TAG, "Received JsOn: "+ jsonString);

            //Using the String that has all the byte data, create a new JSONObject
            JSONObject jsonBody = new JSONObject(jsonString);

            //use the JSONobj and the ArrayList to build a list of GalleryItems. GalleryItems will include 3 Strings
            // (a caption, id, and a url)
            parseItems(galleryItemList, jsonBody);

        }catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON data");
        }catch(IOException e){
            Log.e(TAG,"Failed to fetch items", e);
        }
        return galleryItemList;
    }

    /**
     * Helper method for making this class equipped to deal with both Flickr Rest Quert
     * @param method
     * @param query
     * @return
     */
    private String buildUri(String method, String query){
        //the Flickr REST query needs appending of necessary parameters
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        //check the method string to see if its the search one
        if(method.equals(SEARCH_METHOD))
            uriBuilder.appendQueryParameter("text", query);

        return uriBuilder.build().toString();
    }

//Method modifies a List of GalleryItems (it basically builds it), from the Parameter Provided JSONObj
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
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(){
        //use the buildUri helper for setting the recent (Rest, Flickr query)
        String url = buildUri(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUri(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }
    //method uses the received String to connect (via HTTPURLConnection  url.)
    public byte[] getUrlBytes(String urlSpec) throws IOException{
        //create a Type URL from the parameter string
        URL url = new URL(urlSpec);
        //with that created URL create a HTTPConnection obj
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            //connect inputStream to HttpURLConnection object pointer
            InputStream in = connection.getInputStream();

            //if response code from HTTPConnection is not equal to HTTP_OK throw exception
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+": with "+urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            //this is the method that will actually read the data from the web
            while((bytesRead = in.read(buffer))>0){
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            return outputStream.toByteArray();

        }finally{connection.disconnect();}
    }
}
