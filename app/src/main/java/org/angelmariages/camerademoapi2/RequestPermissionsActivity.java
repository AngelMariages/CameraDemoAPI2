package org.angelmariages.camerademoapi2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Àngel Mariages - 06/02/2017
 */

public class RequestPermissionsActivity extends AppCompatActivity {
	private static final int REQUEST_PERMISSIONS_CODE = 200;
	private static final int REQUEST_PERMISSIONS_CAMERA_CODE = 300;
	private static final int REQUEST_PERMISSIONS_WRITE_CODE = 400;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.request_permissions_layout);
		Button grantPermissionsButton = (Button) findViewById(R.id.button);
		grantPermissionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				checkPermissions(RequestPermissionsActivity.this);
			}
		});

		if (checkPermissions(this)) {
			startActivity(new Intent(this, MainActivity.class));
		}
	}

	public static boolean checkPermissions(Activity activity) {
		int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
		int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (cameraPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED) {
			return true;
		} else {
			if (writePermission == PackageManager.PERMISSION_DENIED && cameraPermission == PackageManager.PERMISSION_DENIED) {
				ActivityCompat.requestPermissions(activity, new String[]{
						Manifest.permission.CAMERA,
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				}, REQUEST_PERMISSIONS_CODE);
			} else if (writePermission == PackageManager.PERMISSION_DENIED) {
				ActivityCompat.requestPermissions(activity, new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				}, REQUEST_PERMISSIONS_WRITE_CODE);
			} else if (cameraPermission == PackageManager.PERMISSION_DENIED) {
				ActivityCompat.requestPermissions(activity, new String[]{
						Manifest.permission.CAMERA
				}, REQUEST_PERMISSIONS_CAMERA_CODE);
			}
			return false;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_PERMISSIONS_CODE: {
				if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					startActivity(new Intent(this, MainActivity.class));
				}
			} break;
			case REQUEST_PERMISSIONS_WRITE_CODE: {
				if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startActivity(new Intent(this, MainActivity.class));
				}
			} break;
			case REQUEST_PERMISSIONS_CAMERA_CODE: {
				if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startActivity(new Intent(this, MainActivity.class));
				}
			} break;
		}
	}
}
