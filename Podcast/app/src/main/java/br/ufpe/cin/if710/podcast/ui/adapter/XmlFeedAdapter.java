package br.ufpe.cin.if710.podcast.ui.adapter;

import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.services.MusicPlayerService;
import br.ufpe.cin.if710.podcast.ui.EpisodeDetailActivity;
import br.ufpe.cin.if710.podcast.ui.MainActivity;
import br.ufpe.cin.if710.podcast.services.DownloadService;

import static android.content.Context.NOTIFICATION_SERVICE;

public class XmlFeedAdapter extends ArrayAdapter<ItemFeed> {

    int linkResource;

    public XmlFeedAdapter(Context context, int resource, List<ItemFeed> objects) {
        super(context, resource, objects);
        linkResource = resource;
    }

    /**
     * public abstract View getView (int position, View convertView, ViewGroup parent)
     * <p>
     * Added in API level 1
     * Get a View that displays the data at the specified position in the data set. You can either create a View manually or inflate it from an XML layout file. When the View is inflated, the parent View (GridView, ListView...) will apply default layout parameters unless you use inflate(int, android.view.ViewGroup, boolean) to specify a root view and to prevent attachment to the root.
     * <p>
     * Parameters
     * position	The position of the item within the adapter's data set of the item whose view we want.
     * convertView	The old view to reuse, if possible. Note: You should check that this view is non-null and of an appropriate type before using. If it is not possible to convert this view to display the correct data, this method can create a new view. Heterogeneous lists can specify their number of view types, so that this View is always of the right type (see getViewTypeCount() and getItemViewType(int)).
     * parent	The parent that this view will eventually be attached to
     * Returns
     * A View corresponding to the data at the specified position.
     */


	/*
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.itemlista, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.item_title);
		textView.setText(items.get(position).getTitle());
	    return rowView;
	}
	/**/

    //http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    static class ViewHolder {
        TextView item_title;
        TextView item_date;
        Button button;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final ItemFeed itemFeed = getItem(position);


        if (convertView == null) {
            convertView = View.inflate(getContext(), linkResource, null);
            holder = new ViewHolder();
            holder.item_title = (TextView) convertView.findViewById(R.id.item_title);
            holder.item_date = (TextView) convertView.findViewById(R.id.item_date);
            convertView.setTag(holder);
            holder.button = convertView.findViewById(R.id.item_action);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.item_title.setText(itemFeed.getTitle());
        holder.item_date.setText(itemFeed.getPubDate());


        if(itemAlreadyDownloaded(getItem(position))){
            holder.button.setText("Ouvir");
            holder.button.setBackgroundColor(Color.MAGENTA);
        }else{
            holder.button.setText("Baixar");

        }


        holder.item_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), EpisodeDetailActivity.class);
                intent.putExtra(PodcastProviderContract.TITLE, itemFeed.getTitle());
                intent.putExtra(PodcastProviderContract.EPISODE_LINK, itemFeed.getLink());
                intent.putExtra(PodcastProviderContract.DESCRIPTION, itemFeed.getDescription());
                intent.putExtra(PodcastProviderContract.DOWNLOAD_LINK, itemFeed.getDownloadLink());
                getContext().startActivity(intent);
            }
        });


        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //se ainda nao foi feito download
                if(!itemAlreadyDownloaded(itemFeed)){
                    ((Button) view).setText("Baixando...");

                    //Toast.makeText(getContext(), "Botao da posicao "+position, Toast.LENGTH_LONG).show();
                    Intent i = new Intent(getContext(),DownloadService.class);
                    //passando link pro getData do service
                    i.setData(Uri.parse(itemFeed.getDownloadLink()));
                    i.putExtra("linkPosition", itemFeed.getDownloadLink());
                    getContext().startService(i);

                    final Intent notificationIntent = new Intent(getContext(), MainActivity.class);
                    final PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);

                    final Notification notification = new Notification.Builder(
                            getContext())
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setOngoing(false).setContentTitle("Download em andamento!")
                            .setContentText("O arquivo estará disponível em instantes...")
                            .setContentIntent(pendingIntent).build();

                    NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(1, notification);

                    //caso ja tenha sido feito o download
                }else{
                    if(holder.button.getText().equals("Pausar")){
                        holder.button.setText("Ouvir");
                    }else{
                        holder.button.setText("Pausar");
                    }

                    Intent intent = new Intent(getContext(), MusicPlayerService.class);
                    intent.putExtra("linkPosition", itemFeed.getDownloadLink());
                    getContext().startService(intent);
                }

            }
        });


/**
 *
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Posicao "+position, Toast.LENGTH_LONG).show();
            }
        });
*/
        return convertView;
    }


    public boolean itemAlreadyDownloaded(ItemFeed itemFeed){
        Cursor cursor = getContext().getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                PodcastProviderContract.EPISODE_LINK+"=?", new String[]{itemFeed.getLink()}, null);
        cursor.moveToNext();
        String v = cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_DOWNLOADED));
        return Boolean.parseBoolean(v);
    }

}