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

import java.util.List;

import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

public class GalleryAdapter extends BaseAdapter {
	final private static String TAG = "GalleryAdapter";

	private List<ParseObject> images;
	
	public GalleryAdapter(List<ParseObject> images) {
		this.images = images;
	}

	@Override
	public int getCount() {
		return images.size();
	}

	@Override
	public Object getItem(int position) {
		return images.get(position);
	}

	@Override
	public long getItemId(int position) {
		return images.get(position).getObjectId().hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ImageView iv;
		if (convertView != null) {
			iv = (ImageView) convertView;
		} else {
			iv = new ImageView(parent.getContext());
			iv.setScaleType(ImageView.ScaleType.FIT_XY);
		}

		// Set to temporary image...
		iv.setImageResource(R.drawable.ic_launcher);

		// ... then download file in background
		final ParseFile imageFile = (ParseFile) images.get(position).get("imageFile");
		Log.d(TAG, "Downloading " + imageFile.getUrl());
		imageFile.getDataInBackground(new GetDataCallback() {
			@Override
			public void done(byte[] data, ParseException e) {
				if (e != null) {
					Log.d(TAG, "Error downloading " + imageFile.getUrl());
					e.printStackTrace();
					return;
				}
				Log.d(TAG, "Download successful: " + imageFile.getUrl());
				iv.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
			}
		});

		return iv;
	}

}
