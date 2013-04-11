package com.kac.customcomponents;

import java.util.Arrays;
import java.util.List;

import com.dataart.customcomponents.R;
import com.kac.customcomponents.TrimControl.OnTrimControlChangeListener;
import com.kac.customcomponents.utils.ImageUtils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "TrimControl";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final TextView logText = (TextView) findViewById(R.id.logValue);

        // This is a preferred way of loading large images. Please, see ImageUtils for explanation.
        Bitmap mBmap0 = ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.photo1, 100, 100);
        Bitmap mBmap1 = ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.photo2, 100, 100);
        Bitmap mBmap2 = ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.photo3, 100, 100);
        Bitmap mBmap3 = ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.photo4, 100, 100);
        Bitmap mBmap4 = ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.photo5, 100, 100);
        
        // push items to the list to simulate fetching of data.
        List<Bitmap> listOfBitmaps = Arrays.asList(mBmap0, mBmap1, mBmap2, mBmap3, mBmap4);
		        
        final ObjectParameters mObjectParameters = new ObjectParameters();
        // here 0L is the start value and 200L is length of the video.
		TrimControl<Long> mTrimControl = new TrimControl<Long>(getApplicationContext(), 0L, 200L);
		mTrimControl.setOnTrimControlChangeListener(new OnTrimControlChangeListener<Long>() {
		        @Override
		        public void onTrimControlValuesChanged(TrimControl<?> bar, Long minValue, Long maxValue) {
		            Log.d(TAG, String.format("User selected range: MIN: %s, MAX: %s", minValue, maxValue));
		        	// handle changed range values
		        	logText.setText("User selected range: MIN=" + minValue + ", MAX=" + maxValue);
		        	mObjectParameters.setStartPosition(minValue);
		        	mObjectParameters.setLengthOfSelectedPlayback(maxValue - minValue);
		        	
		        }
		});
		
		// Preferred way of loading and setting background
		executeLoadingOfImagesInAsyncTask(listOfBitmaps, mTrimControl);

		// Add trim control to layout.
		ViewGroup mLayout = (ViewGroup) findViewById(R.id.test_layout);
		mLayout.addView(mTrimControl);
		
	}
	
	private void executeLoadingOfImagesInAsyncTask(List<Bitmap> listOfBitmaps, TrimControl<Long> trimControl) {
		new BitmapWorkerTask(listOfBitmaps, trimControl).execute();
	}

	
	//class for convenience to set and get startPosition and length.
	class ObjectParameters {
		private long mBaseStartPosition;
		private long mLengthOfSelectedArea;
		
		
		public long getStartPosition() {
			return mBaseStartPosition;
		}
		public void setStartPosition(long mBaseStartPosition) {
			this.mBaseStartPosition = mBaseStartPosition;
		}
		public long getLengthOfSelectedArea() {
			return mLengthOfSelectedArea;
		}
		public void setLengthOfSelectedPlayback(long mLengthOfSelectedArea) {
			this.mLengthOfSelectedArea = mLengthOfSelectedArea;
		}
		
		
	}
	
	/**
	 * Convenient class to execute a process of combining list of images 
	 * to one big Bitmap.
	 *
	 */
	class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
	    private List<Bitmap> mListOfBitmaps;
	    private Bitmap mResultBackgroundImage;
	    private TrimControl<Long> mTrimControl;

	    public BitmapWorkerTask(List<Bitmap> resourseBitmaps, TrimControl<Long> seeker) {
	        this.mListOfBitmaps = resourseBitmaps;
	        this.mTrimControl = seeker;
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(Void... params) {
	    	this.mResultBackgroundImage = ImageUtils.combineBitmapsToOne(mListOfBitmaps); 
	    	return mResultBackgroundImage;
	        
	    }

	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	mTrimControl.setBackgroundImageArea(bitmap);
	    	mTrimControl.invalidate();
	    }
	}

}
