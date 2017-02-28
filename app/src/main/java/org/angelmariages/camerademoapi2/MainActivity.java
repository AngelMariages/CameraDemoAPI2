package org.angelmariages.camerademoapi2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Àngel Mariages - 06/02/2017
 */

public class MainActivity extends AppCompatActivity {

	private TextureView mTextureView;
	private String mCameraId;
	private CameraDevice.StateCallback mStateCallback;
	private CameraDevice mCameraDevice;
	private CaptureRequest.Builder mPreviewRequestBuilder;
	private CameraCaptureSession mCaptureSession;
	private int mCameraHeight;
	private int mCameraWidth;
	private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button captureButton = (Button) findViewById(R.id.captureButton);
		Button switchButton = (Button) findViewById(R.id.switchCamera);
		captureButton.setOnClickListener(view -> takePicture());
		switchButton.setOnClickListener(view -> switchCamera());
		mTextureView = (TextureView) findViewById(R.id.textureView);
		mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
				setupCamera();
				configureCameraStateCallback();
				//transformImage(width, height);
				openCamera();
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
				//transformImage(width, height);
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

	@Override
	protected void onPause() {
		super.onPause();
		/** Quan l'activity faci onPause tanquem la càmera
		 *  no necessitem obrirla a onResume perquè el surfaceTextureListener ja s'encarrega */
		if (mCameraDevice != null) {
			mCameraDevice.close();
			mCameraDevice = null;
		}
	}

	private void takePicture() {
		/** Si no hi ha càmera, sortim */
		if (mCameraDevice == null) {
			return;
		}

		try {
			/** Creem un {@link ImageReader} amb les dimensións que hem obtingut abans, amb el format JPEG i
			 *  un màxim d'una foto */
			ImageReader imageReader = ImageReader.newInstance(mCameraWidth, mCameraHeight, ImageFormat.JPEG, 1);

			/** Aquest imageReader té un surfaceView al qual li passem la imatge de la càmera */
			Surface imageReaderSurface = imageReader.getSurface();

			/** Creem una petició de captura a la càmera */
			final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			/** El target aquest cop serà el imageReaderSurface en comptes del TextureView */
			captureBuilder.addTarget(imageReaderSurface);
			/** Tornem a deixar que la càmera faci el millor que pugui amb la foto*/
			captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_MODE_AUTO);
			CameraManager manager = (CameraManager) MainActivity.this.getSystemService(Context.CAMERA_SERVICE);
			CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics("" + mCameraDevice.getId());
			int rotation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
			captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

			ImageReader.OnImageAvailableListener imageAvailableListener = imageReader1 -> {
                /** Obtenim la imatge del imageReader */
                Image image = imageReader1.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                /** Convertim el buffer de bytes a un array de bytes */
                buffer.get(bytes);
                /** Guardem la foto */
                savePicture(bytes);
                image.close();
            };

			imageReader.setOnImageAvailableListener(imageAvailableListener, null);

			final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
					super.onCaptureStarted(session, request, timestamp, frameNumber);
				}

				@Override
				public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
					super.onCaptureCompleted(session, request, result);
					/** Un cop la captura s'hagi completat, tornem a configurar la preview de la càmera*/
					setupCameraPreview();
				}
			};

			mCameraDevice.createCaptureSession(Collections.singletonList(imageReaderSurface), new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					try {
						/** Intentem crear una captura que gestionara el {@link captureCallback}*/
						cameraCaptureSession.capture(captureBuilder.build(), captureCallback, null);
					} catch (CameraAccessException e) {
						Log.d("TAG", "Error creating the capture");
						Log.d("TAG", e.getMessage());
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

				}
			}, null);
		} catch (CameraAccessException e) {
			Log.d("TAG", "Error taking the picture");
			Log.d("TAG", e.getMessage());
		}
	}

	private void savePicture(byte[] bytes) {
		File mediaFileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File mediaFile = new File(mediaFileDir.getPath() + "/" + System.currentTimeMillis() + "_Test.jpg");
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(mediaFile);
			fileOutputStream.write(bytes);
			fileOutputStream.close();

			Toast.makeText(MainActivity.this, "Foto guardada", Toast.LENGTH_SHORT).show();

			/** La galeria no pot escanejar el nostre directori així que li diem explicitament que ho faci */
			ContentValues values = new ContentValues();

			values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
			values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
			values.put(MediaStore.MediaColumns.DATA, mediaFile.getAbsolutePath());

			MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		} catch (IOException e) {
			if (e.getMessage().contains("EACCES")) {
				startActivity(new Intent(this, RequestPermissionsActivity.class));
			} else {
				Log.d("TAG", "Can't save file");
				Log.d("TAG", e.getMessage());
			}
		}
	}

	private void setupCameraPreview() {
		SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
		/** Si no hi ha surface, sortim */
		if (surfaceTexture == null || mCameraDevice == null) {
			return;
		}
		Surface surface = new Surface(surfaceTexture);

		try {
			/** Creem una petició de previsualització amb el "target", la surface dins del textureView */
			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(surface);

			mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
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

						/** Creem una captura "repetitiva" que es mostrara al surface */
						mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
					} catch (CameraAccessException e) {
						Log.d("TAG", "Error creating the preview session");
						Log.d("TAG", e.getMessage());
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

				}
			}, null);
		} catch (CameraAccessException e) {
			Log.d("TAG", "Error setting up the camera preview");
			Log.d("TAG", e.getMessage());
		}
	}

	private void setupCamera() {
		/**Servei de càmeres del sistema per obtenir totes les càmeres disponibles*/
		CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

		try {
			for (String cameraId : cameraManager.getCameraIdList()) {
				CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
				/**Si la càmera no és la que esta enfocant al costat que volem saltem a la següent*/
				Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
				if (lensFacing != null && lensFacing != mCameraFacing) {
					/** Només volem la càmera que enfoca cap al costat que hem seleccionat */
					continue;
				}
				/**Agafem la id de la càmera que volem*/
				mCameraId = cameraId;
				StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if (streamConfigurationMap == null) {
					throw new RuntimeException("Can't get camera sizes");
				}

				Size[] outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

				/** Guardem les dimensions que pot oferir la càmera*/
				mCameraWidth = outputSizes[0].getWidth();
				mCameraHeight = outputSizes[0].getHeight();
			}
		} catch (CameraAccessException e) {
			Log.d("TAG", "Error setting up the camera");
			Log.d("TAG", e.getMessage());
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
				Log.d("TAG", "cameraDevice error num: " + i);
				mCameraDevice = null;
			}
		};
	}

	@SuppressWarnings("MissingPermission")
	private void openCamera() {
		/** Servei de càmeres del sistema per obtenir totes les càmeres disponibles*/
		CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

		try {
			/** Obrim la càmera amb l'id que hem seleccionat fent servir -> {@link #setupCamera()}
			 *  el {@link #configureCameraStateCallback()} ens permet tenir control sobre l'estat de la càmera */
			cameraManager.openCamera(mCameraId, mStateCallback, null);
		} catch (CameraAccessException e) {
			Log.d("TAG", "Can't open camera");
			Log.d("TAG", e.getMessage());
		} catch (SecurityException e) {
			startActivity(new Intent(this, RequestPermissionsActivity.class));
		}
	}

	private void switchCamera() {
		if (mCameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
			mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
		} else {
			mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
		}

		mCameraDevice.close();
		mCameraDevice = null;

		setupCamera();
		configureCameraStateCallback();
		openCamera();
	}

	private void transformImage(int width, int height) {
		if(mTextureView == null) {
			return;
		}
		Matrix matrix = new Matrix();
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		RectF textureRectF = new RectF(0, 0, width, height);
		RectF previewRectF = new RectF(0, 0, mCameraHeight, mCameraWidth);
		float centerX = textureRectF.centerX();
		float centerY = textureRectF.centerY();
		if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
			previewRectF.offset(centerX - previewRectF.centerX(),
					centerY - previewRectF.centerY());
			matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
			float scale = Math.max((float)width / mCameraWidth,
					(float)height / mCameraHeight);
			matrix.postScale(scale, scale, centerX, centerY);
			matrix.postRotate(90 * (rotation - 2), centerX, centerY);
		}
		mTextureView.setTransform(matrix);
	}
}
