/*
The MIT License

Copyright (c) 2012 Dominik Sommer (dominik.sommer@bluebee.mobi)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package mobi.bluebee.imagebee.android.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Gallery;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	// Notice the Android key is different from the REST key
	private final String PARSE_ID = "INSERT_PARSE_APPLICATION_ID_HERE";
	private final String PARSE_KEY = "INSERT_PARSE_CLIENT_KEY_HERE";
	public final String TAG = "ImageBee";
	private final int PHOTO_TAKEN = 1; 

	private String albumName = "tutorial";
	private ParseObject album;
	private ParseQuery imageQuery;
	private Gallery gallery;
	private GalleryAdapter galleryAdapter;
	private File currentImage;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get album name passed by the bluebee app
		if (!getIntent().hasExtra("albumname") || (albumName = getIntent().getStringExtra("albumname")) == null) {
			Toast.makeText(getApplicationContext(), "No album passed from bluebee", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

        // Initialize the Parse API
		Parse.initialize(this, PARSE_ID, PARSE_KEY);

        setContentView(R.layout.activity_main);

    	// Get album
        Toast.makeText(getApplicationContext(), "Loading album...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Loading album " + albumName);
		ParseQuery query = new ParseQuery("Album");
		query.whereEqualTo("name", albumName);
		query.findInBackground(new FindCallback() {
		    public void done(List<ParseObject> albumList, ParseException e) {
		    	if (e == null) {
		    		if (albumList.isEmpty()) {
		    			// New album, create it
		    			Log.d(TAG, "Creating album " + albumName);
		    			album = new ParseObject("Album");
		    			album.put("name", albumName);
		    			album.saveInBackground();
		    			 // Do not try to load images from empty album
			    		Toast.makeText(getApplicationContext(), "New album created", Toast.LENGTH_SHORT).show();
		    			return;
		    		} else {
		    			Log.d(TAG, "Found album " + albumName);
		    			album = albumList.get(0);
		    		}
		    		// Prepare query & load images
		    		imageQuery = new ParseQuery("Image");
		    		imageQuery.whereEqualTo("album", album);
		    		imageQuery.orderByDescending("createdAt");
		            refreshAlbum();
		    	} else {
		    		Toast.makeText(getApplicationContext(), "Could not load album", Toast.LENGTH_SHORT).show();
		    		finish();
		    		return;
		    	}
		    }
		});

		gallery = (Gallery) findViewById(R.id.gallery);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case R.id.menu_refresh:
        	refreshAlbum();
            return true;
        case R.id.menu_takephoto:
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
            	// Use the system's camera app to acquire a photo that is being stored to the specified file
            	currentImage = createImageFile();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(currentImage));
	            startActivityForResult(takePictureIntent, PHOTO_TAKEN);
			} catch (IOException e) {
	        	Toast.makeText(getApplicationContext(), "Unable to prepare image file", Toast.LENGTH_SHORT).show();
	        	Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}
            return true;
        default:
            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case PHOTO_TAKEN:
			if (resultCode == 0) {
				// No image taken - just refresh
				refreshAlbum();
				return;
			}
	    	if (currentImage == null) {
				Toast.makeText(getApplicationContext(), "No image returned from photo app :-(", Toast.LENGTH_LONG).show();
	    		return;
	    	}
			// Upload the image
	    	byte[] imageData;
			try {
		    	currentImage = resizeImage(currentImage);
				imageData = new byte[(int) currentImage.length()];
				new FileInputStream(currentImage).read(imageData);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Image upload failed :-(", Toast.LENGTH_LONG).show();
				return;
			}
			Toast.makeText(getApplicationContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
			final ParseFile imageFile = new ParseFile(currentImage.getName(), imageData);
			imageFile.saveInBackground(new SaveCallback() {
				@Override
				public void done(ParseException e) {
					if (e != null) {
						e.printStackTrace();
						Toast.makeText(getApplicationContext(), "Image upload failed :-(", Toast.LENGTH_LONG).show();
						return;
					}
					// Image file uploaded, now create the Image object and link it to the album
					final ParseObject image = new ParseObject("Image");
					image.put("album", album);
					image.put("imageFile", imageFile);
					image.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								// Upload complete, now refresh album
								refreshAlbum();
								Log.d(TAG, "Image uploaded to parse, objectid: " + image.getObjectId());
								Toast.makeText(getApplicationContext(), "Image uploaded!", Toast.LENGTH_SHORT).show();
							} else {
								e.printStackTrace();
								Toast.makeText(getApplicationContext(), "Image upload failed :-(", Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			});
		}
	}

	/**
	 * Refresh the album's images
	 */
    protected void refreshAlbum() {
    	Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
		// Fetch images from album
    	imageQuery.cancel();
		imageQuery.findInBackground(new FindCallback() {
		    public void done(List<ParseObject> imageList, ParseException e) {
		    	if (e == null) {
		    		if (imageList.isEmpty()) return;

		    		// Update Gallery
		    		Log.d(TAG, "Image list loaded, now refreshing images");
		    		galleryAdapter = new GalleryAdapter(imageList);
		    		gallery.setAdapter(galleryAdapter);
		    	} else {
		            Log.e(TAG, "Error fetching image list: " + e.getMessage());
		    	}
	    		Log.d(TAG, "Refresh complete");
		    }
		});
    }

    // taken from http://developer.android.com/training/camera/photobasics.html#TaskPath
    private File createImageFile() throws IOException {
        String timeStamp = 
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "ImageBee" + timeStamp + "_";
        File image = File.createTempFile(
            imageFileName,
            ".JPG"
        );
        return image;
    }

    // taken from http://developer.android.com/training/camera/photobasics.html#TaskScalePhoto
    // with minor modifications
    private File resizeImage(File originalImage) {
        // Target dimensions
        int targetW = 240;
        int targetH = 320;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(originalImage.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a resized Bitmap
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        // Save the resized image, using the resized image as currentImage
        Bitmap bmp = BitmapFactory.decodeFile(originalImage.getAbsolutePath(), bmOptions);
        File image = null;
        try {
            image = createImageFile();
	        FileOutputStream out = new FileOutputStream(image);
	        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
	        out.close();
        } catch (Exception e) {
        	Log.e(TAG, e.getMessage());
        	image = null;
        }
        return image;
    }
}
