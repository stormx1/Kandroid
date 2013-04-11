package com.kac.customcomponents.utils;

import java.util.List;
import java.util.ListIterator;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

public class ImageUtils {
	
	public static Bitmap combineImages(Bitmap c, Bitmap s) { 
	    Bitmap cs = null; 

	    int width, height = 0; 

	    if(c.getWidth() > s.getWidth()) { 
	      width = c.getWidth() + s.getWidth(); 
	      height = c.getHeight(); 
	    } else { 
	      width = s.getWidth() + s.getWidth(); 
	      height = c.getHeight(); 
	    } 

	    cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 

	    Canvas comboImage = new Canvas(cs); 

	    comboImage.drawBitmap(c, 0f, 0f, null); 
	    comboImage.drawBitmap(s, c.getWidth(), 0f, null); 

	    return cs; 
	} 
	
	// basic implementation
	public static Bitmap combineBitmapsToOne(List<Bitmap> bitmaps) {
		Bitmap result = null;
		Bitmap current = null;
		Bitmap next = null;
		
		ListIterator<Bitmap> iterator = bitmaps.listIterator();
		
		int counter = 0;
		while(iterator.hasNext()) {
			current = iterator.next();					
		    
			if (iterator.hasNext() && counter == 0) {
		       next = iterator.next();
		    } 
			//
			if(current != null && next != null && result != null) {
				result = ImageUtils.combineImages(result, current);
		    } else if(current != null && next != null) {
		    	result = ImageUtils.combineImages(current, next);
		    	counter++;
		    }
		}
		
		current.recycle();
		next.recycle(); 
		
		counter = 0;
		return result;
		
	}
	
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	
	        // Calculate ratios of height and width to requested height and width
	        final int heightRatio = Math.round((float) height / (float) reqHeight);
	        final int widthRatio = Math.round((float) width / (float) reqWidth);
	
	        // Choose the smallest ratio as inSampleSize value, this will guarantee
	        // a final image with both dimensions larger than or equal to the
	        // requested height and width.
	        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
	    }
	
	    return inSampleSize;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(res, resId, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeResource(res, resId, options);
	}
}
