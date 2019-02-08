package hu.example.jani.ap_all;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Janó on 2017.02.14..
 */

public class PlayListAdapter extends BaseAdapter {

    ArrayList<File> adapterPlayList;
    //ArrayList<File> adapterPlayList = new ArrayList<>();
    int adapterCurrentTrack = 0;
    private Context context;
    int resource;

    public PlayListAdapter(Context aContext, int itemResource, ArrayList<File> aObjects) {
        super();
        this.adapterPlayList = aObjects;
       // this.adapterPlayList.clear();
        //this.adapterPlayList.addAll(aObjects);
        this.context=aContext;
        this.resource=itemResource;
    }

    void clearList(){
        adapterPlayList.clear();
    }

    void addAll(ArrayList<File> playList){
        adapterPlayList.addAll(playList);
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final File item = this.adapterPlayList.get(position);
        Log.d("Adapter get item", "get item");
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(resource, null);

        TextView textViewFilename = (TextView) itemView.findViewById(R.id.text_view_playlist_item_item);

        ImageButton btRemoveItem = (ImageButton) itemView.findViewById(R.id.buttonRemoveItem);
        String filename = item.getName();

        if (this.adapterCurrentTrack == position)
        {
            SpannableString spanString = new SpannableString(filename);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            textViewFilename.setText(spanString);
            textViewFilename.setTextColor(Color.parseColor("#FF0000"));
            btRemoveItem.setEnabled(false);
        }
        else
        {
            textViewFilename.setText(filename);
            btRemoveItem.setEnabled(true);
        }



        ImageButton btMoveItemUp = (ImageButton) itemView.findViewById(R.id.buttonMoveItemUp);

        if (position == 0) btMoveItemUp.setEnabled(false); else
            btMoveItemUp.setEnabled(true);

        // Cache row position inside the button using `setTag`
        btMoveItemUp.setTag(position);
        // Attach the click event handler
        btMoveItemUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (Integer) view.getTag();
                // Access the row position here to get the correct data item
                if (position != 0) {
                    File prevItem = adapterPlayList.get(position - 1);
                    adapterPlayList.set(position - 1, item);
                    adapterPlayList.set(position, prevItem);
                    if(position==adapterCurrentTrack) adapterCurrentTrack--;
                    else if(position-1==adapterCurrentTrack) adapterCurrentTrack++;
                    PlayListAdapter.this.notifyDataSetChanged();
                    sendBroadcastMessage();
                }
            }
        });


        ImageButton btMoveItemDown = (ImageButton) itemView.findViewById(R.id.buttonMoveItemDown);

        if (position == adapterPlayList.size()-1) btMoveItemDown.setEnabled(false); else
            btMoveItemDown.setEnabled(true);

        // Cache row position inside the button using `setTag`
        btMoveItemDown.setTag(position);
        // Attach the click event handler
        btMoveItemDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (Integer) view.getTag();
                // Access the row position here to get the correct data item
                if (position != adapterPlayList.size()) {
                    File nextItem = adapterPlayList.get(position + 1);
                    adapterPlayList.set(position + 1, item);
                    adapterPlayList.set(position, nextItem);
                    if(position==adapterCurrentTrack) adapterCurrentTrack++;
                    else if(position+1==adapterCurrentTrack) adapterCurrentTrack--;
                    PlayListAdapter.this.notifyDataSetChanged();
                    sendBroadcastMessage();
                }
            }
        });


        // Cache row position inside the button using `setTag`
        btRemoveItem.setTag(position);
        // Attach the click event handler
        btRemoveItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 final int position = (Integer) view.getTag();
                // Access the row position here to get the correct data item

                Snackbar.make(view,"Delete item?", Snackbar.LENGTH_LONG).
                        setAction("Yes", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (adapterCurrentTrack > position) adapterCurrentTrack--;
                                File temp = adapterPlayList.get(position);
                                adapterPlayList.remove(position);
                                PlayListAdapter.this.notifyDataSetChanged();
                                sendBroadcastMessage();
                            }
                        })
                .show();
                }
        });
        return itemView;
    }


    //Via a broadcast message we notify main Activity about play list change (order change/removal)
    public void sendBroadcastMessage()
    {
        Log.d("Teszt", "PLAY_LIST_CHANGED_ADAPTER");
        Intent intent = new Intent();
        intent.setAction("PLAY_LIST_CHANGED");
        // Also include extra data.
        ArrayList<File> temp4  = new ArrayList<File>();
        temp4.addAll(this.adapterPlayList);
        intent.putExtra("Play List", temp4);
        intent.putExtra("Current track", adapterCurrentTrack);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }

    @Override
    public int getCount() {
        return adapterPlayList.size();
    }

    @Override
    public Object getItem(int position) {
        return adapterPlayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    public void updatePlayList(ArrayList<File> newlist) {
        Log.d("PL - Adapter param:", String.valueOf(newlist.size()));
        adapterPlayList.clear();
        Log.d("PL - Adapter törl ut:", String.valueOf(adapterPlayList.size()));
        Log.d("newl - Adapter PL törl", String.valueOf(newlist.size()));
        adapterPlayList.addAll(newlist);
        Log.d("PL - Adapter addall ut:", String.valueOf(adapterPlayList.size()));
        this.notifyDataSetChanged();
    }

    public void updatePlayListCurrentTrack(int current) {
        this.adapterCurrentTrack = current;
        this.notifyDataSetChanged();
    }


    public ArrayList<File> getPlayList() {
        return new ArrayList<File>(adapterPlayList);
    }
}
