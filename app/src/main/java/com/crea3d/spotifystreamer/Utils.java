package com.crea3d.spotifystreamer;

import android.content.Context;

/**
 * Created by clucera on 29/06/15.
 */
public class Utils {

    public static boolean isNotificationActiveOnLockScreen(Context context){
        return context.getSharedPreferences(BaseActivity.PREFERENCES, Context.MODE_PRIVATE).getBoolean(BaseActivity.PREFERENCE_NOTIFICATION, true);
    }
}
