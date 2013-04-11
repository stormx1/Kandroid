package com.kac.customcomponents;



import com.dataart.customcomponents.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class TrimControl<T extends Number> extends View {
	
	private final Bitmap mThumbImage = BitmapFactory.decodeResource(
			getResources(), R.drawable.btn_radio_off_pressed_holo_dark);
	private final Bitmap mThumbPressedImage = BitmapFactory.decodeResource(
			getResources(), R.drawable.btn_radio_on_pressed_holo_dark);
	
	private static final int FILTER_RADIUS = 15;
	private static final BlurMaskFilter FADE_MASK_FILTER = new BlurMaskFilter(FILTER_RADIUS, Blur.NORMAL);
	private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_FOR_SHADOW_BOXES = new Paint();
	private static final RectF RIGHT_SHADOW_BOX = new RectF();
	private static final RectF LEFT_SHADOW_BOX = new RectF();
	private static final RectF MAIN_AREA_BOX = new RectF();
	private static final int INVALID_POINTER_ID = 255;
	private static final int START_POSITION = 0;
	private static final float ROUNDED_PX = 12;
		
	// parameters which are used to compute thumbs positions etc.
	private final float mThumbWidth = mThumbImage.getWidth();
	private final float mThumbHalfWidth = 0.5f * mThumbWidth;
	private final float mThumbHalfHeight = 0.5f * mThumbImage.getHeight();
	private final float mLineHeight = 5.0f * mThumbHalfHeight;
	private final float mPadding = mThumbHalfWidth;
		
	private final T mAbsoluteMinValue, mAbsoluteMaxValue;
	private final NumericType mNumberType;
	private final double mAbsoluteMinValuePrim, mAbsoluteMaxValuePrim;
	
	private int mActivePointerId = INVALID_POINTER_ID;
	private double mNormalizedMinValue = 0d;
	private double mNormalizedMaxValue = 1d;
	private boolean isDraggingState = false;
	private float mTouchDownMotionX;
	private int mScaledTouchSlop;
	private boolean mIsDragging;
	
	private Thumb mPressedThumb = null;
	
	private Bitmap mResultBigOne;
	private Bitmap mBackgroundImage;
	
	private Bitmap mScaledBackgroundBitmap;
	private final SparseArray<Bitmap> mControlBackgroundImageCache = new SparseArray<Bitmap>();
	
	private boolean isMiddleArreaTouched = false;
	
	// helper constants to determine which thumb/area was clicked
	private static enum Thumb {
		LEFT, MIDDLE, RIGHT 
	};
	
	private OnTrimControlChangeListener<T> mValueChangeListener;
	
	
	static {
		PAINT_FOR_SHADOW_BOXES.setMaskFilter(FADE_MASK_FILTER);
		PAINT_FOR_SHADOW_BOXES.setAntiAlias(true);
		PAINT_FOR_SHADOW_BOXES.setColor(Color.BLACK);
		// Set transparency for paint boxes
		// (left and right shadows on background image)
		PAINT_FOR_SHADOW_BOXES.setAlpha(175);
	}
	

	/**
	 * Creates a new TrimControl.
	 * 
	 * @param absoluteMinValue
	 *            The minimum value of the selectable range.
	 * @param absoluteMaxValue
	 *            The maximum value of the selectable range.
	 * @param context
	 * @throws IllegalArgumentException
	 *             Will be thrown if min/max value type is not one of 
	 *             Byte, Short, Integer, Long, BigDecimal, Float, Double.
	 *                          
	 */
	public TrimControl(Context context,	T absoluteMinValue, T absoluteMaxValue)
			throws IllegalArgumentException {
		this(context, null, absoluteMinValue, absoluteMaxValue);
	}
	
	/**
	 * Creates a new TrimControl.
	 * 
	 * @param absoluteMinValue
	 *            The minimum value of the selectable range.
	 * @param absoluteMaxValue
	 *            The maximum value of the selectable range.
	 * @param context
	 * @throws IllegalArgumentException
	 *             Will be thrown if min/max value type is not one of 
	 *             Byte, Short, Integer, Long, BigDecimal, Float, Double.
	 */
	public TrimControl(Context context, AttributeSet attrs,
			T absoluteMinValue, T absoluteMaxValue)
			throws IllegalArgumentException {
		this(context, attrs, 0, absoluteMinValue, absoluteMaxValue);
		
	}

	/**
	 * Creates a new TrimControl.
	 * 
	 * @param absoluteMinValue
	 *            The minimum value of the selectable range.
	 * @param absoluteMaxValue
	 *            The maximum value of the selectable range.
	 * @param context
	 * @throws IllegalArgumentException
	 *             Will be thrown if min/max value type is not one of 
	 *             Byte, Short, Integer, Long, BigDecimal, Float, Double.
	 */
	public TrimControl(Context context, AttributeSet attrs, int defStyle, 
			T absoluteMinValue, T absoluteMaxValue)
			throws IllegalArgumentException {
		super(context, attrs, defStyle);

		this.mAbsoluteMinValue = absoluteMinValue;
		this.mAbsoluteMaxValue = absoluteMaxValue;
		mAbsoluteMinValuePrim = absoluteMinValue.doubleValue();
		mAbsoluteMaxValuePrim = absoluteMaxValue.doubleValue();
		mNumberType = NumericType.fromNumber(absoluteMinValue);

		setFocusable(true);
		setFocusableInTouchMode(true);

    	init(context);
		
		
	}
	
	private final void init(Context context) {
		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}
	
	/**
	 * Handles thumb selection and movement. Notifies listener callback on
	 * certain events.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}

		int mPointerIndex;
		final int mAction = event.getAction();
				
		switch (mAction & MotionEvent.ACTION_MASK) {

		case MotionEvent.ACTION_DOWN:
			// Remember where the motion event started
			mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
			mPointerIndex = event.findPointerIndex(mActivePointerId);

			mTouchDownMotionX = event.getX(mPointerIndex);
			mPressedThumb = evalPressedThumb(mTouchDownMotionX);

			// Only handle thumb presses.
			boolean isMiddleTouch = false;
			if (mPressedThumb == null) {
				isMiddleTouch = evalPressedMiddleArea(mTouchDownMotionX);

				if (!isMiddleTouch) {
					return super.onTouchEvent(event);
				} else {
					mPressedThumb = Thumb.MIDDLE;
				}

			}

			setPressed(true);
			invalidate();
			onStartTrackingTouch();
			trackTouchEvent(event);
			attemptToPreventInterceptingOfTouchByParent();

			break;
		case MotionEvent.ACTION_MOVE:
			if (mPressedThumb != null) {

				if (mIsDragging) {
					trackTouchEvent(event);
				} else {
					// Scroll to follow the motion event
					mPointerIndex = event.findPointerIndex(mActivePointerId);
					final float mX = event.getX(mPointerIndex);

					if (Math.abs(mX - mTouchDownMotionX) > mScaledTouchSlop) {
						setPressed(true);
						invalidate();
						onStartTrackingTouch();
						trackTouchEvent(event);
						attemptToPreventInterceptingOfTouchByParent();
					}
				}

				if (isDragging() && mValueChangeListener != null) {
					mValueChangeListener.onTrimControlValuesChanged(this,
							getSelectedMinValue(), getSelectedMaxValue());
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsDragging) {
				trackTouchEvent(event);
				onStopTrackingTouch();
				setPressed(false);
			} else {
				// Touch up when we never crossed the touch slop threshold
				// should be interpreted as a tap-seek to that location.
				onStartTrackingTouch();
				trackTouchEvent(event);
				onStopTrackingTouch();
			}

			mPressedThumb = null;
			invalidate();
			if (mValueChangeListener != null) {
				mValueChangeListener.onTrimControlValuesChanged(this,
						getSelectedMinValue(), getSelectedMaxValue());
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int mIndex = event.getPointerCount() - 1;
			mTouchDownMotionX = event.getX(mIndex);
			mActivePointerId = event.getPointerId(mIndex);
			invalidate();
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(event);
			invalidate();
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsDragging) {
				onStopTrackingTouch();
				setPressed(false);
			}
			invalidate();
			break;
		}
		return true;
	}
	
	/**
	 * Decides which (if any) thumb is touched by the given x-coordinate.
	 * 
	 * @param touchX
	 *            The x-coordinate of a touch event in screen space.
	 * @return The pressed thumb or null if none has been touched.
	 */
	private Thumb evalPressedThumb(float touchX) {
		Thumb mResult = null;
		boolean mMinThumbPressed = isTouchInThumbRange(touchX, mNormalizedMinValue);
		boolean mMaxThumbPressed = isTouchInThumbRange(touchX, mNormalizedMaxValue);
		
		float mNormalizedMinThumbRange = normalizedToScreen(mNormalizedMinValue);
		float mNormalizedMaxThumbRange = normalizedToScreen(mNormalizedMaxValue);
		
		// Both thumbs are pressed (lie on top of each other),
		// choose one with more space to drag. (not being able to drag them apart anymore)
		if (mMinThumbPressed && mMaxThumbPressed) {
			mResult = (touchX / getWidth() > 0.5f) ? Thumb.LEFT : Thumb.RIGHT;
		} else if (mMinThumbPressed || touchX < mNormalizedMinThumbRange && !isMiddleArreaTouched) {
			mResult = Thumb.LEFT;
		} else if (mMaxThumbPressed || touchX > mNormalizedMaxThumbRange && !isMiddleArreaTouched) {
			mResult = Thumb.RIGHT;
		} 
		return mResult;
	}

	/**
	 * Decides if middle area is touched by the given x-coordinate.
	 * 
	 * @param touchX
	 *            The x-coordinate of a touch event in screen space.
	 * @return true if pressed middle area otherwise false.
	 */
	private boolean evalPressedMiddleArea(float touchX) {
		boolean mResult = false;
		
		float mNormalizedMinThumbRange = normalizedToScreen(mNormalizedMinValue);
		float mNormalizedMaxThumbRange = normalizedToScreen(mNormalizedMaxValue);
		
		if (touchX > mNormalizedMinThumbRange && touchX < mNormalizedMaxThumbRange) {
			mResult = true;
		} else if(touchX >= mNormalizedMinThumbRange - mThumbHalfWidth 
				&& touchX <= mNormalizedMaxThumbRange + mThumbHalfWidth) {
			mResult = true;
		}

		return mResult;
	}
	
	private final void trackTouchEvent(MotionEvent event) {
		final int mPointerIndex = event.findPointerIndex(mActivePointerId);
		final float mX = event.getX(mPointerIndex);
		
		if (Thumb.LEFT.equals(mPressedThumb)) {
			setNormalizedMinValue(screenToNormalized(mX));
		} else if (Thumb.RIGHT.equals(mPressedThumb)) {
			setNormalizedMaxValue(screenToNormalized(mX));
		} else if (Thumb.MIDDLE.equals(mPressedThumb)) {
			moveMiddleWindow(mX);
		}
	}
	
	private final void onSecondaryPointerUp(MotionEvent ev) {
		int mPointerIndex = 
				(ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		int mPointerId = ev.getPointerId(mPointerIndex);
		
		if (mPointerId == mActivePointerId) {
			int mPointerIndexValue = mPointerIndex == 0 ? 1 : 0;
			
			mTouchDownMotionX = ev.getX(mPointerIndexValue);
			mActivePointerId = ev.getPointerId(mPointerIndexValue);
		}
	}
	
	/**
	 * Move middle area (window). This will invalidate the widget.
	 * @param x
	 * 		touch position (coordinate)
	 */
	private void moveMiddleWindow(float x) {
		float mDiff = x - mTouchDownMotionX;
		if(mDiff ==  0) {
			return;
		}
		
		double mDifference = screenToNormalizedCoord(mDiff);
		double mOldNormalizedMinValue = mNormalizedMinValue;
		double mOldNormalizedMaxValue = mNormalizedMaxValue;
		
		double mMiddleArea = mOldNormalizedMaxValue - mOldNormalizedMinValue;
		
		mNormalizedMinValue += mDifference;
		mNormalizedMaxValue += mDifference;
		
		if(mNormalizedMinValue < 0) {
			mNormalizedMinValue = 0;
			mNormalizedMaxValue = mMiddleArea;				
		}
		if(mNormalizedMaxValue > 1) {
			mNormalizedMaxValue = 1;
			mNormalizedMinValue = 1 - mMiddleArea;				
		}
		mDiff = 0;
		mDifference = 0d;
		mTouchDownMotionX = x;
		
		invalidate();
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		setParametersForRectF(RIGHT_SHADOW_BOX, 
				mPadding, 
				0.5f * (getHeight() - mLineHeight), 
				getWidth(), 
				0.5f * (getHeight() + mLineHeight));
		
		setParametersForRectF(LEFT_SHADOW_BOX, 
				START_POSITION, 
				0.5f * (getHeight() - mLineHeight), 
				getWidth(), 
				0.5f * (getHeight() + mLineHeight));
		
		setParametersForRectF(MAIN_AREA_BOX, 
				mPadding, 
				0.5f * (getHeight() - mLineHeight), 
				getWidth() - mPadding, 
				0.5f * (getHeight() + mLineHeight));
		

		PAINT.setStyle(Style.FILL);
		PAINT.setColor(Color.BLACK);
		PAINT.setAntiAlias(true);

		canvas.drawRect(MAIN_AREA_BOX, PAINT);

		// some sort of cache to prevent frequently scaling of the big image.
		// Scaling is greedy to memory. Can cause OutOfMemoryException.
		// If image is big - it should be downscaled
		mResultBigOne = getBackgroundImageArea();
		if (mResultBigOne == null) {
			return;
		}
		
		setBackgroundImageFromCacheById(mResultBigOne);
		
		// Background widget's image
		canvas.drawBitmap(mScaledBackgroundBitmap, START_POSITION, 0.5f * (getHeight() - mLineHeight), PAINT);

		PAINT.setColor(Color.TRANSPARENT);
		PAINT.setMaskFilter(FADE_MASK_FILTER);

		canvas.drawRect(MAIN_AREA_BOX, PAINT);

		// draw seek bar active range line
		MAIN_AREA_BOX.left = normalizedToScreen(mNormalizedMinValue);
		MAIN_AREA_BOX.right = normalizedToScreen(mNormalizedMaxValue);

		// set the faded boxes, updates the boundaries
		RIGHT_SHADOW_BOX.left = MAIN_AREA_BOX.right;
		LEFT_SHADOW_BOX.right = MAIN_AREA_BOX.left;

		canvas.drawRoundRect(MAIN_AREA_BOX, ROUNDED_PX, ROUNDED_PX, PAINT);

		PAINT.setStyle(Style.FILL);
		PAINT.setColor(Color.WHITE);

		canvas.drawRect(RIGHT_SHADOW_BOX, PAINT_FOR_SHADOW_BOXES);
		canvas.drawRect(LEFT_SHADOW_BOX, PAINT_FOR_SHADOW_BOXES);

		// draw minimum thumb
		drawThumb(normalizedToScreen(mNormalizedMinValue),
				Thumb.LEFT.equals(mPressedThumb), canvas);

		// draw maximum thumb
		drawThumb(normalizedToScreen(mNormalizedMaxValue),
				Thumb.RIGHT.equals(mPressedThumb), canvas);
	}
	
	/**
	 * Trying to get big background image from cache if exists. If there is no 
	 * image in cache - create scaled bitmap. This is some sort of prevent mechanism 
	 * for infinite scaling operation on every redraw. Gives some boost to performance.
	 * 
	 * @param imageToBeScaled
	 * 		works like key in the cache, by which the image is held.
	 * @return
	 * 		cache or newly created and put to cache image.
	 */
	private void setBackgroundImageFromCacheById(Bitmap imageToBeScaled) {
		mScaledBackgroundBitmap = mControlBackgroundImageCache.get(imageToBeScaled.hashCode());
		if(mScaledBackgroundBitmap == null) {
			mScaledBackgroundBitmap = Bitmap.createScaledBitmap(imageToBeScaled, getWidth(), getHeight(), false);
			mControlBackgroundImageCache.put(imageToBeScaled.hashCode(), mScaledBackgroundBitmap);
		}
	}
	
	// helper method to set parameters (left, top, right, bottom) for RectF 
	private void setParametersForRectF(RectF box, float left, float top, float right, float bottom) {
		box.left = left;
		box.top = top;
		box.right = right;
		box.bottom = bottom;
	}
	
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		int mWidth = 200;
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
		}
		int height = (int) mLineHeight;
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
			height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
		}
		setMeasuredDimension(mWidth, height);
	}

	/**
	 * Should the widget notify the listener callback while the user is still
	 * dragging a thumb? Default is false.
	 * 
	 * @param flag
	 */
	public void setDraggingState(boolean flag) {
		this.isDraggingState = flag;
	}
	
	public boolean isDragging() {
		return isDraggingState;
	}

	/**
	 * Returns the absolute minimum value of the range that has been set at
	 * construction time.
	 * 
	 * @return The absolute minimum value of the range.
	 */
	public T getAbsoluteWidgetMinValue() {
		return mAbsoluteMinValue;
	}

	/**
	 * Returns the absolute maximum value of the range that has been set at
	 * construction time.
	 * 
	 * @return The absolute maximum value of the range.
	 */
	public T getAbsoluteWidgetMaxValue() {
		return mAbsoluteMaxValue;
	}

	/**
	 * Returns the currently selected minimum value.
	 * 
	 * @return The currently selected min value.
	 */
	public T getSelectedMinValue() {
		return normalizedToValue(mNormalizedMinValue);
	}

	/**
	 * Sets the selected minimum value. The widget will be redrawn.
	 * 
	 * @param value
	 *            The Number value to set the minimum value to. Will be clamped
	 *            to given absolute minimum/maximum range.
	 */
	public void setSelectedMinValue(T value) {
		// if absoluteMinValue == absoluteMaxValue, avoid division by zero
		// when normalizing.
		if ((mAbsoluteMaxValuePrim - mAbsoluteMinValuePrim) == 0) {
			setNormalizedMinValue(0d);
		} else {
			setNormalizedMinValue(valueToNormalized(value));
		}
	}

	/**
	 * Returns the currently selected maximum value.
	 * 
	 * @return The currently selected maximum value.
	 */
	public T getSelectedMaxValue() {
		return normalizedToValue(mNormalizedMaxValue);
	}

	/**
	 * Sets the selected maximum value. The widget will be redrawn.
	 * 
	 * @param value
	 *            The Number value to set the maximum value to. Value will be normalized.
	 */
	public void setSelectedMaxValue(T value) {
		// in case absoluteMinValue == absoluteMaxValue, avoid division by zero
		// when normalizing.
		if ((mAbsoluteMaxValuePrim - mAbsoluteMinValuePrim) == 0) {
			setNormalizedMaxValue(1d);
		} else {
			setNormalizedMaxValue(valueToNormalized(value));
		}
	}
	
	private double screenToNormalizedCoord(float screenCoord) {
		double mResult;
		int mWidth = getWidth();
		if (mWidth <= 2 * mPadding) {
			// prevent division by zero, simply return 0.
			mResult = 0d;
		} else {
			mResult = (screenCoord) / (mWidth - 2 * mPadding);
		}
		return mResult;
	}
	
	/**
	 * Prevent ancestors from stealing events in the drag.
	 */
	private void attemptToPreventInterceptingOfTouchByParent() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}

	/**
	 * Called when the user has started touching this widget.
	 */
	void onStartTrackingTouch() {
		mIsDragging = true;
	}

	/**
	 * Called when the user either releases his touch or the touch is
	 * canceled.
	 */
	void onStopTrackingTouch() {
		mIsDragging = false;
	}

	
	/**
	 * Draws the "normal" or "pressed" thumb image on specified x-coordinate.
	 * 
	 * @param screenCoord
	 *            The x-coordinate in screen coordinates where to draw the image.
	 * @param pressed
	 *            Is the thumb currently in "pressed" state?
	 * @param canvas
	 *            The canvas to draw upon.
	 */
	private void drawThumb(float screenCoord, boolean pressed, Canvas canvas) {
		canvas.drawBitmap(pressed ? mThumbPressedImage : mThumbImage, screenCoord
				- mThumbHalfWidth,
				(float) ((0.5f * getHeight()) - mThumbHalfHeight), PAINT);
	}
	

	/**
	 * 
	 * Decide if we touched in the thumb range or not.
	 * 
	 * @param touchX
	 *            The x-coordinate in screen space to check.
	 * @param normalizedThumbValue
	 *            The normalized x-coordinate of the thumb to check.
	 * @return true if x-coordinate is in thumb range, false otherwise.
	 */
	private boolean isTouchInThumbRange(float touchX, double normalizedThumbValue) {
		return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= mThumbHalfWidth;
	}

	/**
	 * Sets normalized min value to value so that 0 <= value <= normalized max
	 * value <= 1. The View will get invalidated when calling this method.
	 * 
	 * @param value
	 *            The new normalized min value to set.
	 */
	public void setNormalizedMinValue(double value) {
		mNormalizedMinValue = Math.max(0d,
				Math.min(1d, Math.min(value, mNormalizedMaxValue)));
		invalidate();
	}

	/**
	 * Sets normalized max value to value so that 0 <= normalized min value <=
	 * value <= 1. The View will get invalidated when calling this method.
	 * 
	 * @param value
	 *            The new normalized max value to set.
	 */
	public void setNormalizedMaxValue(double value) {
		mNormalizedMaxValue = Math.max(0d,
				Math.min(1d, Math.max(value, mNormalizedMinValue)));
		invalidate();
	}

	/**
	 * Converts a normalized value to a Number object in the value between
	 * absolute widget bounds minimum and maximum.
	 * 
	 * @param normalized
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private T normalizedToValue(double normalized) {
		return (T) mNumberType.toNumber(mAbsoluteMinValuePrim + normalized
				* (mAbsoluteMaxValuePrim - mAbsoluteMinValuePrim));
	}

	/**
	 * Converts the given Number value to a normalized double.
	 * 
	 * @param value
	 *            The Number value to normalize.
	 * @return The normalized double.
	 */
	private double valueToNormalized(T value) {
		if (mAbsoluteMaxValuePrim - mAbsoluteMinValuePrim == 0) {
			return 0d;
		}
		return (value.doubleValue() - mAbsoluteMinValuePrim)
				/ (mAbsoluteMaxValuePrim - mAbsoluteMinValuePrim);
	}

	/**
	 * Converts a normalized value into screen coordinates (px).
	 * 
	 * @param normalizedCoord
	 *            The normalized value to convert.
	 * @return The converted value in px.
	 */
	private float normalizedToScreen(double normalizedCoord) {
		return (float) (mPadding + normalizedCoord * (getWidth() - 2 * mPadding));
	}

	/**
	 * Converts screen x-coordinates into normalized value.
	 * 
	 * @param screenCoord
	 *            The x-coordinate in screen space to convert.
	 * @return The normalized value.
	 */
	private double screenToNormalized(float screenCoord) {
		int mWidth = getWidth();
		if (mWidth <= 2 * mPadding) {
			// prevent division by zero, simply return 0.
			return 0d;
		} else {
			double mResult = (screenCoord - mPadding) / (mWidth - 2 * mPadding);
			return Math.min(1d, Math.max(0d, mResult));
		}
	}
	
	/**
	 * Get widget background image. This image will be scaled already.
	 * @return Bitmap widget background scaled image.
	 */
	public Bitmap getBackgroundImageArea() {
		return mBackgroundImage;
	}

	/**
	 * Helper method that could be used, for example, in {@link AsyncTask} to
	 * set a background from the newly created image. 
	 * <div><b>Warning:</b> images should be scaled down and combined to one to guarantee 
	 * a better performance.</div> 
	 * @param backgroundImage
	 */
	public void setBackgroundImageArea(Bitmap backgroundImage) {
		this.mBackgroundImage = backgroundImage;
	}
	
	/**
	 * Registers given listener callback to notify about changed selected
	 * values.
	 * 
	 * @param listener
	 *            The listener to notify about changed selected values.
	 */
	public void setOnTrimControlChangeListener(OnTrimControlChangeListener<T> listener) {
		this.mValueChangeListener = listener;
	}

	public interface OnTrimControlChangeListener<T> {
		public void onTrimControlValuesChanged(TrimControl<?> bar, T minValue,
				T maxValue);
	}
}