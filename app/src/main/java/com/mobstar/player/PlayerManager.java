package com.mobstar.player;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.mobstar.home.new_home_screen.EntryItem;

import java.io.File;

/**
 * Created by Alexandr on 16.09.2015.
 */
public class PlayerManager implements TextureView.SurfaceTextureListener {
    private static final String LOG_TAG = PlayerManager.class.getName();
    private MediaPlayer mediaPlayer;
    private Context mContext;
    private EntryItem mEntryItem;
    private static PlayerManager instance;
    private String mFilePath;
    private boolean isMediaPlayerError = false;
    private boolean isVideoFile;
    private Surface mWorkingSurface;

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void init(Context context, EntryItem entryItem, String fileName) {
        this.mContext = context;
        this.mEntryItem = entryItem;
        isVideoFile = (mEntryItem.getEntryPojo().getType().equals("video"));
        mFilePath = fileName;
    }

    private void sendRequstAddCount() {
//        Log.d(LOG_TAG, "sendRequstAddCount");

    }

    public boolean tryToPlayNew() {
        Log.d(LOG_TAG, "mEntryItem.pos" + mEntryItem.getPos());
        Log.d(LOG_TAG, "mEntryItem.pos" + mEntryItem.getEntryPojo().getDescription());
//        Log.d(LOG_TAG,"mEntryItem.pos"+mEntryItem.getPos());

        releaseMP();
        mWorkingSurface = null;
        mediaPlayer = new MediaPlayer();
        File file = new File(mFilePath);
        boolean result = false;
        if (file.exists()) {
            try {
                mediaPlayer.setDataSource(mFilePath);

                if (isVideoFile) {
                    Log.v(LOG_TAG, "getTextureView().isAvailable()=" + mEntryItem.getTextureView().isAvailable());
                    if (mEntryItem.getTextureView().isAvailable()) {
                        mWorkingSurface = new Surface(mEntryItem.getTextureView().getSurfaceTexture());
                        mediaPlayer.setSurface(mWorkingSurface);
                    } else mEntryItem.getTextureView().setSurfaceTextureListener(this);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }

                // Play video when the media source is ready for
                // playback.
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        Log.v(LOG_TAG, "setOnPreparedListener.onPrepared");


                        if (isVideoFile) {
                            if (mWorkingSurface != null) {
                                Log.v(LOG_TAG, "setOnPreparedListener.onPrepared.startVIDEO");
                                mEntryItem.playVideoState();
                                mediaPlayer.start();
                            }
                        } else {
                            Log.v(LOG_TAG, "setOnPreparedListener.onPrepared.startAUDIO");
                            mEntryItem.playAudioState();
                            mediaPlayer.start();
                        }

                    }
                });
                //														mediaPlayer.setLooping(true);
                //Added by Khyati
                mediaPlayer.setLooping(false);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
//                        Log.v(LOG_TAG,"setOnCompletionListener.onCompletion");
                        if (!isMediaPlayerError) {
                            sendRequstAddCount();
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        } else {
                            isMediaPlayerError = false;
                        }
                    }
                });

                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.v(LOG_TAG, "setOnErrorListener.onError");
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying())
                                mediaPlayer.pause();
                            isMediaPlayerError = true;
                            mediaPlayer.reset();

                        }
                        return false;
                    }
                });

                mediaPlayer.prepareAsync();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }

    public boolean tryToPause() {
        Log.v(LOG_TAG, "tryToPause");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                if (isVideoFile) mEntryItem.pauseVideoState();
                else mEntryItem.pauseAudioState();
            } else {
                if (isVideoFile) mEntryItem.playVideoState();
                else mEntryItem.playAudioState();
                mediaPlayer.start();
            }
            return true;
        }
        return false;
    }

    private boolean tryToStop() {
        Log.v(LOG_TAG, "tryToStop");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            return true;
        }
        return false;
    }

    private boolean tryToReset() {
        Log.v(LOG_TAG, "tryToReset");
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            return true;
        }
        return false;
    }

    public boolean finalizePlayer() {
        Log.v(LOG_TAG, "finalizePlayer");
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                return true;
            } catch (Exception e) {
                Log.v(LOG_TAG, "finalizePlayer.error=" + e.toString());
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void releaseMP() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(LOG_TAG, "onSurfaceTextureAvailable");
        tryToPlayNew();
//        mWorkingSurface = new Surface(surface);
//        mediaPlayer.setSurface(mWorkingSurface);
//        if (mediaPlayer != null)
//        if (!mediaPlayer.isPlaying()){
//            mEntryItem.playVideoState();
//            mediaPlayer.start();
//        }


    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
