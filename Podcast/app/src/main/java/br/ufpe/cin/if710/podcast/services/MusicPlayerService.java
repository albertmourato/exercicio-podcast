package br.ufpe.cin.if710.podcast.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by albert on 11/10/17.
 */

public class MusicPlayerService extends Service {
    public MediaPlayer mediaPlayer;

    public void onCreate(){
        super.onCreate();

        
    }

    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
        //return null;
    }
}
