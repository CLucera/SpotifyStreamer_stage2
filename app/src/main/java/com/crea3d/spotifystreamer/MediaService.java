package com.crea3d.spotifystreamer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.crea3d.spotifystreamer.data.ParcelableArtist;
import com.crea3d.spotifystreamer.data.ParcelableTrack;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;

public class MediaService extends Service {

    public static final int NOTIFICATION_ID = 100;

    public static final String ACTION_BROADCAST = "com.crea3d.spotifystreamer.PLAYER_BROADCAST";
    public static final String ACTION_SET_TRACK = "com.crea3d.spotifystreamer.PLAYER_SET_TRACK";
    public static final String ACTION_PLAY = "com.crea3d.spotifystreamer.PLAYER_PLAY";
    public static final String ACTION_PAUSE = "com.crea3d.spotifystreamer.PLAYER_PAUSE";
    public static final String ACTION_SEEK = "com.crea3d.spotifystreamer.PLAYER_SEEK";
    public static final String ACTION_PREVIOUS = "com.crea3d.spotifystreamer.PLAYER_PREVIOUS";
    public static final String ACTION_NEXT = "com.crea3d.spotifystreamer.PLAYER_NEXT";

    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_TRACK_LIST = "trackList";
    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_DURATION = "maxProgress";
    public static final String EXTRA_PLAYING = "isPlaying";
    public static final String EXTRA_SEEK_TO = "seekTo";
    public static final String EXTRA_PREPARING = "preparing";

    private static MediaPlayer player;

    private ParcelableArtist artist;
    private ArrayList<ParcelableTrack> trackList;
    private ParcelableTrack track;

    private boolean preparing;
    private boolean loadingWithoutPlay = false;

    private Handler syncHandler = new Handler();
    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {

            //recursive handler for broadcasting the player status

            if (player != null) {

                Intent intent = new Intent(ACTION_BROADCAST);
                intent.putExtra(EXTRA_ARTIST, artist);
                intent.putExtra(EXTRA_TRACK_LIST, trackList);
                intent.putExtra(EXTRA_TRACK, track);
                intent.putExtra(EXTRA_PROGRESS, player.getCurrentPosition());
                intent.putExtra(EXTRA_DURATION, player.getDuration());
                intent.putExtra(EXTRA_PLAYING, player.isPlaying());
                intent.putExtra(EXTRA_PREPARING, preparing);

                sendBroadcast(intent);
                syncHandler.postDelayed(this, 100);
            } else {
                stopSelf();
            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (player == null) {
            player = new MediaPlayer();

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    startNotification();
                }
            });
        }

        if (intent != null && intent.getAction().equals(ACTION_SET_TRACK)) {
            artist = intent.getParcelableExtra(EXTRA_ARTIST);
            trackList = intent.getParcelableArrayListExtra(EXTRA_TRACK_LIST);
            track = intent.getParcelableExtra(EXTRA_TRACK);

            // check if the argument was correctly passed in the arguments bundle

            if (track == null && (trackList != null && trackList.size() > 0)) {
                track = trackList.get(0);
                loadingWithoutPlay = true;
            }

            if (artist == null || trackList == null || track == null) {
                String missingArguments = "";
                if (artist == null) {
                    missingArguments += "artist";
                }

                if (trackList == null) {
                    missingArguments += (TextUtils.isEmpty(missingArguments) ? "" : ", ") + "track list";
                }

                if (track == null) {
                    missingArguments += (TextUtils.isEmpty(missingArguments) ? "" : ", ") + "current track";
                }

                throw new IllegalArgumentException("Missing arguments: " + missingArguments);
            }

            syncHandler.post(syncRunnable);

            setCurrentTrack(track);
        }

        if (intent != null && intent.getAction().equals(ACTION_PLAY)) {
            player.start();
            startNotification();
        }

        if (intent != null && intent.getAction().equals(ACTION_PAUSE)) {
            player.pause();
            startNotification();
        }

        if (intent != null && intent.getAction().equals(ACTION_SEEK)) {
            player.seekTo(intent.getIntExtra(EXTRA_SEEK_TO, 0));
        }

        if (intent != null && intent.getAction().equals(ACTION_PREVIOUS)) {
            prevTrack();
        }

        if (intent != null && intent.getAction().equals(ACTION_NEXT)) {
            nextTrack();
        }

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }

        if (syncHandler != null) {
            syncHandler.removeCallbacks(syncRunnable);
            syncHandler = null;
        }



        super.onDestroy();
    }

    public void setCurrentTrack(ParcelableTrack newTrack) {

        preparing = true;
        track = newTrack;

        startNotification();

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                preparing = false;
                if(loadingWithoutPlay){
                    loadingWithoutPlay = false;
                    return;
                }
                player.start();
                startNotification();

            }
        });

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.reset();
            player.setDataSource(track.getPreviewUrl());
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace(); //url not valid;
            preparing = false;
        }

    }

    private void nextTrack() {
        int trackIndex = trackList.indexOf(track);
        if (trackIndex < trackList.size() - 1) {
            track = trackList.get(trackIndex + 1);
            setCurrentTrack(track);
        }
    }

    private void prevTrack() {
        int trackIndex = trackList.indexOf(track);
        if (trackIndex > 0) {
            track = trackList.get(trackIndex - 1);
            setCurrentTrack(track);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("NewApi")
    private void startNotification() {

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                String songName = track.getName();

                boolean playing = player.isPlaying();
                int trackIndex = trackList.indexOf(track);

                Intent prevIntent = new Intent(MediaService.this, MediaService.class).setAction(ACTION_PREVIOUS);
                Intent playPauseIntent = new Intent(MediaService.this, MediaService.class).setAction(playing ? ACTION_PAUSE : ACTION_PLAY);
                Intent nextIntent = new Intent(MediaService.this, MediaService.class).setAction(ACTION_NEXT);

                PendingIntent prevPending = PendingIntent.getService(MediaService.this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent playPausePending = PendingIntent.getService(MediaService.this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent nextPending = PendingIntent.getService(MediaService.this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(bitmap)
                        .setTicker(songName)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setContentTitle(songName);

                if (trackIndex > 0) {
                    notificationBuilder.addAction(android.R.drawable.ic_media_previous, getString(R.string.previous), prevPending);
                }

                if(!preparing) {
                    notificationBuilder.addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, getString(playing ? R.string.pause : R.string.play), playPausePending);
                }

                if (trackIndex < trackList.size() - 1) {
                    notificationBuilder.addAction(android.R.drawable.ic_media_next, getString(R.string.next), nextPending);
                }

                notificationBuilder.setVisibility(Utils.isNotificationActiveOnLockScreen(MediaService.this) ? Notification.VISIBILITY_PUBLIC : Notification.VISIBILITY_SECRET);

                Notification notification = notificationBuilder.build();

                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                startForeground(NOTIFICATION_ID, notification);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        if (TextUtils.isEmpty(track.getThumbnailURL())) {
            Picasso.with(this).load(R.drawable.image_placeholder_square).into(target);
        } else {
            Picasso.with(this).load(track.getThumbnailURL()).into(target);
        }

    }

}
