package edu.orangecoastcollege.cs273.gabyers.photogallery;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;




/**
 * Fragment controller that starts a background task to download user photos
 * @author Grant Byers
 */

public class PhotoGalleryFragment extends Fragment {


    private static String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> arrayList = new ArrayList<>();

    //A handlerThread extended class instance variable
    ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //attach fragment to activity
        setRetainInstance(true);
        //allow for Menu callbackcs
        setHasOptionsMenu(true);

        updateItems();
//        new FetchItems().execute();

        Handler responsehandler = new Handler();

        //initiaize Thread class (class extending HandlerThread) with a Handler (for responses)
        mThumbnailDownloader = new ThumbnailDownloader<>(responsehandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);

                //this is the front end of the app using the PhotoHolder object's method (bindDrawable) to attach the actual image
                target.bindDrawable(drawable);
            }
        });

        mThumbnailDownloader.start();
        //need looper for background thread  (.getLooper)
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop the background thread (.quit)
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed with .quit()");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        //reference for MenuItem (SearchView needs a menu item, think of it as digging in to provided Menu
        // to get a SearchView reference)
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setupAdapter(){
        if(isAdded())
            mPhotoRecyclerView.setAdapter( new PhotoAdapter(arrayList));
    }
//Class Creates the PhotoAdapter (essentially this is what is being seen)
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItemList;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }

    //inflate layout (return view)
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {

            GalleryItem gItem = mGalleryItemList.get(position);

//            Drawable placeHolder = getResources().getDrawable(R.drawable.battlefield);
//            holder.bindDrawable(placeHolder);

            //method queueThumbnail(T target, String url) adds the parameters to Thread-ready hashMap(Target, Url)
            //it uses Thumdnlder.. instance variable requesthandler to .obtainMessage() and .sendToTarget
            //remeber Target is only the container and yes the url is the oic but this will now have to be
            mThumbnailDownloader.queueThumbnail(holder, gItem.getUrl());

        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mImage;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImage = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImage.setImageDrawable(drawable);
        }
    }


    //    ArrayList<String> permList = new ArrayList<>();
//
//    //start by seeing if we have permisson to camera
//    int camPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//    if(camPerm != PackageManager.PERMISSION_GRANTED)
//    {
//        permList.add(Manifest.permission.CAMERA);
//    }
//    int readPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//    if(readPerm != PackageManager.PERMISSION_GRANTED)
//            permList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
//    int writePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//    if(writePerm != PackageManager.PERMISSION_GRANTED)
//            permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//    if(permList.size() > 0)
//    {
//        //method accepts only String
//        //convert arrayLList to array of String
//        String[] perms = new String[permList.size()];
//
//        ActivityCompat.requestPermissions(this, permList.toArray(perms),100);
//
//    }
//    //if we have all thre permissions. Open Image gallery
//    if(camPerm == PackageManager.PERMISSION_GRANTED&& writePerm == PackageManager.PERMISSION_GRANTED && readPerm == PackageManager.PERMISSION_GRANTED)
//    {
//        //use an intent to launch gallery and take pics
//        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(galleryIntent, CONSTANT);
//    }else
//            Toast.makeText(this, "set Permisions",Toast.LENGTH_LONG);
    //class for Background task because android doesnt allow internet task on main thread
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        //implement interface method
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
//            try{
//                String result = new FlickrFetcher()
//                        .getUrlString("https://www.google.com");
//                Log.i(TAG, "Fetched contents of URL: " + result);
//            }catch(IOException e){
//                Log.e(TAG, "Failed to fetch URL: ", e);
//            }return null;


            //return new FlickrFetcher().fetchItems();


            if(mQuery == null) return new FlickrFetcher().fetchRecentPhotos();
            else
                return new FlickrFetcher().searchPhotos(mQuery);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            arrayList =  items;
            setupAdapter();
        }
    }

}
