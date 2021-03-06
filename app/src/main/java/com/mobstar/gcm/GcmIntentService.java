package com.mobstar.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mobstar.ProfileActivity;
import com.mobstar.R;
import com.mobstar.home.HomeActivity;
import com.mobstar.inbox.GroupMessageDetail;
import com.mobstar.inbox.MessageDetail;
import com.mobstar.utils.Constant;
import com.mobstar.utils.Utility;

public class GcmIntentService extends IntentService {

	public static int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	SharedPreferences pref;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {

			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error: ");
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server: ");
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

				//added khyati temporary commented



				if(extras.containsKey("Type")){
					String badgeCount="";
					if(extras.getString("badge").toString()!=null){
						badgeCount=extras.getString("badge");
					}
					
					if(extras.getString("Type").toString().equalsIgnoreCase("Message")){
						String messageGroup=extras.getString("messageGroup");
						String threadId=extras.getString("entry_id");
						String message=extras.getString("message").toString();
						String userName=extras.getString("diaplayname").toString();
						if(messageGroup!=null && threadId!=null && message!=null && userName!=null){
							sendNotification(message,messageGroup,threadId,userName);
						}

					}
					else if(extras.getString("Type").toString().equalsIgnoreCase("Like")){
						String message=extras.getString("message").toString();
						String entryId=extras.getString("entry_id").toString();
						if(entryId!=null){
							sendNotification(message,entryId);
						}
					}
					else{
						if(extras.getString("message").toString()!=null) {
							sendNotification(extras.getString("message").toString());
						}
					}
					
					Utility.setBadgeSamsung(getApplicationContext(), Integer.parseInt(badgeCount));
					Utility.setBadgeSony(getApplicationContext(), Integer.parseInt(badgeCount));
				}

				Log.v(Constant.TAG, "Received: " + extras.toString());
			}
		}

		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	 


	private void sendNotification(String msg) {

		Intent intent = new Intent("GetNotificationCount");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Mobstar Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setContentText(msg);
		mBuilder.setAutoCancel(true);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void sendNotification(String msg,String messageGroup,String threadId,String name) {

		Intent intent = new Intent("GetNotificationCount");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = null;

		if(messageGroup.equalsIgnoreCase("0")){
			Intent i=new Intent(this,MessageDetail.class);
			i.putExtra("threadId",threadId);
			i.putExtra("UserName",name);
			i.putExtra("FromNotification",true);
			contentIntent = PendingIntent.getActivity(this, 0,i, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		else{
			Intent i=new Intent(this,GroupMessageDetail.class);
			i.putExtra("threadId",threadId);
			i.putExtra("FromNotification",true);
			contentIntent = PendingIntent.getActivity(this, 0,i, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Mobstar Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setContentText(msg);
		mBuilder.setAutoCancel(true);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private void sendNotification(String msg,String entryId) {

		Intent intent = new Intent("GetNotificationCount");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = null;
		Intent i=new Intent(this,ProfileActivity.class);
		i.putExtra("EntryId",entryId);
		contentIntent = PendingIntent.getActivity(this, 0,i, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Mobstar Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg)).setContentText(msg);
		mBuilder.setAutoCancel(true);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
	
	
}
