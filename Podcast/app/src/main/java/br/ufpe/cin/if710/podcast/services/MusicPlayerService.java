package br.ufpe.cin.if710.podcast.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;

import static java.security.AccessController.getContext;

/**
 * Created by albert on 11/10/17.
 */

public class MusicPlayerService extends Service {
    private final String TAG = "MusicPlayerNoBindingService";

    public static String COMPLETE_LISTENED = "COMPLETE_LISTENED";

    private static MediaPlayer mPlayer;
    private int mStartID;
    private String itemLink;

    public static MediaPlayer getInstance(){
        if(mPlayer==null){
            mPlayer = new MediaPlayer();
        }
        return mPlayer;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // configurar media player
        //Nine Inch Nails Ghosts I-IV is licensed under a Creative Commons Attribution Non-Commercial Share Alike license.
        mPlayer = getInstance();

        //nao deixa entrar em loop
        mPlayer.setLooping(false);
        Log.d("service", "ENTREI NO ON CREATE");
        // encerrar o service quando terminar a musica
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // encerra se foi iniciado com o mesmo ID
                Intent completlyListened = new Intent(COMPLETE_LISTENED);
                completlyListened.putExtra("linkPosition", itemLink);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(completlyListened);
                stopSelf(mStartID);
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != mPlayer) {

            // ID para o comando de start especifico
            mStartID = startId;
            itemLink = intent.getStringExtra("linkPosition");

            /**/
            //se ja esta tocando...
            if (mPlayer.isPlaying()) {
                int currentTime = mPlayer.getCurrentPosition();
                //Atualiza a posicao em que parou
                ContentValues contentValues = new ContentValues();
                contentValues.put(PodcastProviderContract.EPISODE_TIME, currentTime+"");
                getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI,contentValues,
                        PodcastProviderContract.DOWNLOAD_LINK +"=?", new String[]{itemLink});
                stopSelf(mStartID);
            }
            else {
                // inicia musica
                //Pega tempo da ultima execucao
                Log.d("service", "INICIANDO MUSICA");
                try {
                    mPlayer.setDataSource(getApplicationContext(), Uri.parse(getUri(itemLink)));
                    mPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mPlayer.seekTo(getTime(itemLink));
                mPlayer.start();
            }
        }
        // nao reinicia service automaticamente se for eliminado
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mPlayer) {
            mPlayer.stop();
            mPlayer.release();
        }

    }

    //nao eh possivel fazer binding com este service
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        //return null;
    }


    public String getUri(String link){
        Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                PodcastProviderContract.DOWNLOAD_LINK+"=?", new String[]{link}, null);
        cursor.moveToNext();
        String v = cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_URI));
        Log.d("uri", v);
        return v;
    }

    public int getTime(String link){
        Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                PodcastProviderContract.DOWNLOAD_LINK+"=?", new String[]{link}, null);
        cursor.moveToNext();
        String v = cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_TIME));
        return v !=null? Integer.parseInt(v) : 0;
    }
}
