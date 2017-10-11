package br.ufpe.cin.if710.podcast.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.ui.MainActivity;

/**
 * Created by albert on 09/10/17.
 */

public class EpisodeListDownloadService extends IntentService {
    public static final String downloadEpisodeList = "download_episode_List";
    public EpisodeListDownloadService() {
        super("EpisodeListDownloadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("xml", "entrei no service");
        List<ItemFeed> itemList = new ArrayList<>();
        try {
            itemList = XmlFeedParser.parse(MainActivity.getRssFeed(MainActivity.RSS_FEED));
            Log.d("xml", itemList.size()+" itens pegos");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("xml", "vou salvar o item");
        saveItems(getApplicationContext(),itemList);
        //Atualiza a lista
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(downloadEpisodeList));
    }


    public void saveItems(Context context, List<ItemFeed> list){
        Log.d("xml", "entrei saveItems");
        for(ItemFeed i : list){
            //Log.d("list", list.size()+"");
            //Cursor aux = dbHelper.getWritableDatabase().query(PodcastDBHelper.DATABASE_TABLE,PodcastDBHelper.columns, null, new String[] {}, null, null, null);

            Cursor aux = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                    PodcastProviderContract.EPISODE_LINK+"=?", new String[]{i.getLink()}, null);

            //Cursor aux2 = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,"", null, null);

   //         Log.d("link", i.getLink()+"");
  //          Log.d("title", i.getTitle()+"");
 //           Log.d("date", i.getPubDate()+"");
//            Log.d("description", i.getDescription()+"");
//            Log.d("download", i.getDownloadLink()+"");


            //so adiciona itens que nao estão no banco
            if(aux.getCount()<=0){
                ContentValues c = new ContentValues();
                c.put(PodcastProviderContract.TITLE, i.getTitle());
                c.put(PodcastProviderContract.EPISODE_LINK, i.getLink());
                c.put(PodcastProviderContract.DATE, i.getPubDate());
                c.put(PodcastProviderContract.DESCRIPTION, i.getDescription());
                c.put(PodcastProviderContract.DOWNLOAD_LINK, i.getDownloadLink());
                c.put(PodcastProviderContract.EPISODE_URI, Environment.DIRECTORY_DOWNLOADS+getNameUri(i.getDownloadLink()));
                c.put(PodcastProviderContract.EPISODE_TIME, 0);
                getContentResolver().insert(PodcastProviderContract.EPISODE_LIST_URI, c);

                //Log.d("diretorio", Environment.DIRECTORY_DOWNLOADS+getNameUri(i.getDownloadLink()));
                //podcastProvider.insert(PodcastProviderContract.EPISODE_LIST_URI, c);
                //dbHelper.getWritableDatabase().insert(PodcastDBHelper.DATABASE_TABLE, null, c);

            }else{
                Log.d("teste", "Episódio já salvo!");
            }
        }
        Log.d("xml", "terminei de salvar");
    }
    public String getNameUri(String s){
        return s.substring(s.lastIndexOf('/'), s.length());
    }

}
