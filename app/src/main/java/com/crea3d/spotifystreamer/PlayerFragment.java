package com.crea3d.spotifystreamer;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.crea3d.spotifystreamer.data.ParcelableArtist;
import com.crea3d.spotifystreamer.data.ParcelableTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerFragment extends DialogFragment {

    private static final String BUNDLE_ARTIST = "artist";
    private static final String BUNDLE_TRACK_LIST = "trackList";
    private static final String BUNDLE_TRACK = "track";


    private View loadingView;

    private TextView artistName;
    private TextView albumName;
    private ImageView trackImage;
    private TextView trackName;
    private SeekBar playerProgress;
    private TextView currentTime;
    private TextView duration;
    private ImageView prevTrack;
    private ImageView playPause;
    private ImageView nextTrack;

    private ParcelableArtist artist;
    private ArrayList<ParcelableTrack> trackList;
    private ParcelableTrack track;

    private PlayerReceiver playerReceiver;
    private boolean isPlaying;

    public static PlayerFragment newInstance(ParcelableArtist artist, ArrayList<ParcelableTrack> trackList, ParcelableTrack track) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_ARTIST, artist);
        bundle.putParcelableArrayList(BUNDLE_TRACK_LIST, trackList);
        bundle.putParcelable(BUNDLE_TRACK, track);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (playerReceiver == null) {
            playerReceiver = new PlayerReceiver();
        }

        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light);

        //recovering data from savedinstancestate if avalilable;

        if (savedInstanceState != null) {
            artist = savedInstanceState.getParcelable(BUNDLE_ARTIST);
            trackList = savedInstanceState.getParcelableArrayList(BUNDLE_TRACK_LIST);
            track = savedInstanceState.getParcelable(BUNDLE_TRACK);
        } else if (getArguments() != null) {
            artist = getArguments().getParcelable(BUNDLE_ARTIST);
            trackList = getArguments().getParcelableArrayList(BUNDLE_TRACK_LIST);
            track = getArguments().getParcelable(BUNDLE_TRACK);
        }

        if(track == null && trackList != null && trackList.size() > 0){
            track = trackList.get(0);
            Intent intent = new Intent(getActivity(), MediaService.class);
            intent.setAction(MediaService.ACTION_SET_TRACK);
            intent.putExtra(MediaService.EXTRA_ARTIST, artist);
            intent.putExtra(MediaService.EXTRA_TRACK_LIST, trackList);
            getActivity().startService(intent);
        }
        // check if the argument was correctly passed in the arguments bundle

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

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(BUNDLE_ARTIST, artist);
        outState.putParcelableArrayList(BUNDLE_TRACK_LIST, trackList);
        outState.putParcelable(BUNDLE_TRACK, track);

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(playerReceiver, new IntentFilter(MediaService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(playerReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_player, container, false);

        // set layouts

        loadingView = view.findViewById(R.id.loading_view);
        artistName = (TextView) view.findViewById(R.id.player_artist_name);
        albumName = (TextView) view.findViewById(R.id.player_album_name);
        trackImage = (ImageView) view.findViewById(R.id.player_image);
        trackName = (TextView) view.findViewById(R.id.player_track_name);
        playerProgress = (SeekBar) view.findViewById(R.id.player_seekbar);
        currentTime = (TextView) view.findViewById(R.id.player_current_time);
        duration = (TextView) view.findViewById(R.id.player_duration);
        prevTrack = (ImageView) view.findViewById(R.id.player_previous);
        playPause = (ImageView) view.findViewById(R.id.player_play);
        nextTrack = (ImageView) view.findViewById(R.id.player_next);

        // valorize layout items;

        nextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextTrack();
            }
        });

        prevTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevTrack();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    getActivity().startService(new Intent(getActivity(), MediaService.class).setAction(MediaService.ACTION_PAUSE));
                    playPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    getActivity().startService(new Intent(getActivity(), MediaService.class).setAction(MediaService.ACTION_PLAY));
                    playPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });

        playerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    getActivity().startService(new Intent(getActivity(), MediaService.class).setAction(MediaService.ACTION_SEEK).putExtra(MediaService.EXTRA_SEEK_TO, progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return view;
    }




    private void nextTrack() {
        getActivity().startService(new Intent(getActivity(), MediaService.class).setAction(MediaService.ACTION_NEXT));
    }

    private void prevTrack() {
        int trackIndex = trackList.indexOf(track);
        if (trackIndex > 0) {
            track = trackList.get(trackIndex - 1);
            getActivity().startService(new Intent(getActivity(), MediaService.class).setAction(MediaService.ACTION_PREVIOUS));
        }
    }

    private void checkButtonsStatus() {
        int trackIndex = trackList.indexOf(track);

        if (trackIndex > 0) {
            prevTrack.setAlpha(1f);
        } else {
            prevTrack.setAlpha(0.3f);
        }

        if (trackIndex < trackList.size() - 1) {
            nextTrack.setAlpha(1f);
        } else {
            nextTrack.setAlpha(0.3f);
        }
    }


    private class PlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MediaService.ACTION_BROADCAST)) {
                loadingView.setVisibility(intent.getBooleanExtra(MediaService.EXTRA_PREPARING, false) ? View.VISIBLE : View.GONE);
                artist = intent.getParcelableExtra(MediaService.EXTRA_ARTIST);
                trackList = intent.getParcelableArrayListExtra(MediaService.EXTRA_TRACK_LIST);
                track = intent.getParcelableExtra(MediaService.EXTRA_TRACK);
                int currentPosition = intent.getIntExtra(MediaService.EXTRA_PROGRESS, -1);
                int trackDuration = intent.getIntExtra(MediaService.EXTRA_DURATION, -1);
                isPlaying = intent.getBooleanExtra(MediaService.EXTRA_PLAYING, false);

                checkButtonsStatus();

                artistName.setText(artist.getName());
                albumName.setText(track.getAlbumName());
                trackName.setText(track.getName());

                if (TextUtils.isEmpty(track.getThumbnailURL())) {
                    Picasso.with(getActivity()).load(R.drawable.image_placeholder).into(trackImage);
                } else {
                    Picasso.with(getActivity()).load(track.getThumbnailURL()).into(trackImage);
                }

                if (trackDuration > 0) {
                    int totalSecs = (int) TimeUnit.MILLISECONDS.toSeconds(trackDuration);
                    int hours = totalSecs / 3600;
                    int minutes = (totalSecs % 3600) / 60;
                    int seconds = totalSecs % 60;
                    duration.setText((hours > 0 ? "" + hours + ":" : "") + (minutes > 0 ? "" + minutes + ":" : "00:") + (seconds < 10 ? "0" + seconds : "" + seconds));
                    playerProgress.setMax(trackDuration);
                } else {
                    duration.setText("--:--");
                }

                if (currentPosition > 0) {
                    int totalSecs = (int) TimeUnit.MILLISECONDS.toSeconds(currentPosition);
                    int hours = totalSecs / 3600;
                    int minutes = (totalSecs % 3600) / 60;
                    int seconds = totalSecs % 60;
                    currentTime.setText((hours > 0 ? "" + hours + ":" : "") + (minutes > 0 ? "" + minutes + ":" : "00:") + (seconds < 10 ? "0" + seconds : "" + seconds));
                    playerProgress.setProgress(currentPosition);
                } else {
                    currentTime.setText("--:--");
                    playerProgress.setProgress(0);
                }

                playPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

            }
        }
    }
}
