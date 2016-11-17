package edu.orangecoastcollege.cs273.gabyers.photogallery;


import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class PhotoGalleryActivity extends SingleFragmentActivity {

public Fragment createFragment(){
     return PhotoGalleryFragment.newInstance();
 }
}
