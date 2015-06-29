package com.crea3d.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crea3d.spotifystreamer.data.ParcelableArtist;
import com.crea3d.spotifystreamer.data.ParcelableTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PlayerActivity extends BaseActivity {

    public final static String EXTRA_ARTIST = "artist";
    public static final String EXTRA_TRACKS = "tracks";
    public static final String EXTRA_TRACK = "track";

    private ParcelableArtist artist;
    private ArrayList<ParcelableTrack> trackList;
    private ParcelableTrack track;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);


        // get albumName id from intent, finish the activity if the id is missing
        artist = getIntent().getParcelableExtra(EXTRA_ARTIST);
        trackList = getIntent().getParcelableArrayListExtra(EXTRA_TRACKS);
        track = getIntent().getParcelableExtra(EXTRA_TRACK);

        getFragmentManager().beginTransaction().replace(R.id.playerContainer, PlayerFragment.newInstance(artist, trackList, track)).commit();


    }


}
