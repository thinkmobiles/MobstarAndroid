package com.mobstar.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.mobstar.R;
import com.mobstar.utils.Utility;

public class SelectMediaTypeActivity extends Activity implements OnClickListener {

	Context mContext;

	ImageView btnBack, btnImage, btnAudioClip, btnMovieClip;

	Typeface typefaceBtn;

	String categoryId,subCat;
	
	private boolean isModelType=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_select_media_type);

		mContext = SelectMediaTypeActivity.this;

		Bundle b=getIntent().getExtras();
		if(b!=null) {
			if(b.containsKey("categoryId")) {
				categoryId=b.getString("categoryId");
				subCat=b.getString("subCat");
			}
		}

		InitControls();

		Utility.SendDataToGA("SelectMediaType Screen", SelectMediaTypeActivity.this);
	}

	void InitControls() {

		typefaceBtn = Typeface.createFromAsset(getAssets(), "GOTHAM-BOLD.TTF");

		btnBack = (ImageView) findViewById(R.id.btnBack);

		btnBack.setOnClickListener(this);

		btnImage = (ImageView) findViewById(R.id.btnImage);
		btnImage.setOnClickListener(this);

		btnAudioClip = (ImageView) findViewById(R.id.btnAudioClip);
		btnAudioClip.setOnClickListener(this);

		btnMovieClip = (ImageView) findViewById(R.id.btnMovieClip);
		btnMovieClip.setOnClickListener(this);

		if(subCat!=null && subCat.length()>0){
			isModelType=true;
			btnImage.setVisibility(View.GONE);
			btnAudioClip.setVisibility(View.GONE);
		}
		else {
			btnImage.setVisibility(View.VISIBLE);
			btnAudioClip.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if (view.equals(btnBack)) {
			onBackPressed();
		
		} else if (view.equals(btnImage)) {
			Intent intent = new Intent(mContext, TakePictureActivity.class);
			intent.putExtra("categoryId",categoryId);
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		} else if (btnAudioClip.equals(view)) {
			Intent intent = new Intent(mContext, RecordAudioActivity.class);
			intent.putExtra("categoryId",categoryId);
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		} else if (btnMovieClip.equals(view)) {
			Intent intent = new Intent(mContext, RecordVideoActivity.class);
			intent.putExtra("categoryId",categoryId);
			if(subCat!=null){
				intent.putExtra("subCat",subCat);	
			}
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		if(isModelType){
			Intent intent = new Intent(mContext, SelectSubCategoryActivity.class);
			intent.putExtra("categoryId",categoryId);
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		}
		else {
			Intent intent = new Intent(mContext, SelectCategoryActivity.class);
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		}
	}

}
