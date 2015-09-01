package com.mobstar.home.split;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.edmodo.cropper.CropImageView;
import com.mobstar.R;
import com.mobstar.custom.CustomTextviewBold;
import com.mobstar.home.split.ffmpeg.AfterDoneBackground;
import com.mobstar.home.split.ffmpeg.CropBackground;
import com.mobstar.home.split.position_variants.PositionVariant;
import com.mobstar.utils.Constant;
import com.mobstar.utils.Utility;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;

/**
 * Created by vasia on 06.08.15.
 */
public class CropVideoFragment extends Fragment implements View.OnClickListener {

    private CustomTextviewBold btnNext, btnBack;
    private CropImageView ivVideoImage;
    private SplitActivity mSplitActivity;
    private String mVideoThumb;
    private PositionVariant mPositionVariant;
    private ProgressBar progress;
    private Point fullScreenImageSize;

    private String tempRotateFilePth;

    public static CropVideoFragment newInstance(final String videoThumb, PositionVariant positionVariant){
        final CropVideoFragment fragment = new CropVideoFragment();
        Bundle args = new Bundle();
        args.putString(Constant.IMAGE, videoThumb);
        args.putSerializable(Constant.POSITION_VARIANT, positionVariant);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSplitActivity = (SplitActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (args != null) {
            mVideoThumb = args.getString(Constant.IMAGE);
            mPositionVariant = (PositionVariant) args.getSerializable(Constant.POSITION_VARIANT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.fragment_crop_video, container, false);
        findViews(inflatedView);
        setListeners();
        setupVideoImage();
        return inflatedView;
    }

    private void findViews(final View _view){
        btnBack       = (CustomTextviewBold) _view.findViewById(R.id.btnBack);
        btnNext       = (CustomTextviewBold) _view.findViewById(R.id.btnNext);
        ivVideoImage  = (CropImageView) _view.findViewById(R.id.ivCropImage);
        progress      = (ProgressBar) _view.findViewById(R.id.progressbar);
    }

    private void setListeners(){
        btnBack.setOnClickListener(this);
        btnNext.setOnClickListener(this);
    }

    private void setupVideoImage(){
        if (mVideoThumb != null)
        Picasso.with(mSplitActivity).load(mVideoThumb)
                .placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                progress.setVisibility(View.GONE);
                ivVideoImage.setImageBitmap(getResizedBitmap(bitmap));
                setupStartCropFrame();
            }

            @Override
            public void onBitmapFailed(Drawable drawable) {
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onPrepareLoad(Drawable drawable) {
                progress.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnBack:
                mSplitActivity.onBackPressed();
                break;
            case R.id.btnNext:
                onClickNext();
                break;
        }
    }

    private void onClickNext(){
        createComplexVideoCommand();
    }

    private int getVideoRotation(final String _videoFilePath){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(_videoFilePath);
        String orientationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (orientationStr == null)
            return 0;
        int rotation = 0;
        switch (Integer.parseInt(orientationStr)) {
            case 0:
                rotation = 0;
                break;
            case 90:
                rotation = 1;
                break;
            case 270:
                rotation = 2;
                break;
        }
        return rotation;
    }

    private void createComplexVideoCommand(){
        final String fileInPath = mSplitActivity.getVideoFilePath();
        final String fileOutPath = Utility.getTemporaryMediaFile(mSplitActivity, "cropUot").toString();
        final Rect rect = getCropRect();
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("ffmpeg -y -i ")
                .append(fileInPath)
                .append(" -strict experimental -vf crop=")
                .append(rect.right - rect.left - 1)
                .append(":")
                .append(rect.bottom - rect.top - 1)
                .append(":")
                .append(rect.left)
                .append(":")
                .append(rect.top);
        final int rotation = getVideoRotation(mSplitActivity.getVideoFilePath());
        if (rotation != 0){
            stringBuilder
                    .append(",transpose=")
                    .append(rotation);
        }
        stringBuilder
                .append(" -s ")
                .append(getOutputVideoSizeString())
                .append(" -metadata:s:v rotate=0")
                .append(" -vcodec mpeg4 ")
                .append(fileOutPath);
        final String complexCommand = stringBuilder.toString();
        startCropTask(complexCommand, fileOutPath);
    }

    private void startCropTask(final String _stringCommand, final String _fileOutPath){
        new CropBackground(mSplitActivity, _stringCommand, new AfterDoneBackground() {
            @Override
            public void onAfterDone() {
                if (tempRotateFilePth != null)
                    removeFile(tempRotateFilePth);
                mSplitActivity.setVideoFilePath(_fileOutPath);
                mSplitActivity.replaceRecordVideoFragment(mPositionVariant, ivVideoImage.getCroppedImage());
            }

            @Override
            public void onCancel() {
                if (tempRotateFilePth != null)
                    removeFile(tempRotateFilePth);
                removeFile(_fileOutPath);
                mSplitActivity.setDefaultFilePath();
            }
        }).runTranscoding();
    }

    private String getOutputVideoSizeString(){
        String outSize = "";
        switch (mPositionVariant){
            case ORIGIN_LEFT:
            case ORIGIN_RIGHT:
                outSize = "154x308";
                break;
            case ORIGIN_RIGHT_TOP:
                outSize = "100x100";
                break;
            case ORIGIN_FULLSCREEN:
                outSize = "308x308";
                break;
            case ORIGIN_TOP:
            case ORIGIN_BOTTOM:
                outSize = "308x154";
                break;
        }
        return outSize;
    }

    private Rect getCropRect(){
        RectF rectF = ivVideoImage.getActualCropRect();
        Rect originRect = new Rect();
        float ratio = ((float) Constant.VIDEO_SIZE) / fullScreenImageSize.x;
        originRect.set(
                (int) (rectF.left * ratio),
                (int) (rectF.top * ratio),
                (int) (rectF.right * ratio),
                (int) (rectF.bottom * ratio)
        );
        return originRect;
    }


    private void setupStartCropFrame(){
        switch (mPositionVariant){
            case ORIGIN_LEFT:
            case ORIGIN_RIGHT:
                onOriginVertical();
                break;
            case ORIGIN_RIGHT_TOP:
            case ORIGIN_FULLSCREEN:
                onOriginSquare();
                break;
            case ORIGIN_TOP:
            case ORIGIN_BOTTOM:
                onOriginHorizontal();
                break;
        }
        ivVideoImage.setFixedAspectRatio(true);
    }

    private void onOriginVertical(){
        ivVideoImage.setAspectRatio(50, 100);
    }

    private void onOriginSquare(){
        ivVideoImage.setAspectRatio(CropImageView.DEFAULT_ASPECT_RATIO_X, CropImageView.DEFAULT_ASPECT_RATIO_Y);
    }

    private void onOriginHorizontal(){
        ivVideoImage.setAspectRatio(100, 50);
    }

    public Bitmap getResizedBitmap(Bitmap bm) {
        fullScreenImageSize = new Point();
        int width = bm.getWidth();
        int height = bm.getHeight();
        final Display display = mSplitActivity.getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        int newWidth = size.x;
        int newHeight = newWidth * height / width;
        fullScreenImageSize.set(newWidth, newHeight);

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        final Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    private void removeFile(String filePath){
        final File file = new File(filePath);
        file.delete();
    }
}
