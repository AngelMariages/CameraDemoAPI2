package org.angelmariages.camerademoapi2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

	private TextureView mTextureView;
	private String mCameraId;
	private CameraDevice.StateCallback mStateCallback;
	private CameraDevice mCameraDevice;
	private CaptureRequest.Builder mPreviewRequestBuilder;
	private CameraCaptureSession mCaptureSession;
	private int mCameraHeight;
	private int mCameraWidth;
	private Surface mSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button captureButton = (Button) findViewById(R.id.captureButton);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				takePicture();
			}
		});
		mTextureView = (TextureView) findViewById(R.id.textureView);
		mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
				setupCamera();
				configureCameraStateCallback();
				openCamera();
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
				return false;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

			}
		});
	}

	private void takePicture() {
		if (mCameraDevice == null) {
			return;
		}

		try {
			ImageReader imageReader = ImageReader.newInstance(mCameraWidth, mCameraHeight, ImageFormat.JPEG,1);
			ArrayList<Surface> outputSurfaces = new ArrayList<>(2);

			Surface imageReaderSurface = imageReader.getSurface();

			outputSurfaces.add(imageReaderSurface);
			outputSurfaces.add(mSurface);

			final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(imageReaderSurface);
			captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

			HandlerThread handlerThread = new HandlerThread("TakePictureThread");
			handlerThread.start();
			final Handler handler = new Handler(handlerThread.getLooper());

			ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
				@Override
				public void onImageAvailable(ImageReader imageReader) {
					Image image = imageReader.acquireLatestImage();
					ByteBuffer buffer = image.getPlanes()[0].getBuffer();
					byte[] bytes = new byte[buffer.capacity()];
					buffer.get(bytes);
					savePicture(bytes);
					image.close();
				}
			};

			imageReader.setOnImageAvailableListener(imageAvailableListener, handler);

			final CameraCaptureSession.CaptureCallback previewSSession = new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
					super.onCaptureStarted(session, request, timestamp, frameNumber);
				}

				@Override
				public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
					super.onCaptureCompleted(session, request, result);
					setupCameraPreview();
				}
			};

			mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					try {
						cameraCaptureSession.capture(captureBuilder.build(), previewSSession, handler);
					} catch (CameraAccessException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

				}
			}, handler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private void savePicture(byte[] bytes) {
		File mediaFileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Test");
		File mediaFile = new File(mediaFileDir.getPath() + File.pathSeparatorChar + System.currentTimeMillis());
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(mediaFile);
			fileOutputStream.write(bytes);
			fileOutputStream.flush();
			fileOutputStream.close();
			System.out.println("what");
			System.out.println(Arrays.toString(bytes));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupCameraPreview() {
		SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
		/** Si no hi ha surface, sortim */
		if (surfaceTexture == null) {
			return;
		}
		mSurface = new Surface(surfaceTexture);

		try {
			/** Creem una petició de previsualització amb el "target", la surface dins del textureView */
			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(mSurface);

			mCameraDevice.createCaptureSession(Collections.singletonList(mSurface), new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					/** Si no hi ha càmera, sortim */
					if (mCameraDevice == null) {
						return;
					}

					mCaptureSession = cameraCaptureSession;
					try {
						/** Deixem que la càmera ajusti automàticament la millor visualització*/
						mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

						/** Creem una captura "repetitiva" que es mostrara al {@link mSurface} */
						mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
					} catch (CameraAccessException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

				}
			}, null);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private void setupCamera() {
		/**Servei de càmeres del sistema per obtenir totes les càmeres disponibles*/
		CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

		try {
			for (String cameraId : cameraManager.getCameraIdList()) {
				CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
				/**Si la càmera no és la que esta enfocant al darrere saltem a la següent*/
				Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
				if (lensFacing != null && lensFacing != CameraCharacteristics.LENS_FACING_BACK) {
					/** Només volem la càmera que enfoca cap al darrere */
					continue;
				}
				/**Agafem la id de la càmera que volem*/
				mCameraId = cameraId;
				StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if(streamConfigurationMap == null) {
					throw new RuntimeException("Can't get camera sizes");
				}

				Size[] outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

				/** Guardem les dimensions que pot oferir la càmera*/
				mCameraWidth = outputSizes[0].getWidth();
				mCameraHeight = outputSizes[0].getHeight();
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private void configureCameraStateCallback() {
		mStateCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened(@NonNull CameraDevice cameraDevice) {
				/** Quan sabem que la camera ha estat oberta correctament la guardem*/
				mCameraDevice = cameraDevice;
				/** Configurem la previsualització de la càmera */
				setupCameraPreview();
			}

			@Override
			public void onDisconnected(@NonNull CameraDevice cameraDevice) {
				Log.d("TAG", "cameraDevice disconnected");
				mCameraDevice = null;
			}

			@Override
			public void onError(@NonNull CameraDevice cameraDevice, int i) {
				Log.d("TAG", "cameraDevice error");
				mCameraDevice = null;
			}
		};
	}

	private void openCamera() {
		/** Servei de càmeres del sistema per obtenir totes les càmeres disponibles*/
		CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

		// TODO: 2/9/17 Comprovar permissos

		try {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return;
			}

			/** Obrim la càmera amb l'id que hem seleccionat fent servir -> {@link #setupCamera()}
			 *  el {@link #configureCameraStateCallback()} ens permet tenir control sobre l'estat de la càmera */
			cameraManager.openCamera(mCameraId, mStateCallback, null);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}
}
