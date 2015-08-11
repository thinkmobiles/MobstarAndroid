package com.mobstar.home.split;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.edmodo.cropper.CropImageView;
import com.mobstar.R;
import com.mobstar.home.split.position_variants.PositionVariant;
import com.mobstar.utils.Constant;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by vasia on 06.08.15.
 */
public class CropVideoFragment extends Fragment implements View.OnClickListener {

    private Button btnNext, btnBack;
    private CropImageView ivVideoImage;
    private SplitActivity mSplitActivity;
    private String mVideoThumb;
    private PositionVariant mPositionVariant;
    private ProgressBar progress;

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
        btnBack       = (Button) _view.findViewById(R.id.btnBack);
        btnNext       = (Button) _view.findViewById(R.id.btnNext);
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
                mSplitActivity.replaceRecordVideoFragment();
                break;
        }
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

        int width = bm.getWidth();
        int height = bm.getHeight();
        final Display display = mSplitActivity.getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        int newWidth = size.x;
        int newHeight = newWidth * height / width;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        final Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

}
