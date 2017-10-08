package br.ufpe.cin.if710.podcast.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastDBHelper;
import br.ufpe.cin.if710.podcast.db.PodcastProvider;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.ui.adapter.XmlFeedAdapter;
import br.ufpe.cin.if710.podcast.util.DownloadService;

import static java.security.AccessController.getContext;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    private final String RSS_FEED = "http://leopoldomt.com/if710/fronteirasdaciencia.xml";
    //TODO teste com outros links de podcast

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
        new DownloadXmlTask().execute(RSS_FEED);
    }


    protected void onResume(){
        super.onResume();
        IntentFilter intent = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadCompletedEvent, intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        XmlFeedAdapter adapter = (XmlFeedAdapter) items.getAdapter();
        if(adapter!=null) adapter.clear();
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, List<ItemFeed>> {
        @Override
        protected void onPreExecute() {
            //Toast.makeText(getApplicationContext(), "iniciando...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<ItemFeed> doInBackground(String... params) {
            List<ItemFeed> itemList = new ArrayList<>();
            try {
                itemList = XmlFeedParser.parse(getRssFeed(params[0]));
                saveItems(mContext, itemList);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return itemList;
        }

        @Override
        protected void onPostExecute(List<ItemFeed> feed) {
            //Toast.makeText(getApplicationContext(), "terminando...", Toast.LENGTH_SHORT).show();

            feed = readItems();

            //Adapter Personalizado
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, feed);

            //atualizar o list view
            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);

        }
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

    public String getNameUri(String s){
        return s.substring(s.lastIndexOf('/'), s.length());
    }

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


    public void saveItems(Context context, List<ItemFeed> list){

        for(ItemFeed i : list){
            //Log.d("list", list.size()+"");
            //Cursor aux = dbHelper.getWritableDatabase().query(PodcastDBHelper.DATABASE_TABLE,PodcastDBHelper.columns, null, new String[] {}, null, null, null);

            Cursor aux = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                    PodcastProviderContract.EPISODE_LINK+"=?", new String[]{i.getLink()}, null);

            //Cursor aux2 = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,"", null, null);

            Log.d("link", i.getLink()+"");
            Log.d("title", i.getTitle()+"");
            Log.d("date", i.getPubDate()+"");
            Log.d("description", i.getDescription()+"");
            Log.d("download", i.getDownloadLink()+"");


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
                Log.d("diretorio", Environment.DIRECTORY_DOWNLOADS+getNameUri(i.getDownloadLink()));
                //podcastProvider.insert(PodcastProviderContract.EPISODE_LIST_URI, c);
                //dbHelper.getWritableDatabase().insert(PodcastDBHelper.DATABASE_TABLE, null, c);

            }else{
                Log.d("teste", "Episódio já salvo!");
            }
        }
    }

    //TODO Opcional - pesquise outros meios de obter arquivos da internet
    private String getRssFeed(String feed) throws IOException {
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
