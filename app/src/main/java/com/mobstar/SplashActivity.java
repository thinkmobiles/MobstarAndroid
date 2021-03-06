package com.mobstar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mobstar.home.HomeActivity;
import com.mobstar.home.HomeInformationActivity;
import com.mobstar.login.LoginSocialActivity;
import com.mobstar.utils.Constant;
import com.mobstar.utils.JSONParser;
import com.mobstar.utils.Utility;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends Activity {

	Timer timer;
	Context mContext;

	GoogleCloudMessaging gcm;
	String regid;
	String deepLinkedId;
	private String show_system_notification="",defaultNotificationTitle="",defaultNotificationImage="",description="";

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	SharedPreferences preferences;
	private String sErrorMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		String android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);
		String device_id=android_id;
		Log.d("mobstar","device id"+device_id); 

		mContext = SplashActivity.this;

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy tp = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(tp);
		}

		preferences = getSharedPreferences("mobstar_pref", Activity.MODE_PRIVATE);
		
		

		//Added by khyati for deeplinking
		Intent intent = getIntent();
		if(intent!=null) {
			String action = intent.getAction();
			Uri data = intent.getData();
			if(data!=null) {
				Log.d("mobstar","uri==>"+data.toString());	
				deepLinkedId=data.getQueryParameter("id");
				if(deepLinkedId!=null){
					Log.d("mobstar","id==>"+deepLinkedId);
					//				    		SharedPreferences pref = getSharedPreferences("mobstar_pref", MODE_PRIVATE);
					//							pref.edit().putString("deepLinkedId",deepLinkedId).commit();

				}
				
			}

		}
		
		preferences = getSharedPreferences("mobstar_pref", MODE_PRIVATE);
		if (preferences.getBoolean("isLogin", false)) {

			if(deepLinkedId!=null) {
				Intent intent1 = new Intent(mContext,HomeActivity.class);
				intent1.putExtra("deepLinkedId",deepLinkedId);
				startActivity(intent1);
			}
			else {
				//clear badge
				Utility.clearBadge(mContext);
				new BadgeRead().run();
				sendAnalytics();
				if (Utility.isNetworkAvailable(mContext)) {

					new HomeInfoCall().start();

				} else {

					Toast.makeText(mContext, "No, Internet Access!", Toast.LENGTH_SHORT).show();
//					Utility.HideDialog(mContext);
				}
				
			}

		} else {
			sendAnalytics();
			timer = new Timer();
			TimerTask task = new TimerTask() {

				@Override
				public void run() {
					Intent i = new Intent(mContext,LoginSocialActivity.class);
					startActivity(i);
					finish();
					
					

					//added by khyati
					/*Log.d("mobstar","isVerifyMobileCode"+pref.getBoolean("isVerifyMobileCode", false));
					if (pref.getBoolean("isLogin", false) && pref.getBoolean("isVerifyMobileCode", false)) {
						Intent intent = new Intent(mContext,HomeActivity.class);
						startActivity(intent);
					}
					else if(pref.getBoolean("isLogin", false) && !pref.getBoolean("isVerifyMobileCode", false)){
						Intent intent = new Intent(mContext,VerifyMobileNoActivity.class);
						startActivity(intent);
					}
					else {
						Intent intent = new Intent(mContext,LoginSocialActivity.class);
						startActivity(intent);
					}
					finish();*/
				}
			};
			timer.schedule(task, 3000);
		}
		


		

		Utility.SendDataToGA("Splash Screen", SplashActivity.this);

		// GCM Push Notification
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			regid = Utility.getRegistrationId(mContext);
			//Log.v(Constant.TAG, "GCM Registration ID "+regid);
			if (regid.isEmpty()) {
				registerInBackground();
			}
		}

	}

	private void registerInBackground() {
		new GCMRegistrationCall().start();
	}

	class GCMRegistrationCall extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(mContext);
				}
				regid = gcm.register(Constant.SENDER_ID);
				Log.v(Constant.TAG, "GCM Registration ID "+regid);
				// Persist the regID - no need to register again.
				Utility.storeRegistrationId(SplashActivity.this, regid);

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		// super.onBackPressed();
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(Constant.TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}
	class HomeInfoCall extends Thread {

		@Override
		public void run() {

			String Query=Constant.SERVER_URL + Constant.HOME_INFO;
			String response = JSONParser.getRequest(Query,preferences.getString("token", null));

			Log.v(Constant.TAG, "home info response " + response);

			if (response != null) {

				try {
					Thread.sleep(5000);
					if(response!=null){
						JSONObject jsonObject = new JSONObject(response);

						if (jsonObject.has("error")) {
							sErrorMessage = jsonObject.getString("error");
						}

						if (jsonObject.has("show_system_notification")) {
							show_system_notification = jsonObject.getString("show_system_notification");
							defaultNotificationTitle=jsonObject.getString("defaultNotificationTitle");
							defaultNotificationImage=jsonObject.getString("defaultNotificationImage");
							description=jsonObject.getString("description");

						}

						if (sErrorMessage != null && !sErrorMessage.equals("")) {
							handlerInfo.sendEmptyMessage(0);
						} else {
							handlerInfo.sendEmptyMessage(1);
						}
					}
					

				} catch (Exception e) {
					e.printStackTrace();
					handlerInfo.sendEmptyMessage(0);
				}

			} else {

				handlerInfo.sendEmptyMessage(0);
			}

		}
	}



	Handler handlerInfo = new Handler() {

		@Override
		public void handleMessage(Message msg) {
//			Utility.HideDialog(mContext);

			if (msg.what == 1) {
				if(show_system_notification!=null && show_system_notification.equalsIgnoreCase("TRUE")){
					Intent i=new Intent(mContext,HomeInformationActivity.class);
					i.putExtra("title",defaultNotificationTitle);
					i.putExtra("img",defaultNotificationImage);
					i.putExtra("des",description);
					startActivity(i);
					finish();

				}
			} else {
				OkayAlertDialog(sErrorMessage);
			}
		}
	};
	
	class BadgeRead extends Thread {

		@Override
		public void run() {

			String Query=Constant.SERVER_URL + Constant.BADGE_READ;
			String response = JSONParser.postRequest(Query,null,null,preferences.getString("token", null));

			Log.v(Constant.TAG, "home info response " + response);

			if (response != null) {

				try {
					if(response!=null){
						
							handlerBadge.sendEmptyMessage(1);
					}
					

				} catch (Exception e) {
					e.printStackTrace();
					handlerBadge.sendEmptyMessage(0);
				}

			} else {

				handlerBadge.sendEmptyMessage(0);
			}

		}
	}



	Handler handlerBadge= new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what == 1) {
				
			} else {
			}
		}
	};
	
	private void sendAnalytics(){
		String myVersion = android.os.Build.VERSION.RELEASE; // e.g. myVersion := "1.6"
		
		String packageToCheck = "com.mobstar";  
		
		String versionName="";
		int versionCode = 0;

		List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
		for (int i=0; i<packages.size(); i++) {
		    PackageInfo p = packages.get(i);
		    if (p.packageName.contains(packageToCheck)) {
		        String name = p.applicationInfo.loadLabel(getPackageManager()).toString();
		        String pname = p.applicationInfo.loadLabel(getPackageManager()).toString();
		        String packageName = p.packageName;
		        versionName = p.versionName;
		        versionCode = p.versionCode;
		        Log.i("mobstar", name + ": " + pname + ": " + packageName + ": " + versionName + ": " + versionCode);
		    }
		}
		
		String deviceName=getDeviceName();
		
		if (Utility.isNetworkAvailable(mContext)) {
			Log.d("log_tag","versionName"+versionName);
			Log.d("log_tag","versionCode"+versionCode);
			Log.d("log_tag","Analytics data==>osVersion "+myVersion +" appVersion "+versionCode+" deviceName "+deviceName);
			new AddAnalytics(myVersion,versionCode,deviceName).start();

		} else {
			
		}
		
	}
	
	public String getDeviceName() {
		   String manufacturer = Build.MANUFACTURER;
		   String model = Build.MODEL;
		   if (model.startsWith(manufacturer)) {
		      return capitalize(model);
		   } else {
		      return capitalize(manufacturer) + " " + model;
		   }
		}


		private String capitalize(String s) {
		    if (s == null || s.length() == 0) {
		        return "";
		    }
		    char first = s.charAt(0);
		    if (Character.isUpperCase(first)) {
		        return s;
		    } else {
		        return Character.toUpperCase(first) + s.substring(1);
		    }
		}
	
	class AddAnalytics extends Thread {
		
		String osVersion,appVersion,deviceName;
		
		public AddAnalytics(String os,int appV,String deviceName){
			this.osVersion=os;
			this.appVersion=String.valueOf(appV);
			this.deviceName=deviceName;
		}

		@Override
		public void run() {
			
			String[] name = {"platform","osversion","appversion","devicename"};
			String[] value = {"Android",osVersion,appVersion,deviceName};
			String response = JSONParser.postRequest(Constant.SERVER_URL + Constant.ANALYTICS_READ, name, value,preferences.getString("token", null));

			Log.v(Constant.TAG, "home info response " + response);

			if (response != null) {

				try {
					Thread.sleep(10000);
					JSONObject jsonObject = new JSONObject(response);

					if (jsonObject.has("error")) {
						sErrorMessage = jsonObject.getString("error");
					}

					if (sErrorMessage != null && !sErrorMessage.equals("")) {
						handlerAnalytics.sendEmptyMessage(0);
					} else {
						handlerAnalytics.sendEmptyMessage(1);
					}

				} catch (Exception e) {
					e.printStackTrace();
					handlerAnalytics.sendEmptyMessage(0);
				}

			} else {

				handlerAnalytics.sendEmptyMessage(0);
			}

		}
	}



	Handler handlerAnalytics = new Handler() {

		@Override
		public void handleMessage(Message msg) {
//			Utility.HideDialog(mContext);

			if (msg.what == 1) {
			
			} else {
			}
		}
	};

	void OkayAlertDialog(final String msg) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

				// set title
				alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));

				// set dialog message
				alertDialogBuilder.setMessage(msg).setCancelable(false).setNeutralButton("OK", null);

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
			}
		});

	}

}
