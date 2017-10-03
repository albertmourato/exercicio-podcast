package br.ufpe.cin.if710.podcast.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class PodcastProvider extends ContentProvider {
    //PodcastDBHelper dbHelper = PodcastDBHelper.getInstance(getContext()); <- isso dá erro
    PodcastDBHelper dbHelper;
    public PodcastProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        if(verifyUriEpisode(uri)){
            long id = dbHelper.getWritableDatabase().insert(PodcastDBHelper.DATABASE_TABLE, null, values);
            return Uri.withAppendedPath(PodcastProviderContract.EPISODE_LIST_URI, id+"");
        }else{
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public boolean verifyUriEpisode(Uri uri){
        return uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE);
    }

    @Override
    public boolean onCreate() {
        dbHelper = PodcastDBHelper.getInstance(getContext()); //Isso nao da erro :)
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if(verifyUriEpisode(uri)){
            return dbHelper.getWritableDatabase().query(PodcastDBHelper.DATABASE_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        }else{
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if(verifyUriEpisode(uri)){
            return dbHelper.getWritableDatabase().update(PodcastDBHelper.DATABASE_TABLE, values, selection, selectionArgs);
        } else{
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
