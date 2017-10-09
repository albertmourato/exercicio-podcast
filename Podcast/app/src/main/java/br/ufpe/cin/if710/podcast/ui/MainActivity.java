package br.ufpe.cin.if710.podcast.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastDBHelper;
import br.ufpe.cin.if710.podcast.db.PodcastProvider;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.services.EpisodeListDownloadService;
import br.ufpe.cin.if710.podcast.ui.adapter.XmlFeedAdapter;
import br.ufpe.cin.if710.podcast.services.DownloadService;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    public static final String RSS_FEED = "http://leopoldomt.com/if710/fronteirasdaciencia.xml";
    //TODO teste com outros links de podcast

    public static boolean activityRunning = false;

    static ListView items;
    static Button mButtonItem;
    static PodcastDBHelper dbHelper;
    static Uri uriConsumer;
    static PodcastProvider podcastProvider;
    static Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        //PodcastDBHelper.deleteDatabase(this);
        dbHelper = PodcastDBHelper.getInstance(getApplicationContext());
        items = (ListView) findViewById(R.id.items);
        items.setClickable(true);
        if (!(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        setListeners();


    }

    public void setListeners(){

        items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("list", i+"");
                XmlFeedAdapter xmlFeedAdapter = (XmlFeedAdapter) adapterView.getAdapter();
                ItemFeed item = xmlFeedAdapter.getItem(i);
                Intent intent = new Intent(getApplicationContext(), EpisodeDetailActivity.class);
                intent.putExtra(PodcastProviderContract.TITLE, item.getTitle());
                intent.putExtra(PodcastProviderContract.EPISODE_LINK, item.getLink());
                intent.putExtra(PodcastProviderContract.DESCRIPTION, item.getDescription());
                intent.putExtra(PodcastProviderContract.DOWNLOAD_LINK, item.getDownloadLink());
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityRunning = true;
        Intent i = new Intent(this, EpisodeListDownloadService.class);
        startService(i);
    }


    protected void onResume(){
        super.onResume();
        activityRunning = true;
        IntentFilter intent = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadCompletedEvent, intent);

        IntentFilter intent2 = new IntentFilter(EpisodeListDownloadService.downloadEpisodeList);
        LocalBroadcastManager.getInstance(this).registerReceiver(episodeDownloadListEvent, intent2);

    }

    protected void onPause(){
        super.onPause();
        activityRunning = false;
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadCompletedEvent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        activityRunning = false;
        XmlFeedAdapter adapter = (XmlFeedAdapter) items.getAdapter();
        if(adapter!=null) adapter.clear();
    }



    public ArrayList<ItemFeed> readItems(){
        Cursor c =  getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                "", null, null);
        ArrayList <ItemFeed> l = new ArrayList<>();
        c.moveToFirst();
        while(c.moveToNext()){
            String title = c.getString(c.getColumnIndex(PodcastProviderContract.TITLE));
            String link = c.getString(c.getColumnIndex(PodcastProviderContract.EPISODE_LINK));
            String pubDate = c.getString(c.getColumnIndex(PodcastProviderContract.DATE));
            String desc = c.getString(c.getColumnIndex(PodcastProviderContract.DESCRIPTION));
            String downloadLink = c.getString(c.getColumnIndex(PodcastProviderContract.DOWNLOAD_LINK));
            ItemFeed item = new ItemFeed(title, link, pubDate, desc, downloadLink);
            l.add(item);
        }
        return l;
    }

    private BroadcastReceiver episodeDownloadListEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<ItemFeed> feed = readItems();

            //Adapter Personalizado
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, feed);

            //atualizar o list view
            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);
        }
    };

    private BroadcastReceiver downloadCompletedEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<ItemFeed> arrayList = readItems();
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, arrayList);

            //atualizar o list view
            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);

            Toast.makeText(getApplicationContext(), "Itens atualizados!", Toast.LENGTH_SHORT).show();
        }
    };



    //TODO Opcional - pesquise outros meios de obter arquivos da internet
    public static String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }
}
