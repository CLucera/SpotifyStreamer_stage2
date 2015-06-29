package com.crea3d.spotifystreamer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.Toast;

import com.crea3d.spotifystreamer.data.ParcelableArtist;
import com.crea3d.spotifystreamer.data.ParcelableTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by clucera on 28/06/15.
 */
public class BaseActivity extends ActionBarActivity {

    public static final String PREFERENCES = "SpotifyStreamerPreferences";
    private static final String PREFERENCE_COUNTRY = "countryPreference";
    private static final String PREFERENCE_LANGUAGE = "languagePreference";
    public static final String PREFERENCE_NOTIFICATION = "notificationPreferences";


    private SharedPreferences preferences;
    private static List<Locale> locales;
    private static Locale selectedLocale;
    private boolean notificationActive;
    private ParcelableArtist artist;
    private ArrayList<ParcelableTrack> trackList;
    private ParcelableTrack track;
    private boolean isPlaying = false;

    private PlayerReceiver playerReceiver;
    private ShareActionProvider shareActionProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playerReceiver = new PlayerReceiver();

        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        selectedLocale = new Locale(preferences.getString(PREFERENCE_LANGUAGE, Locale.getDefault().getLanguage()), preferences.getString(PREFERENCE_COUNTRY, Locale.getDefault().getCountry()));
        notificationActive = preferences.getBoolean(PREFERENCE_NOTIFICATION, true);

        Comparator countryComparator = new Comparator<Locale>() {
            @Override
            public int compare(Locale lhs, Locale rhs) {
                return lhs.getDisplayCountry().compareTo(rhs.getDisplayCountry());
            }
        };

        //get locales

        Set<Locale> allLocales = new TreeSet<Locale>(countryComparator);

        allLocales.addAll(new ArrayList<Locale>(Arrays.asList(Locale.getAvailableLocales())));

        locales = new ArrayList<>(allLocales);

        Iterator<Locale> iter = locales.iterator();

        //removing empty occurrences

        while (iter.hasNext()) {
            if (TextUtils.isEmpty(iter.next().getCountry())) {
                iter.remove();
            }
        }

        //sort locales

        Collections.sort(locales, new Comparator<Locale>() {
            @Override
            public int compare(Locale lhs, Locale rhs) {
                return lhs.getDisplayCountry().compareTo(rhs.getDisplayCountry());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(playerReceiver, new IntentFilter(MediaService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(playerReceiver);
    }

    public void showLocaleSelection() {
        String[] localeStrings = new String[locales.size()];
        for (int i = 0; i < locales.size(); i++) {
            localeStrings[i] = locales.get(i).getDisplayCountry();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alert_choice_region_title);
        builder.setSingleChoiceItems(localeStrings, Math.max(locales.indexOf(selectedLocale), 0), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedLocale = locales.get(which);
                preferences.edit().putString(PREFERENCE_LANGUAGE, selectedLocale.getLanguage()).commit();
                preferences.edit().putString(PREFERENCE_COUNTRY, selectedLocale.getCountry()).commit();
                refreshLocale();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void showNotificationSelection() {
        String[] notificationStrings = getResources().getStringArray(R.array.alert_choice_notification_array);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alert_choice_notification_title);
        builder.setSingleChoiceItems(notificationStrings, notificationActive ? 0 : 1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                notificationActive = which == 0;
                preferences.edit().putBoolean(PREFERENCE_NOTIFICATION, notificationActive).commit();
                refreshNotifications();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    protected void refreshLocale() {
        // used in subclasses
    }

    protected void refreshNotifications() {
        // used in subclasses
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    public boolean isNotificationActive() {
        return notificationActive;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        if(isPlaying()){
            inflater.inflate(R.menu.menu_main_now_playing, menu);
            shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));
            shareActionProvider.setShareHistoryFileName("spotify_share_history.xml");
        } else {
            inflater.inflate(R.menu.menu_main, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_now_playing:
                Intent playerIntent;
                if(getResources().getBoolean(R.bool.isTablet)){
                    playerIntent = new Intent(this, TopTenActivity.class);
                    playerIntent.putExtra(TopTenActivity.EXTRA_ARTIST, artist);
                    playerIntent.putExtra(TopTenActivity.EXTRA_TRACKS, trackList);
                } else {
                    playerIntent = new Intent(this, PlayerActivity.class);
                    playerIntent.putExtra(PlayerActivity.EXTRA_ARTIST, artist);
                    playerIntent.putExtra(PlayerActivity.EXTRA_TRACKS, trackList);
                    playerIntent.putExtra(PlayerActivity.EXTRA_TRACK, track);
                }
                playerIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(playerIntent);

                return true;
            case R.id.action_share:
                if(track != null && !TextUtils.isEmpty(track.getExternalUrl())) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, track.getExternalUrl());
                    setShareIntent(shareIntent);
                } else {
                    Toast.makeText(this, getString(R.string.share_error), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_region_settings:
                showLocaleSelection();
                return true;
            case R.id.action_notification_settings:
                showNotificationSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setShareIntent(Intent shareIntent){
        if(shareActionProvider != null){
            shareActionProvider.setShareIntent(shareIntent);
        }
    }


    private class PlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MediaService.ACTION_BROADCAST)) {
                isPlaying = intent.getBooleanExtra(MediaService.EXTRA_PLAYING, false);
                artist = intent.getParcelableExtra(MediaService.EXTRA_ARTIST);
                trackList = intent.getParcelableArrayListExtra(MediaService.EXTRA_TRACK_LIST);
                track = intent.getParcelableExtra(MediaService.EXTRA_TRACK);
            }
        }

    }
}


