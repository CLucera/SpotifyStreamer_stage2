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

    //constructor from the Spotify API Track

    public ParcelableTrack(Track track)
    {
        name = track.name;
        albumName = track.album.name;

        List<Image> images = track.album.images;
        if(images != null && images.size() > 0) {
           thumbnailURL = images.get(0).url;
        } else {
           thumbnailURL = "";
        }

        previewUrl = track.preview_url;
    }

    //constructor for the Parcelable interface

    public ParcelableTrack(Parcel in)
    {
        String[] data = new String[3];

        in.readStringArray(data);
        name = data[0];
        albumName = data[1];
        thumbnailURL = data[2];
        previewUrl = data[3];

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
    } //to be used in the Spotify Wrapper part 2

    //Parcelable interface methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {name, albumName, thumbnailURL, previewUrl});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ParcelableTrack createFromParcel(Parcel in) {
            return new ParcelableTrack(in);
        }
        public ParcelableTrack[] newArray(int size) {
            return new ParcelableTrack[size];
        }
    };
}
