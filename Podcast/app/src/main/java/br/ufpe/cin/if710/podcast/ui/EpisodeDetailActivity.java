package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;

public class EpisodeDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_detail);

        TextView title = findViewById(R.id.title);
        TextView link = findViewById(R.id.link);
        TextView description = findViewById(R.id.description);
        TextView linkDownload = findViewById(R.id.linkDownload);

        Bundle b = getIntent().getExtras();

        title.setText(b.getString(PodcastProviderContract.TITLE, "errorTitle"));
        link.setText(b.getString(PodcastProviderContract.EPISODE_LINK, "errorLink"));
        description.setText(b.getString(PodcastProviderContract.DESCRIPTION, "errorTitle"));
        linkDownload.setText(b.getString(PodcastProviderContract.DOWNLOAD_LINK, "errorTitle"));

        //TODO preencher com informações do episódio clicado na lista...
    }
}
