package com.crea3d.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crea3d.spotifystreamer.data.ParcelableArtist;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends ActionBarActivity {

    private static String OUTSTATE_ARTIST_LIST = "outstateList";
    private static String OUTSTATE_SEARCH_TEXT = "outstateSearch";

    private EditText search;
    private ListView artistsList;
    private TextView emptyText;
    private View loadingView;

    private SpotifyApi spotifyApi;
    private SpotifyService spotify;

    private ArrayList<ParcelableArtist> artists = null;
    private ArtistsAdapter adapter;

    int requestSentCount = 0;
    private boolean restoringStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spotifyApi = new SpotifyApi();
        spotify = spotifyApi.getService();

        //set layouts

        search = (EditText) findViewById(R.id.search);
        artistsList = (ListView) findViewById(R.id.artist_listview);
        emptyText = (TextView) findViewById(R.id.artist_emptytext);
        loadingView = findViewById(R.id.loading_view);

        //recovering state

        if (savedInstanceState != null) {
            artists = savedInstanceState.getParcelableArrayList(OUTSTATE_ARTIST_LIST);
            restoringStatus = true; // prevent the TextWatcher from firing while recovering state
            search.setText(savedInstanceState.getString(OUTSTATE_SEARCH_TEXT));
        }

        if (artists == null) {
            artists = new ArrayList<>();
        }

        adapter = new ArtistsAdapter();
        artistsList.setAdapter(adapter);
        artistsList.setEmptyView(emptyText);

        //bind events

        artistsList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (restoringStatus) {
                    restoringStatus = false;
                }
                else {
                    startSearch(s.toString());
                }
            }
        });

        artistsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //get the ParcelableArtist from the adapter and put as extra in the intent

                Intent intent = new Intent(MainActivity.this, TopTenActivity.class);
                intent.putExtra(TopTenActivity.EXTRA_ARTIST, adapter.getItem(position));
                startActivity(intent);
            }
        });
    }

    private void startSearch(final String s) {

        //show loader and start the asyncronous request

        showLoader();
        spotify.searchArtists(s.toString(), new Callback<ArtistsPager>() {
            @Override
            public void success(final ArtistsPager artistsPager, Response response) {

                //check if activity is still running

                if (isFinishing()) {
                    return;
                }

                //refresh the adapter data in the UiThread and hide the loader

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoader();
                        artists = new ArrayList<ParcelableArtist>();
                        for (Artist artist : artistsPager.artists.items) {
                            artists.add(new ParcelableArtist(artist));
                        }
                        adapter.notifyDataSetChanged(s.toString());
                    }
                });
            }

            @Override
            public void failure(final RetrofitError error) {

                //check if activity is still running

                if (isFinishing()) {
                    return;
                }

                //show an alert with the RetrofitErrorMessage (only if the search wasn't empty)

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoader();
                        if (s.length() == 0) {
                            artists = new ArrayList<>();
                            adapter.notifyDataSetChanged("");
                        } else {
                            Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    //activity methods

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(OUTSTATE_ARTIST_LIST, artists);
        outState.putString(OUTSTATE_SEARCH_TEXT, search.getText().toString());

        super.onSaveInstanceState(outState);
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

    private class ArtistsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return artists.size();
        }

        @Override
        public ParcelableArtist getItem(int position) {
            return artists.get(position);
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
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.artists_row, parent, false);
                holder.thumb = (ImageView) convertView.findViewById(R.id.listitem_image);
                holder.name = (TextView) convertView.findViewById(R.id.listitem_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            //load data into the ViewHolder

            ParcelableArtist artist = getItem(position);

            //put a placeholder if the thumbnail is missing (using Picasso)

            if (!TextUtils.isEmpty(artist.getThumbnailURL())) {
                Picasso.with(MainActivity.this).load(artist.getThumbnailURL()).into(holder.thumb);
            } else {
                Picasso.with(MainActivity.this).load(R.drawable.image_placeholder).into(holder.thumb);
            }

            holder.name.setText(artist.getName());

            return convertView;
        }

        public void notifyDataSetChanged(String s) {

            //prevent a NullPointerException with a new empty ArrayList if the SpotifyWrapper return null

            if (artists == null) {
                artists = new ArrayList<>();
            }

            //set the emptyText for the artist ListView

            emptyText.setText(TextUtils.isEmpty(s) ? getString(R.string.start_search) : getString(R.string.no_artists_found, s));
            if(artists.size() == 0 && !TextUtils.isEmpty(s)){

                //if the search is not empty and the request return 0 results (or null) show a toast to inform the user

                Toast.makeText(MainActivity.this, getString(R.string.no_artists_found, s), Toast.LENGTH_SHORT).show();
            }
            super.notifyDataSetChanged();
        }

        //class for the ViewHolder pattern

        private class ViewHolder {
            ImageView thumb;
            TextView name;

        }
    }


}
