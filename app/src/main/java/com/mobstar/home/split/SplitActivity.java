package com.mobstar.home.split;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.mobstar.R;
import com.mobstar.api.DownloadFileManager;
import com.mobstar.home.split.position_variants.PositionVariant;
import com.mobstar.home.split.position_variants.PositionVariantsFragment;
import com.mobstar.pojo.EntryPojo;
import com.mobstar.utils.Utility;

import cz.msebera.android.httpclient.Header;

import java.io.File;

/**
 * Created by vasia on 06.08.15.
 */
public class SplitActivity extends Activity implements DownloadFileManager.DownloadCallback {

    public static final String ENTRY_SPLIT = "entry split";

    private EntryPojo entry;
    private String videoFilePath;
    private OnDownloadFileCompletedListener onDownloadFileCompletedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split);

        if (getIntent() != null)
            entry = (EntryPojo) getIntent().getSerializableExtra(ENTRY_SPLIT);
        if (videoFilePath == null) {
//            setDefaultFilePath();
            downloadVideo();
        }

        if (savedInstanceState == null)
            replaceTopNavigationFragment(new PositionVariantsFragment());
//             replaceTopNavigationFragment(new RecordSplitVideoFragment());
    }


    public void setDefaultFilePath(){
        final String fileName = Utility.GetFileNameFromURl(entry.getVideoLink());
        final String filePath = Utility.getCurrentDirectory(this);
        videoFilePath = filePath + fileName;
    }

    @Override
    public void onBackPressed() {
        final int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public final void replaceFragmentWithBackStack(final Fragment _fragment) {
        getFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragmentContainer, _fragment)
                .addToBackStack(null)
                .commit();

    }

    public final void replaceTopNavigationFragment(final Fragment _fragment) {
        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, _fragment)
                .commit();
    }

    public void replaceCropVideoFragment(final PositionVariant _positionVariant){
        replaceFragmentWithBackStack(CropVideoFragment.newInstance(entry.getVideoThumb(), _positionVariant));
    }

    public void replaceRecordVideoFragment(final PositionVariant _positionVariant, final Bitmap _imagePreview){
        replaceTopNavigationFragment(RecordSplitVideoFragment.newInstance(_positionVariant, _imagePreview));
    }

    public String getVideoFilePath(){
        return videoFilePath;
    }

    public void setVideoFilePath(final String _filePath){
        videoFilePath = _filePath;
    }

    public EntryPojo getEntry(){
        return entry;
    }

    public void setOnDownloadFileCompletedListener(OnDownloadFileCompletedListener _onDownloadFileCompletedListener){
        onDownloadFileCompletedListener = _onDownloadFileCompletedListener;
    }

    public void downloadVideo() {
        DownloadFileManager downloadFileManager = new DownloadFileManager(this, this);
        downloadFileManager.downloadFile(entry.getVideoLink(), 0);
//        final String sFileName = Utility.GetFileNameFromURl(entry.getVideoLink());
//        final String filePath = Utility.getCurrentDirectory(this);
//        try {
//            final File file = new File(filePath + sFileName);
//
//            if (file != null && !file.exists()) {
//
//                if (Utility.isNetworkAvailable(this)) {
//                    AsyncHttpClient client = new AsyncHttpClient();
//                    final int DEFAULT_TIMEOUT = 60 * 1000;
//
//                    client.setTimeout(DEFAULT_TIMEOUT);
//                    client.get(entry.getVideoLink(), new FileAsyncHttpResponseHandler(file) {
//
//                        @Override
//                        public void onFailure(int arg0, Header[] arg1, Throwable arg2, File file) {
////										Log.d("mobstar","Download fail video=>"+arrEntryPojos.get(position).getVideoLink());
//                            if (onDownloadFileCompletedListener != null)
//                                onDownloadFileCompletedListener.onFailed();
//
//                        }
//
//                        @Override
//                        public void onSuccess(int arg0, Header[] arg1, File file) {
//                            videoFilePath = filePath + sFileName;
//                            if (onDownloadFileCompletedListener != null)
//                                onDownloadFileCompletedListener.onCompleted(videoFilePath);
//                        }
//
//                    });
//                } else {
//                    Toast.makeText(this, getString(R.string.no_internet_access), Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                videoFilePath = filePath + sFileName;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void onDownload(String filePath, int position) {
        videoFilePath = filePath;
        if (onDownloadFileCompletedListener != null)
            onDownloadFileCompletedListener.onCompleted(videoFilePath);
    }

    @Override
    public void onFailed() {
        if (onDownloadFileCompletedListener != null)
            onDownloadFileCompletedListener.onFailed();
    }
}
