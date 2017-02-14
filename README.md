# CameraDemoAPI2

A demo app showing how to do a simple preview and save pictures with the [android.hardware.camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) and take pictures with it

## Summary
* First of all we need the permissions to acces the camera and write to the external storage for saving pictures
```xml
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
> We use the Activity [RequestPermissionsActivity.java](angelmariages/CameraDemoAPI2/app/src/main/java/org/angelmariages/camerademoapi2/RequestPermissionsActivity.java)
