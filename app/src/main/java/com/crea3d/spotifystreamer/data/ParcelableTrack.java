package com.crea3d.spotifystreamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by clucera on 01/06/15.
 */
public class ParcelableTrack implements Parcelable {

    private String name;
    private String albumName;
    private String thumbnailURL;
    private String previewUrl;
    private String externalUrl;
    private long durationMillis;


    //constructor from the Spotify API Track

    public ParcelableTrack(Track track)
    {
        name = track.name;
        albumName = track.album.name;
        durationMillis = track.duration_ms;

        List<Image> images = track.album.images;
        if(images != null && images.size() > 0) {
           thumbnailURL = images.get(0).url;
        } else {
           thumbnailURL = "";
        }

        previewUrl = track.preview_url;
        if(track.external_urls != null && track.external_urls.containsKey("spotify")){
            externalUrl = track.external_urls.get("spotify");
        } else {
            externalUrl = "";
        }
    }

    //constructor for the Parcelable interface

    public ParcelableTrack(Parcel in)
    {
        String[] data = new String[5];
        long[] longData = new long[1];

        in.readStringArray(data);
        name = data[0];
        albumName = data[1];
        thumbnailURL = data[2];
        previewUrl = data[3];
        externalUrl = data[4];

        in.readLongArray(longData);
        durationMillis = longData[0];
    }

    //get methods

    public String getName()
    {
        return name;
    }

    public String getAlbumName()
    {
        return albumName;
    }

    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    public String getPreviewUrl()
    {
        return previewUrl;
    }

    public long getDurationMillis()
    {
        return durationMillis;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    //Parcelable interface methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {name, albumName, thumbnailURL, previewUrl, externalUrl});
        dest.writeLongArray(new long[] {durationMillis});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ParcelableTrack createFromParcel(Parcel in) {
            return new ParcelableTrack(in);
        }
        public ParcelableTrack[] newArray(int size) {
            return new ParcelableTrack[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o != null && (o instanceof ParcelableTrack &&  previewUrl.equals(((ParcelableTrack) o).previewUrl));
    }
}
