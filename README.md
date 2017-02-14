# CameraDemoAPI2

A demo app showing how to do a simple preview and save pictures with the [android.hardware.camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) and take pictures with it.
###### It doesn't use handlers for simplicity

## Pictures
<img src="https://raw.githubusercontent.com/AngelMariages/CameraDemoAPI2/master/Screenshot_1487092104.png" width="250">

## Summary
1. First of all we need the permissions to acces the camera and write to the external storage for saving pictures:
```xml
	<uses-permission android:name="android.permission.CAMERA" />
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
> We use the Activity [RequestPermissionsActivity.java](https://github.com/AngelMariages/CameraDemoAPI2/blob/master/app/src/main/java/org/angelmariages/camerademoapi2/RequestPermissionsActivity.java) to manage all the permission requests

2. Then we will need a TextureView to display the preview, to setup the preview we wil wait until the surface is available:
```java
mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
		setupCamera();
		configureCameraStateCallback();
		openCamera();
	}
  ...
```

3. The setup camera method is for getting the id of the camera we want to use, we also save the camera size to correctly save the picture:
```java
CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

try {
	for (String cameraId : cameraManager.getCameraIdList()) {
		CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
		/**We only get the camera that's facing the direction that we want*/
		Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
		if (lensFacing != null && lensFacing != mCameraFacing) {
			continue;
		}
		/**Save the camera id*/
		mCameraId = cameraId;
		StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		if (streamConfigurationMap == null) {
			throw new RuntimeException("Can't get camera sizes");
		}

		Size[] outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

		/** Save the camera dimensions*/
		mCameraWidth = outputSizes[0].getWidth();
		mCameraHeight = outputSizes[0].getHeight();
	}
}
```

4. The CameraDevice.StateCallback() controls the state of the current camera device:
```java
mStateCallback = new CameraDevice.StateCallback() {
	@Override
	public void onOpened(@NonNull CameraDevice cameraDevice) {
		/** Only save the camera device if it's correctly opened*/
		mCameraDevice = cameraDevice;
		/** Config the camera preview */
		setupCameraPreview();
	}

	@Override
	public void onDisconnected(@NonNull CameraDevice cameraDevice) {
		Log.d("TAG", "cameraDevice disconnected");
		mCameraDevice = null;
	}

	@Override
	public void onError(@NonNull CameraDevice cameraDevice, int i) {
		Log.d("TAG", "cameraDevice error num: " + i);
		mCameraDevice = null;
	}
	};
```

5. After we know that the camera has been opened we prepare the preview:
```java
Surface surface = new Surface(surfaceTexture);

try {
	/** We set the capture target to the surface "inside" the TextureView */
	mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
	mPreviewRequestBuilder.addTarget(surface);
```

6. ... and then we create the captureSession to show the preview:
```java
mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
	@Override
	public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
		mCaptureSession = cameraCaptureSession;
		try {
			/** Set the camera mode to auto*/
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

			mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}
	...
```

7. The above will only happen if we open the actual camera, to do this we call the method openCamera() with the callbacks we created
```java
CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
	try {
		/** We open the camera selected with setupCamera()
		 *  and the mStateCallback allows us to control the opening of the camera as mentioned above */
		cameraManager.openCamera(mCameraId, mStateCallback, null);
```

***
## And that's all for the basic setup of the preview of the camera, if you want to know how to take pictures and switch from the back facing camera to the front one, try the code for yourself and experiment with it!
