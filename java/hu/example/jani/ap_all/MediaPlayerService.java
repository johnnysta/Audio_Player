package hu.example.jani.ap_all;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static hu.example.jani.ap_all.MainActivity.Status.INITIAL_NO_FILES;
import static hu.example.jani.ap_all.MainActivity.Status.INITIAL_WITH_FILES;
import static hu.example.jani.ap_all.MainActivity.Status.PAUSED;
import static hu.example.jani.ap_all.MainActivity.Status.PAUSED_BY_INCOMING_CALL;
import static hu.example.jani.ap_all.MainActivity.Status.PLAYING;
import static hu.example.jani.ap_all.MainActivity.Status.STOPPED;


/**
 * Created by Janó on 2017.01.25..
 */

public class MediaPlayerService extends Service {


    private final IBinder myBinder = new MyLocalBinder();

    public Thread t = null;
    private MediaPlayer mediaPlayer = null;
    private ArrayList<File> servicePlayList = null;
    private int currentTrack, index = 0;
    private boolean looping = false;
    private hu.example.jani.ap_all.MainActivity.Status
            playerStatus = INITIAL_NO_FILES;

    public NotificationCompat.Builder notifBuilder;
    private PendingIntent pi;
    public static final int PENDING_INTENT_ID = 201;
    public static final int NOTIF_ID = 101;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }


    public void onCreate(){
        super.onCreate();

        //Intent pending intent létrehozása a notificationhöz, ami Foreground mód esetén kötelező
        Intent i = new Intent(this, MainActivity.class);
        pi = PendingIntent.getActivity(this, PENDING_INTENT_ID, i, PendingIntent.FLAG_CANCEL_CURRENT);

        notifBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.music)
                        .setContentTitle("Music Player")
                        .setContentText("Running service. " +
                                "Tap here to open.")
                        .setTicker("Playing MP3")
                        .setContentIntent(pi);
    }

    public void onDestroy(){
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            }
        super.onDestroy();
    }


    public int onStartCommand(Intent intent, int flags, int startId){
        //Beállítjuk a Foreground módot és a hozzá kötelező notificationt
        Notification notification = notifBuilder.build();
        startForeground(NOTIF_ID, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    public void pauseMusic(boolean byIncomingCall) {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            if (!byIncomingCall)
                playerStatus=PAUSED;
            else
                playerStatus=PAUSED_BY_INCOMING_CALL;
        }
    }

    public void setCurrentTrack(int currentTrack) {
        this.currentTrack = currentTrack;
    }

    public void setPlayerStatus(MainActivity.Status playerStatus) {
        this.playerStatus = playerStatus;
    }

    public void setPlayList(ArrayList<File> aPlayList) {
        if (this.servicePlayList==null)
            this.servicePlayList = new ArrayList<File>(aPlayList);
        else {
            this.servicePlayList.clear();
            this.servicePlayList.addAll(aPlayList);
        }
    }

    public void setLooping(boolean looping){
        this.looping = looping;
    }

    public class MyLocalBinder extends Binder {
        MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }

    public void stopMusic(){
        if (mediaPlayer != null) {
            //mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            playerStatus=STOPPED;
            }
    }

    public int getCurrentTrack(){
        return currentTrack;
    }

    public boolean getLooping(){
        return looping;
    }

    public ArrayList<File> getPlayList(){
        if (servicePlayList!=null)
            return new ArrayList<File>(servicePlayList);
        else
            return null;
    }

    public MainActivity.Status getPlayerStatus(){
        return playerStatus;
    }




    //When playing jumps to the next track,
    // sends a local broadcast message to main activity so tht it can update UI elements:
    public void sendBroadcastMessage(int track, String ps)
    {
        Intent intent = new Intent();
        intent.setAction("CURRENT_TRACK_CHANGED");
        // You can also include some extra data.
        intent.putExtra("Current track", track);
        intent.putExtra("Player status", ps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }


    public void setIndex(int ii){
        index= ii;
    }

    public void playMusic(ArrayList<File> aFiles, final boolean aLooping, int aCurrentTrack){
        if (this.servicePlayList==null)
            this.servicePlayList = new ArrayList<File>(aFiles);
        else {
            this.servicePlayList.clear();
            this.servicePlayList.addAll(aFiles);
        }
        Log.d("PL mérete a serviceben:", String.valueOf(servicePlayList.size()));
        looping = aLooping;
        currentTrack= aCurrentTrack;
        if (playerStatus == PAUSED || playerStatus == PAUSED_BY_INCOMING_CALL){
            if (mediaPlayer != null) {
                playerStatus = PLAYING;
                mediaPlayer.start();
            }
        }
        else {
            t = new Thread()
            {
                boolean playFinished;

                @Override
                public void run() {
                    Log.d("PL run metódus elején:", String.valueOf(servicePlayList.size()));
                    //If a mediaPlayer exists (e.g. the previous object was not released, because some button
                    // had benn pressed), release it:
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    //Initialize mediaPlayer
                    for (index = currentTrack; index < servicePlayList.size(); index++) {
                        Log.d("PL mérete ciklusnál:", String.valueOf(servicePlayList.size()));
                        currentTrack = index;
                        sendBroadcastMessage(currentTrack, "PLAYING");
                        Log.d("PL mérete BR után:", String.valueOf(servicePlayList.size()));
                        playerStatus = PLAYING;
                        playFinished = false;
                        Log.d("PL new MP előtt:", String.valueOf(servicePlayList.size()));
                        mediaPlayer = new MediaPlayer();
                        Log.d("PL new MP után:", String.valueOf(servicePlayList.size()));
                        //mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        //mediaPlayer.setLooping(looping);

                        try {
                            Log.d("PL mérete a hibánál:", String.valueOf(servicePlayList.size()));
                            Log.d("i értéke:", String.valueOf(index));

                            mediaPlayer.setDataSource(servicePlayList.get(index).getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                if (!mp.isPlaying()) mp.start();

                            }
                        });

                        try {
                            mediaPlayer.prepareAsync();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }


                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                                public void onCompletion(MediaPlayer mp) {
                                                                    playFinished = true;
                                                                }
                                                            }
                        );

                        while (!playFinished) {
                            try {
                                this.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //When track is finished, free media player
                        mediaPlayer.release();
                        mediaPlayer = null;
                        //if looping is on, at the end of servicePlayList start form  beginning
                        if (index == servicePlayList.size() - 1 & looping) {
                            index = -1;
                        }
                    }

                    //If finished playing all tracks, and no looping is set, set the program to initial
                    // by sending a broadcast with -2 "NON_LOOPING_PLAY_FINISHED" state:
                    currentTrack = 0;
                    playerStatus = INITIAL_WITH_FILES;
                    sendBroadcastMessage(currentTrack, "INITIAL_WITH_FILES");


                }
            };
            t.start();
        }
    }

}



