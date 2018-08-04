package com.burnweb.rnwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.ValueCallback;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.common.annotations.VisibleForTesting;

public class RNWebViewModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    @VisibleForTesting
    public static final String REACT_CLASS = "RNWebViewAndroidModule";
    protected static final int REQUEST_CODE_FILE_PICKER = 51426;

    private RNWebViewPackage aPackage;

    /* FOR UPLOAD DIALOG */
    private final static int REQUEST_SELECT_FILE = 1001;
    private final static int REQUEST_SELECT_FILE_LEGACY = 1002;

    private ValueCallback<Uri> mUploadMessage = null;
    private ValueCallback<Uri[]> mUploadMessageArr = null;

    public RNWebViewModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public void setPackage(RNWebViewPackage aPackage) {
        this.aPackage = aPackage;
    }

    public RNWebViewPackage getPackage() {
        return this.aPackage;
    }

    @SuppressWarnings("unused")
    public Activity getActivity() {
        return getCurrentActivity();
    }

    public void showAlert(String url, String message, final JsResult result) {
        AlertDialog ad = new AlertDialog.Builder(getCurrentActivity())
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                })
                .create();

        ad.show();
    }

    // For Android 4.1+
    @SuppressWarnings("unused")
    public boolean startFileChooserIntent(ValueCallback<Uri> uploadMsg, String acceptType,final boolean allowMultiple) {
        Log.d(REACT_CLASS, "Open old file dialog");

        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
            mUploadMessage = null;
        }

        mUploadMessage = uploadMsg;

        if(acceptType == null || acceptType.isEmpty()) {
            acceptType = "*/*";
        }

        Intent intentChoose = new Intent(Intent.ACTION_GET_CONTENT);
        intentChoose.addCategory(Intent.CATEGORY_OPENABLE);
        intentChoose.setType(acceptType);
        if (allowMultiple) {
            if (Build.VERSION.SDK_INT >= 18) {
                intentChoose.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
        }
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.w(REACT_CLASS, "No context available");
            return false;
        }

        try {
            currentActivity.startActivityForResult(intentChoose, REQUEST_SELECT_FILE_LEGACY, new Bundle());
        } catch (ActivityNotFoundException e) {
            Log.e(REACT_CLASS, "No context available");
            e.printStackTrace();

            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            }
            return false;
        }

        return true;
    }

    // For Android 5.0+
    @SuppressLint("NewApi")
    public boolean startFileChooserIntent(ValueCallback<Uri[]> filePathCallback, Intent intentChoose,final boolean allowMultiple) {
        Log.d(REACT_CLASS, "Open new file dialog");

        if (mUploadMessageArr != null) {
            mUploadMessageArr.onReceiveValue(null);
            mUploadMessageArr = null;
        }

        mUploadMessageArr = filePathCallback;

        Activity currentActivity = getCurrentActivity();
        if (allowMultiple) {
            if (Build.VERSION.SDK_INT >= 18) {
                intentChoose.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
        }
        if (currentActivity == null) {
            Log.w(REACT_CLASS, "No context available");
            return false;
        }

        try {
            currentActivity.startActivityForResult(intentChoose, REQUEST_SELECT_FILE, new Bundle());
        } catch (ActivityNotFoundException e) {
            Log.e(REACT_CLASS, "No context available");
            e.printStackTrace();

            if (mUploadMessageArr != null) {
                mUploadMessageArr.onReceiveValue(null);
                mUploadMessageArr = null;
            }
            return false;
        }

        return true;
    }

    @SuppressLint({"NewApi", "Deprecated"})
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        /*
        if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    if (mUploadMessageArr != null) {
                        Uri[] dataUris = null;

                        try {
                            if (intent.getDataString() != null) {
                                dataUris = new Uri[] { Uri.parse(intent.getDataString()) };
                            }
                            else {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    if (intent.getClipData() != null) {
                                        final int numSelectedFiles = intent.getClipData().getItemCount();

                                        dataUris = new Uri[numSelectedFiles];

                                        for (int i = 0; i < numSelectedFiles; i++) {
                                            dataUris[i] = intent.getClipData().getItemAt(i).getUri();
                                        }
                                    }
                                }
                            }
                        }
                        catch (Exception ignored) { }

                        mUploadMessageArr.onReceiveValue(dataUris);
                        mUploadMessageArr = null;
                    }
                }
            }
            else {
                if (mUploadMessageArr != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessageArr = null;
                }
            }
        }
        */
        if (requestCode == REQUEST_SELECT_FILE_LEGACY) {
            Log.i("pp","p1");
            if (mUploadMessage == null) return;

            Uri result = ((intent == null || resultCode != Activity.RESULT_OK) ? null : intent.getData());

            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else if (requestCode == REQUEST_SELECT_FILE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i("pp","p2");
            if (mUploadMessageArr == null) return;
            Uri[] dataUris = null;

            try {
                if (intent.getDataString() != null) {
                    dataUris = new Uri[] { Uri.parse(intent.getDataString()) };
                }
                else {
                    if (Build.VERSION.SDK_INT >= 16) {
                        if (intent.getClipData() != null) {
                            final int numSelectedFiles = intent.getClipData().getItemCount();

                            dataUris = new Uri[numSelectedFiles];

                            for (int i = 0; i < numSelectedFiles; i++) {
                                dataUris[i] = intent.getClipData().getItemAt(i).getUri();
                            }
                        }
                    }
                }
            }
            catch (Exception ignored) { }

            mUploadMessageArr.onReceiveValue(dataUris);
            mUploadMessageArr = null;
//            mUploadMessageArr.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
//            mUploadMessageArr = null;
        }

    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        this.onActivityResult(requestCode, resultCode, data);
    }

    public void onNewIntent(Intent intent) {}

}
