package br.ufpe.cin.if710.podcast.util;
import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import br.ufpe.cin.if710.podcast.db.PodcastDBHelper;

public class DownloadService extends IntentService {

    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if710.services.action.DOWNLOAD_COMPLETE";
    public static PodcastDBHelper podcastDBHelper;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onHandleIntent(Intent i) {
        try {
            //checar se tem permissao... Android 6.0+
            Log.d("service", "ENTREI NO SERVICE");
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //File write logic here
                Log.d("service", "VOU FAZER DOWNLOAD");
                File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                root.mkdirs();

                //TODO colocar link no setData
                File output = new File(root, i.getData().getLastPathSegment());
                if (output.exists()) {
                    output.delete();
                }
                Log.d("service", "DEPOIS DO IF");
                URL url = new URL(i.getData().toString());
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                FileOutputStream fos = new FileOutputStream(output.getPath());
                BufferedOutputStream out = new BufferedOutputStream(fos);

                try {
                    InputStream in = c.getInputStream();
                    byte[] buffer = new byte[8192];
                    int len = 0;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
                finally {
                    fos.getFD().sync();
                    out.close();
                    c.disconnect();
                }
                //implicit
                Intent intent = new Intent(DOWNLOAD_COMPLETE);
                //explicit
                //Intent intent = new Intent(getApplicationContext(), GlobalBroadcastReceiver.class);
                //intent.putExtra("itemBaixado", i);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                //sendBroadcast(intent);

                //Toast.makeText(getApplicationContext(), "DOWNLOAD FINALIZADO!", Toast.LENGTH_LONG).show();

            }else{
                Toast.makeText(getApplicationContext(), "Conceda as premissões de armazenamento!", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e2) {
            Log.e(getClass().getName(), "Exception durante download", e2);
        }

    }
}