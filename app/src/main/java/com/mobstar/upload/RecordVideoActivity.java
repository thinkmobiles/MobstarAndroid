package com.mobstar.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobstar.R;
import com.mobstar.utils.CameraUtility;
import com.mobstar.utils.Utility;

import java.io.IOException;
import java.util.List;

public class RecordVideoActivity extends Activity implements SensorEventListener {

	private Context mContext;
	private Camera mCamera;
	private CameraPreview mPreview;
	private TextView tvFlash;
	private LinearLayout btnFlash;
	private ImageView btnRecord, ivFlash, btnChangeCamera;
	private boolean isFlashOn = false;
	private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
	private boolean isFrontCameraAvailable = false;
	private boolean isRecording = false;
	private MediaRecorder mMediaRecorder;
	private LinearLayout layoutCameraOption;
	private TextView textRecordSecond;
	private int currentCount = 15;
	private CountDownTimer recordTimer;
	private String sFilepath;
	private int desiredwidth = 480;
	private int desiredheight = 720;
	private List<Size> videosizes;
	private final int cMaxRecordDurationInMs = 30099;
	private final long cMaxFileSizeInBytes = 8000000;
	private final long cMaxFileSizeInBytesProfile = 52428800; //50MB
	private String categoryId,subCat;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final WindowManager.LayoutParams lp = this.getWindow().getAttributes();
		lp.screenBrightness = 1.0f;
		this.getWindow().setAttributes(lp);
		setContentView(R.layout.activity_record_video);
		mContext = RecordVideoActivity.this;
		getArgs();
		initControls();
		Utility.SendDataToGA("RecordVideo Screen", RecordVideoActivity.this);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

	private void getArgs(){
		final Bundle args = getIntent().getExtras();
		if(args != null) {
			if(args.containsKey("categoryId")) {
				categoryId = args.getString("categoryId");
				subCat = args.getString("subCat");
			}
		}
	}

	private void initControls() {

		layoutCameraOption = (LinearLayout) findViewById(R.id.layoutCameraOption);
		layoutCameraOption.setVisibility(View.VISIBLE);

		textRecordSecond = (TextView) findViewById(R.id.textRecordSecond);
		textRecordSecond.setVisibility(View.GONE);

		textRecordSecond.setText(currentCount + "");

		recordTimer = new CountDownTimer(19000, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				// TODO Auto-generated method stub

				// Log.v(Constant.TAG, "currentCount " + currentCount);

				if (currentCount == 1) {

					currentCount = 0;

					mPreview.recordVideo();
					textRecordSecond.setText("0");

				} else if ((millisUntilFinished / 1000) != 18) {

					if (currentCount != 0) {
						currentCount--;
						textRecordSecond.setText(currentCount + "");
					}
				}
			}

			@Override
			public void onFinish() {

			}
		};

		if (checkCameraHardware(mContext)) {

			CameraInfo cameraInfo = new CameraInfo();
			for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
					isFrontCameraAvailable = true;
				}
			}

			if (isFrontCameraAvailable) {
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
			}

			// Create an instance of Camera
			mCamera = CameraUtility.getVideoCameraInstance(currentCameraId);

			// Create our Preview view and set it as the content of our
			// activity.
			mPreview = new CameraPreview(this);
			mPreview.setCamera(mCamera);
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.addView(mPreview);

			btnRecord = (ImageView) findViewById(R.id.btnRecord);
			btnRecord.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
//                    mPreview.recordVideo();
                    if(mGravity != null && mGeomagnetic != null) {
                        float q = getDirection();
                        Log.d("qwe",q+"");
                    }
				}
			});

			btnChangeCamera = (ImageView) findViewById(R.id.btnChangeCamera);
			btnChangeCamera.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (isFrontCameraAvailable) {
						onCameraChange();
					}
				}
			});
			tvFlash = (TextView) findViewById(R.id.tvFlash);
			btnFlash = (LinearLayout) findViewById(R.id.btnFlash);
			ivFlash = (ImageView) findViewById(R.id.ivFlash);
			btnFlash.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub

					if (!isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {

					} else {
						if (isFlashOn) {
							isFlashOn = false;
							tvFlash.setText(getString(R.string.off));

						} else {
							isFlashOn = true;
							tvFlash.setText(getString(R.string.on));
						}

						mPreview.onOffFlash(isFlashOn);
					}

				}
			});
		}
	}

	private void onCameraChange() {
		try {
			mCamera.stopPreview();
			mCamera.release(); // release the camera for other
			// applications


			// swap the id of the camera to be used
			if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
			} else {
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
			}

			// Create an instance of Camera


			mCamera = CameraUtility.getVideoCameraInstance(currentCameraId);

			mPreview.setCamera(mCamera);

			mCamera.setPreviewDisplay(mPreview.mHolder);
			mCamera.startPreview();
			CameraUtility.setCameraDisplayOrientation((Activity) mContext, currentCameraId, mCamera);



		} catch (Exception e) {
			e.printStackTrace();
			// ignore: tried to stop a non-existent preview
		}
	}

	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

    private float getDirection()
    {

        float[] temp = new float[9];
        float[] R = new float[9];
        //Load rotation matrix into R
        SensorManager.getRotationMatrix(temp, null,
                mGravity, mGeomagnetic);

        //Remap to camera's point-of-view
        SensorManager.remapCoordinateSystem(temp,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z, R);

        //Return the orientation values
        float[] values = new float[3];
        SensorManager.getOrientation(R, values);

        //Convert to degrees
        for (int i=0; i < values.length; i++) {
            Double degrees = (values[i] * 180) / Math.PI;
            values[i] = degrees.floatValue();
        }

        return values[2];

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                mGravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = event.values.clone();
                break;
            default:
                return;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
		private SurfaceHolder mHolder;

		@SuppressWarnings("deprecation")
		public CameraPreview(Context context) {
			super(context);

		}

		private void onOffFlash(boolean isFlashOn) {

			final Camera.Parameters parameters = mCamera.getParameters();
			if (!isFlashOn) {
				parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
				mCamera.setParameters(parameters);
			} else {
				parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(parameters);
			}
		}

		private boolean prepareVideoRecorder() {
			if (mCamera == null)
				return false;
			try {
				mCamera.stopPreview();
				mCamera.setPreviewDisplay(null);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			mMediaRecorder = new MediaRecorder();

			// Step 1: Unlock and set camera to MediaRecorder
			mCamera.unlock();
			mMediaRecorder.setCamera(mCamera);

			// Step 2: Set sources
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);



			// // Step 3: Set a CamcorderProfile (requires API Level 8 or
			// higher)
			final CamcorderProfile profile = CamcorderProfile.get(currentCameraId, CamcorderProfile.QUALITY_HIGH);
			if (videosizes != null) {
				Size optimalVideoSize = getOptimalPreviewSize(videosizes, desiredwidth, desiredheight);
				profile.videoFrameWidth = optimalVideoSize.width;
				profile.videoFrameHeight = optimalVideoSize.height;
			}
			mMediaRecorder.setProfile(profile);

			// Log.v(Constant.TAG, "optimalVideoSize width " +
			// optimalVideoSize.width + " height " + optimalVideoSize.height);
			// mMediaRecorder.setVideoSize(optimalVideoSize.width,
			// optimalVideoSize.height);

			mMediaRecorder.setVideoEncodingBitRate(1280000);
			mMediaRecorder.setMaxDuration(cMaxRecordDurationInMs);
			if(categoryId.equalsIgnoreCase("7")){
				mMediaRecorder.setMaxFileSize(cMaxFileSizeInBytesProfile);
			}
			else{
				mMediaRecorder.setMaxFileSize(cMaxFileSizeInBytes);	
			}
			

			mMediaRecorder.setVideoFrameRate(30);

			sFilepath = Utility.getOutputMediaFile(Utility.MEDIA_TYPE_VIDEO,mContext).toString();

			// Step 4: Set output file
			mMediaRecorder.setOutputFile(sFilepath);

			// Step 5: Set the preview output
			mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

			if (currentCameraId == CameraInfo.CAMERA_FACING_BACK) {
				mMediaRecorder.setOrientationHint(90);
				//				Log.d("mobstar","setorientation 90");
			} else {
				mMediaRecorder.setOrientationHint(270);
				//				Log.d("mobstar","setorientation 270");
			}

			// Step 6: Prepare configured MediaRecorder
			try {
				mMediaRecorder.prepare();
			} catch (IllegalStateException e) {
				//				Log.d(Constant.TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
				releaseMediaRecorder();
				return false;
			} catch (IOException e) {
				//				Log.d(Constant.TAG, "IOException preparing MediaRecorder: " + e.getMessage());
				releaseMediaRecorder();
				return false;
			}
			return true;
		}

		private void recordVideo() {

			//			Log.v(Constant.TAG, "recordVideo " + isRecording);

			if (isRecording) {
				// stop recording and release camera
				try{
					mMediaRecorder.stop(); // stop the recording
				}
				catch(Exception e){
					e.printStackTrace();
				}

				releaseMediaRecorder(); // release the MediaRecorder object
				if (mCamera != null)
					mCamera.lock(); // take camera access back from MediaRecorder

				releaseCamera();

				isRecording = false;

				startApproveVideoActivity();

			} else {
				// initialize video camera
				if (prepareVideoRecorder()) {
					// Camera is available and unlocked, MediaRecorder is
					// prepared,
					// now you can start recording
                    try{
                        mMediaRecorder.start(); // stop the recording
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

					layoutCameraOption.setVisibility(View.GONE);
					textRecordSecond.setVisibility(View.VISIBLE);

					recordTimer.start();

					isRecording = true;
				} else {
					// prepare didn't work, release the camera
					releaseMediaRecorder();
					// inform user
				}
			}
		}

		private void setCamera(Camera camera) {
			mCamera = camera;
			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
				CameraUtility.setCameraDisplayOrientation((Activity) mContext, currentCameraId, mCamera);
			} catch (Exception e) {
				//				Log.d(Constant.TAG, "Error setting camera preview: " + e.getMessage());
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// empty. Take care of releasing the Camera preview in your
			// activity.
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				if(mCamera!=null){
					mCamera.stopPreview();

					Camera.Parameters parameters = mCamera.getParameters();
					Camera.Size size = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), width, height);

					if (size != null) {
						parameters.setPreviewSize(size.width, size.height);
						mCamera.setParameters(parameters);
						mCamera.startPreview();
					}

					Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

					if (display.getRotation() == Surface.ROTATION_270) {
						parameters.setPreviewSize(width, height);
					} else {

					}
					CameraUtility.setCameraDisplayOrientation((Activity) mContext, currentCameraId, mCamera);

					mCamera.setParameters(parameters);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
				//				Log.d(Constant.TAG, "Error starting camera preview: " + e.getMessage());
			}
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
		Camera.Size result = null;
		for (Camera.Size size : sizes) {

			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;
					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return (result);
	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			mCamera.lock(); // lock camera for later use
		}

		if (recordTimer != null) {
			recordTimer.cancel();
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		releaseMediaRecorder();
		releaseCamera();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
	}

	private void startApproveVideoActivity(){
		final Intent intent = new Intent(mContext, ApproveVideoActivity.class);
		intent.putExtra("video_path", sFilepath);
		intent.putExtra("categoryId", categoryId);
		if(subCat != null && subCat.length() > 0){
			intent.putExtra("subCat", subCat);
		}
		startActivity(intent);
		finish();
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
	}
}
