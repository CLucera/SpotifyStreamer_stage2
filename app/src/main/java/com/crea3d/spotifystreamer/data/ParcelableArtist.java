package com.crea3d.spotifystreamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.crea3d.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by clucera on 01/06/15.
 */
public class ParcelableArtist implements Parcelable{

    String spotifyId;
    String name;
    String thumbnailURL;

    //constructor from the Spotify API Artist

    public ParcelableArtist(Artist artist)
    {
        spotifyId = artist.id;
        name = artist.name;

        List<Image> images = artist.images;
        if(images != null && images.size() > 0) {
           thumbnailURL = images.get(0).url;
        } else {
           thumbnailURL = "";
        }
    }

    //constructor for the Parcelable interface

    public ParcelableArtist(Parcel in)
    {
        String[] data = new String[3];
        in.readStringArray(data);

        spotifyId = data[0];
        name = data[1];
        thumbnailURL = data[2];
    }

    //get methods

    public String getSpotifyId()
    {
        return spotifyId;
    }

    public String getName()
    {
        return name;
    }

    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    //Parcelable interface methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {spotifyId, name, thumbnailURL});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ParcelableArtist createFromParcel(Parcel in) {
            return new ParcelableArtist(in);
        }
        public ParcelableArtist[] newArray(int size) {
            return new ParcelableArtist[size];
        }
    };
}
