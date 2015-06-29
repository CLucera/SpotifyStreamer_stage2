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


public class TopTenActivity extends BaseActivity {

    public final static String EXTRA_ARTIST = "artist";
    public final static String EXTRA_TRACKS = "tracks";
    private final static String OUTSTATE_TRACK_LIST = "trackList";

    private int currentSelected = -1;

    private ListView topTenList;
    private View loadingView;

    private SpotifyApi spotifyApi;
    private SpotifyService spotify;

    private ArrayList<ParcelableTrack> tracks = null;
    private TrackAdapter adapter;

    private ParcelableArtist artist;
    private int requestSentCount = 0;

    private boolean isTablet = false;
    private PlayerFragment playerFragment;
    private PlayerReceiver trackReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackReceiver = new PlayerReceiver();

        isTablet = getResources().getBoolean(R.bool.isTablet);

        // get albumName id from intent, finish the activity if the id is missing
        artist = getIntent().getParcelableExtra(EXTRA_ARTIST);
        tracks = getIntent().getParcelableExtra(EXTRA_TRACKS);

        if (artist == null) {
            Toast.makeText(this, R.string.artist_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //init spotify api

        spotifyApi = new SpotifyApi();
        spotify = spotifyApi.getService();

        //set layouts

        if (!TextUtils.isEmpty(artist.getName()) && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(artist.getName());
        }

        setContentView(R.layout.activity_topten);

        topTenList = (ListView) findViewById(R.id.top_ten_list);
        loadingView = findViewById(R.id.loading_view);
        isTablet = findViewById(R.id.playerContainer) != null;

        //restore state

        if (savedInstanceState != null) {
            tracks = savedInstanceState.getParcelableArrayList(OUTSTATE_TRACK_LIST);
        }

        //load data from web (only if not restored)

        if (tracks == null) {
            refreshLocale();
        }

        // set the topTenList adapter

        adapter = new TrackAdapter();
        topTenList.setAdapter(adapter);

        // bind events

        topTenList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(TopTenActivity.this, MediaService.class);
                intent.setAction(MediaService.ACTION_SET_TRACK);
                intent.putExtra(MediaService.EXTRA_ARTIST, artist);
                intent.putExtra(MediaService.EXTRA_TRACK_LIST, tracks);
                intent.putExtra(MediaService.EXTRA_TRACK, adapter.getItem(position));
                startService(intent);

                if (!isTablet) {
                    Intent playerIntent = new Intent(TopTenActivity.this, PlayerActivity.class);
                    playerIntent.putExtra(PlayerActivity.EXTRA_ARTIST, artist);
                    playerIntent.putExtra(PlayerActivity.EXTRA_TRACKS, tracks);
                    playerIntent.putExtra(PlayerActivity.EXTRA_TRACK, adapter.getItem(position));
                    startActivity(playerIntent);
                }

            }
        });

    }

    //activity methods

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(OUTSTATE_TRACK_LIST, tracks);
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void refreshLocale() {
        super.refreshLocale();
        tracks = new ArrayList<>();

        //show loader and start asyncronous request
        showLoader();
        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.COUNTRY, getSelectedLocale().getCountry()); //set default country for the getArti// ck request

        spotify.getArtistTopTrack(artist.getSpotifyId(), options, new Callback<Tracks>() {
            @Override
            public void success(final Tracks tracks, Response response) {

                //check if activity is still running

                if (isFinishing()) {
                    return;
                }

                //refresh the adapter data in the UiThread and hide the loader

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoader();
                        TopTenActivity.this.tracks = new ArrayList<ParcelableTrack>();
                        for (Track track : tracks.tracks) {
                            TopTenActivity.this.tracks.add(new ParcelableTrack(track));
                        }
                        if (isTablet) {
                            playerFragment = playerFragment != null ? playerFragment : PlayerFragment.newInstance(artist, TopTenActivity.this.tracks, null);
                            getFragmentManager().beginTransaction().replace(R.id.playerContainer, playerFragment).commit();
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void failure(final RetrofitError error) {

                //check if activity is still running

                if (isFinishing()) {
                    return;
                }

                //if the artist can't be retreived (connection lost or other errors) finish this activity and notify the user

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (error.getResponse().getStatus() == 400) {
                            Toast.makeText(TopTenActivity.this, getString(R.string.region_error, getSelectedLocale().getDisplayCountry()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(TopTenActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(trackReceiver, new IntentFilter(MediaService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(trackReceiver);
    }

    //layout managing methods

    public void showLoader() {
        requestSentCount++;
        loadingView.setVisibility(View.VISIBLE);
    }

    public void hideLoader() {
        requestSentCount--;
        requestSentCount = Math.max(requestSentCount, 0);
        if (requestSentCount == 0) {
            loadingView.setVisibility(View.GONE);
        }
    }

    //inner classes

    private class TrackAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return tracks.size();
        }

        @Override
        public ParcelableTrack getItem(int position) {
            return tracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            //set viewHolder

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(TopTenActivity.this).inflate(R.layout.track_row, parent, false);
                holder.baseLayout = convertView.findViewById(R.id.baseLayout);
                holder.thumb = (ImageView) convertView.findViewById(R.id.listitem_image);
                holder.trackName = (TextView) convertView.findViewById(R.id.listitem_album);
                holder.albumName = (TextView) convertView.findViewById(R.id.listitem_artist);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.baseLayout.setSelected(position == currentSelected);
            holder.baseLayout.setActivated(position == currentSelected);
            //load data into the ViewHolder

            ParcelableTrack track = getItem(position);

            //put a placeholder if the thumbnail is missing (using Picasso)

            if (!TextUtils.isEmpty(track.getThumbnailURL())) {
                Picasso.with(TopTenActivity.this).load(track.getThumbnailURL()).into(holder.thumb);
            } else {
                Picasso.with(TopTenActivity.this).load(R.drawable.image_placeholder).into(holder.thumb);
            }

            holder.trackName.setText(track.getName());
            holder.albumName.setText(track.getAlbumName());

            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {

            //prevent a NullPointerException with a new empty ArrayList if the SpotifyWrapper return null

            if (tracks == null) {
                tracks = new ArrayList<>();
            }
            super.notifyDataSetChanged();
        }

        //class for the ViewHolder pattern

        private class ViewHolder {
            View baseLayout;
            ImageView thumb;
            TextView trackName, albumName;
        }
    }

    private class PlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(MediaService.ACTION_BROADCAST) && tracks != null) {
                int index = tracks.indexOf(intent.getParcelableExtra(MediaService.EXTRA_TRACK));
                if (currentSelected != index) {
                    currentSelected = index;
                    topTenList.setSelection(currentSelected);
                    adapter.notifyDataSetChanged();
                }

            }
        }

    }
}
