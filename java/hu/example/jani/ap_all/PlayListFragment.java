package hu.example.jani.ap_all;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class PlayListFragment extends Fragment {


    private ArrayList<File> playList = new ArrayList<>();
    PlayListAdapter playListAdapter;
    ListView playListView;
    Context context;
    BroadcastReceiver mMessageReceiver;
    int currentTrack;

    interface IFragmentReadyListener{
        void onPlayListFragmentReady();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context=context;
        Log.d("Ciklus", "fragment - onattach");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Ciklus", "fragment - oncreate");
//        if (savedInstanceState != null) {
//            // Restore last state
//            playList = (ArrayList<File>) savedInstanceState.getSerializable("playList");
//        }
        playListAdapter = new PlayListAdapter(context, R.layout.text_view_playlist_item, playList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("Ciklus", "fragment - oncreateview");
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        playListView = (ListView) rootView.findViewById(R.id.listview_playlist);
        playListView.setAdapter(playListAdapter);

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                Log.d("Adas", "NEW_FILE_LIST_RECEIVED - adás megjött" );
                switch (intent.getAction()) {
                    //local broadcast sent by main activity if a new file list has been received
                    // from file lister fragment
                    case "UPDATE_FILE_LIST":
                        Log.d("Fajllist", "broadcast fogadva");
                        ArrayList<File> list = (ArrayList<File>) intent.getSerializableExtra("New Files");
                        currentTrack = intent.getIntExtra("Current Track", 0);
                        if (list != null) {
                            playList.clear();
                            playList.addAll(list);
                            if (playListAdapter != null) {
                                //We need to update adapter's list as well in order to view change
                                // playListAdapter.clearList();
                                //playListAdapter.addAll(playList);
                                //Toast.makeText(context, "Play list size after broadcast processed: "+ Integer.toString(playList.size()),
                                //        Toast.LENGTH_LONG).show();
                                playListAdapter.notifyDataSetChanged();
                                playListAdapter.updatePlayListCurrentTrack(currentTrack);
                                playListView.smoothScrollToPosition(currentTrack);
                            }
                        }
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("UPDATE_FILE_LIST");
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, intentFilter);
        Log.d("Fajllist", "broadcast receiver REGISZTRÁLVA");

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((IFragmentReadyListener) context).onPlayListFragmentReady();
        Log.d("Ciklus", "fragment - onstart");
        //Setting to receive local broadcast messages for communication and data exchange

    }

//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putSerializable("playList", playList);
//        Log.d("Ciklus", "fragment - onsaveinstacestate");
//    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("Ciklus", "fragment - onpause");
    }

    @Override
    public void onStop() {
        Log.d("Ciklus", "fragment - onstop");
        super.onStop();
    }


    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
          Log.d("Ciklus", "fragment - ondestroy");
        super.onDestroy();
    }

}
