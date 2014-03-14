package course.labs.GraphicsLab;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.RelativeLayout;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BubbleActivity extends Activity {

	// These variables are for testing purposes, do not modify
	private final static int RANDOM = 0;
	private final static int SINGLE = 1;
	private final static int STILL = 2;
	private static int speedMode = RANDOM;

	private static final int MENU_STILL = Menu.FIRST;
	private static final int MENU_SINGLE_SPEED = Menu.FIRST + 1;
	private static final int MENU_RANDOM_SPEED = Menu.FIRST + 2;

	private static final String TAG = "Lab-Graphics";

	// Main view
	private RelativeLayout mFrame;

	// Bubble image
	private Bitmap mBitmap;

	// Display dimensions
	private int mDisplayWidth, mDisplayHeight;

	// Sound variables

	// AudioManager
	private AudioManager mAudioManager;
	// SoundPool
	private SoundPool mSoundPool;
	// ID for the bubble popping sound
	private int mSoundID;
	// Audio volume
	private float mStreamVolume;

	// Gesture Detector
	private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Set up user interface
		mFrame = (RelativeLayout) findViewById(R.id.frame);

		// Load basic bubble Bitmap
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Manage bubble popping sound
		// Use AudioManager.STREAM_MUSIC as stream type

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		mStreamVolume = (float) mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC)
				/ mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);


		// TODO - make a new SoundPool, allowing up to 10 streams 
		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

		// TODO - set a SoundPool OnLoadCompletedListener that calls setupGestureDetector()
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                setupGestureDetector();
            }
        });

		
		// TODO - load the sound from res/raw/bubble_pop.wav
		mSoundID = mSoundPool.load(getApplicationContext(), R.raw.bubble_pop, 1);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {

			// Get the size of the display so this view knows where borders are
			mDisplayWidth = mFrame.getWidth();
			mDisplayHeight = mFrame.getHeight();

		}
	}

	// Set up GestureDetector
	private void setupGestureDetector() {

		mGestureDetector = new GestureDetector(this,

		new GestureDetector.SimpleOnGestureListener() {

			// If a fling gesture starts on a BubbleView then change the
			// BubbleView's velocity

			@Override
			public boolean onFling(MotionEvent event1, MotionEvent event2,
					float velocityX, float velocityY) {

				// TODO - Implement onFling actions.
				// You can get all Views in mFrame using the
				// ViewGroup.getChildCount() method

				for (int i = 0; i < mFrame.getChildCount(); i++) {
					
					BubbleView flingBubble = (BubbleView) mFrame.getChildAt(i);
					
					// if the fling action is inside a bubble then change the velocity and return true
					if ( flingBubble.intersects(event1.getX(), event1.getY()) ) {
						
						flingBubble.deflect(velocityX, velocityY);
						return true;
					}
				}
				// if the fling action is outside a bubble return false
				return false;
				
			}

			// If a single tap intersects a BubbleView, then pop the BubbleView
			// Otherwise, create a new BubbleView at the tap's location and add
			// it to mFrame. You can get all views from mFrame with ViewGroup.getChildAt()

			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {

				// TODO - Implement onSingleTapConfirmed actions.
				// You can get all Views in mFrame using the
				// ViewGroup.getChildCount() method

				for (int i=0; i<mFrame.getChildCount(); i++) {
					// if event intersects a bubble, pop it and return true
					BubbleView popBubble = (BubbleView) mFrame.getChildAt(i);

                    if (popBubble != null && popBubble.intersects(event.getX(), event.getY())) {
                        if (popBubble.intersects(event.getX(), event.getY())) {
                        	popBubble.stop(true);
    						return true;
                        }
                    }
                }

				// if event doesn't intersects a bubble, create a new one at x & y and return false
				BubbleView createBubble = new BubbleView(mFrame.getContext(),event.getX(),event.getY());
				mFrame.addView(createBubble);
				createBubble.start();
				return false;

			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// TODO - delegate the event to the gestureDetector
		return mGestureDetector.onTouchEvent(event);
	
	}

	@Override
	protected void onPause() {
		
		// TODO - Release all SoundPool resources
		if (null != mSoundPool) {
			mSoundPool.unload(mSoundID);
			mSoundPool.release();
			mSoundPool = null;
		}
		mAudioManager.unloadSoundEffects();
		
		super.onPause();
	}

	// BubbleView is a View that displays a bubble.
	// This class handles animating, drawing, popping amongst other actions.
	// A new BubbleView is created for each bubble on the display

	private class BubbleView extends View {

		private static final int BITMAP_SIZE = 64;
		private static final int REFRESH_RATE = 40;
		private final Paint mPainter = new Paint();
		private ScheduledFuture<?> mMoverFuture;
		private int mScaledBitmapWidth;
		private Bitmap mScaledBitmap;

		// location, speed and direction of the bubble
		private float mXPos, mYPos, mDx, mDy;
		private long mRotate, mDRotate;

		public BubbleView(Context context, float x, float y) {
			super(context);
			log("Creating Bubble at: x:" + x + " y:" + y);

			// Create a new random number generator to
			// randomize size, rotation, speed and direction
			Random r = new Random();

			// Creates the bubble bitmap for this BubbleView
			createScaledBitmap(r);

			// Adjust position to center the bubble under user's finger
			mXPos = x - mScaledBitmapWidth / 2;
			mYPos = y - mScaledBitmapWidth / 2;

			// Set the BubbleView's speed and direction
			setSpeedAndDirection(r);
			
			// Set the BubbleView's rotation
			setRotation(r);

			mPainter.setAntiAlias(true);

		}

		private void setRotation(Random r) {

			if (speedMode == RANDOM) {
				// TODO - set rotation in range [1..3]
				mDRotate = r.nextInt(3) + 1;
			} else {
				mDRotate = 0;
			}
		}

		private void setSpeedAndDirection(Random r) {

			// Used by test cases
			switch (speedMode) {

			case SINGLE:

				// Fixed speed
				mDx = 10;
				mDy = 10;
				break;

			case STILL:

				// No speed
				mDx = 0;
				mDy = 0;
				break;

			default:

				// TODO - Set movement direction and speed
				// Limit movement speed in the x and y
				// direction to [-3..3].

                mDx = r.nextInt(7) - 3;
                mDy = r.nextInt(7) - 3;
			}
		}

		private void createScaledBitmap(Random r) {

			if (speedMode != RANDOM) {

				mScaledBitmapWidth = BITMAP_SIZE * 3;
			
			} else {
			
				//TODO - set scaled bitmap size in range [1..3] * BITMAP_SIZE
                // 2 - 0, 1
                // 3 - 1, 2, 3
				mScaledBitmapWidth = (r.nextInt(3) + 1) * BITMAP_SIZE;
			}

			// TODO - create the scaled bitmap using size set above
			mScaledBitmap = Bitmap.createScaledBitmap(mBitmap, mScaledBitmapWidth, 
														mScaledBitmapWidth, false);
			log("scaledBMP created with bitmapWidth: "+mScaledBitmapWidth);
		}

		// Start moving the BubbleView & updating the display
		private void start() {

			// Creates a WorkerThread
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			// Execute the run() in Worker Thread every REFRESH_RATE
			// milliseconds
			// Save reference to this job in mMoverFuture
			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					// TODO - implement movement logic.
					// Each time this method is run the BubbleView should
					// move one step. If the BubbleView exits the display, 
					// stop the BubbleView's Worker Thread. 
					// Otherwise, request that the BubbleView be redrawn.
					
					if (moveWhileOnScreen()) {
                        BubbleView.this.postInvalidate();
                    } else {
                        BubbleView.this.stop(false);
                    }

				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}

		private synchronized boolean intersects(float x, float y) {

			// TODO - Return true if the BubbleView intersects position (x,y)
			return mXPos < x && x < mXPos + mScaledBitmap.getWidth()
                    && mYPos < y && y < mYPos + mScaledBitmap.getHeight();
		}

		// Cancel the Bubble's movement
		// Remove Bubble from mFrame
		// Play pop sound if the BubbleView was popped
		
		private void stop(final boolean popped) {

			if (null != mMoverFuture && mMoverFuture.cancel(true)) {

				// This work will be performed on the UI Thread
				
				mFrame.post(new Runnable() {
					@Override
					public void run() {
						
						// TODO - Remove the BubbleView from mFrame
                        mFrame.removeView(BubbleView.this);
						
						if (popped) {
							log("Pop!");

							// TODO - If the bubble was popped by user,
							// play the popping sound
                            mSoundPool.play(mSoundID, mStreamVolume, mStreamVolume, 1, 0, 1);
						}

						log("Bubble removed from view!");
					
					}
				});
			}
		}

		// Change the Bubble's speed and direction
		private synchronized void deflect(float velocityX, float velocityY) {
			log("velocity X:" + velocityX + " velocity Y:" + velocityY);

			//TODO - set mDx and mDy to be the new velocities divided by the REFRESH_RATE
			mDx = velocityX / REFRESH_RATE;
			mDy = velocityY / REFRESH_RATE;

		}

		// Draw the Bubble at its current location
		@Override
		protected synchronized void onDraw(Canvas canvas) {

			// TODO - save the canvas
            canvas.save();


			// TODO - increase the rotation of the original image by mDRotate
            mRotate = ( mRotate + mDRotate ) % 360;

			
			// TODO Rotate the canvas by current rotation
            canvas.rotate(mRotate, mXPos + mScaledBitmapWidth / 2, mYPos + mScaledBitmapWidth / 2);
			
			
			// TODO - draw the bitmap at it's new location
			canvas.drawBitmap(mScaledBitmap, mXPos, mYPos, mPainter);

			
			// TODO - restore the canvas
            canvas.restore();

		}


		private synchronized boolean moveWhileOnScreen() {

			// TODO - Move the BubbleView
			// Returns true if the BubbleView has exited the screen
            mXPos += mDx;
            mYPos += mDy;


			return !isOutOfView();

		}

		private boolean isOutOfView() {

			// TODO - Return true if the BubbleView has exited the screen
			return mXPos < 0 ||
                    mYPos < 0 ||
                    mXPos + mScaledBitmap.getWidth() > mDisplayWidth ||
                    mYPos + mScaledBitmap.getHeight() > mDisplayHeight;

		}
	}

	// Do not modify below here
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(Menu.NONE, MENU_STILL, Menu.NONE, "Still Mode");
		menu.add(Menu.NONE, MENU_SINGLE_SPEED, Menu.NONE, "Single Speed Mode");
		menu.add(Menu.NONE, MENU_RANDOM_SPEED, Menu.NONE, "Random Speed Mode");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_STILL:
			speedMode = STILL;
			return true;
		case MENU_SINGLE_SPEED:
			speedMode = SINGLE;
			return true;
		case MENU_RANDOM_SPEED:
			speedMode = RANDOM;
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private static void log (String message) {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i(TAG,message);
	}
}