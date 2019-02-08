package hu.example.jani.ap_all;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

public class FileListerFragment extends Fragment implements View.OnClickListener {

    public interface IFileListReadyListener
    {
        void onFileListReady(ArrayList<QueryObject> queryList);
    }

    IFileListReadyListener fileListReadyListener;

    // QueryObject list
    ArrayList<QueryObject> queryList = new ArrayList<>();
    Context context;
    Cursor cursor;
    boolean permissionOk  = false;
    ListView musicListView;
    MusicListAdapter musicListAdapter;
    Button sortByArtistBtn, sortByTitleBtn, sortByAlbumBtn, toPlayListBtn;
    CheckBox allSelector;


    enum SortBy {ALBUM_ASC, ALBUM_DESC, TITLE_ASC, TITLE_DESC, ARTIST_ASC, ARTIST_DESC}
    SortBy currentSort;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context=context;
        try {
            fileListReadyListener = (IFileListReadyListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_list, container, false);

        musicListView = (ListView) rootView.findViewById(R.id.listview);
        sortByArtistBtn = (Button) rootView.findViewById(R.id.artistBtn);
        sortByTitleBtn = (Button) rootView.findViewById(R.id.titleBtn);
        sortByAlbumBtn = (Button) rootView.findViewById(R.id.albumBtn);
        toPlayListBtn = (Button) rootView.findViewById(R.id.toPlayListButton);
        allSelector = (CheckBox) rootView.findViewById(R.id.allSelector);

        //Setting the icon indicating sort criteria and order
        sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
        sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
        sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);

        sortByArtistBtn.setOnClickListener(this);
        sortByTitleBtn.setOnClickListener(this);
        sortByAlbumBtn.setOnClickListener(this);
        toPlayListBtn.setOnClickListener(this);

        // Get Cursor
        cursor = populateQueries(context);
        queryList = getList();

        //Sort the list by title - default
        currentSort = SortBy.TITLE_ASC;
        Collections.sort(queryList, new QueryObject.SorterByTitle(-1));
        musicListAdapter = new MusicListAdapter(context,
                R.layout.rowlayout_file_list, queryList);
        musicListView.setAdapter(musicListAdapter);


        allSelector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    for (QueryObject object: queryList) {
                        object.setSelected(true);
                    }
                }
                else {
                    for (QueryObject object: queryList) {
                        object.setSelected(false);
                    }
                }
                musicListAdapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }


    public ArrayList<QueryObject> getList(){
        if (cursor != null) {
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                queryList.add(new QueryObject(cursor));
            }
        }
        return queryList;
    }


    public Cursor populateQueries(Context context) {

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,    // filepath of the audio file
                MediaStore.Audio.Media._ID,     // context id/ uri id of the file
        };

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE);

        // the last parameter sorts the data alphanumerically

        return cursor;
    }



    @Override
    public void onClick(final View v) { //check for what button is pressed
        Log.d("Teszt", Integer.toString(v.getId()));
        switch (v.getId()) {
            case R.id.titleBtn:
                if(currentSort==SortBy.TITLE_ASC)
                {

                    currentSort= SortBy.TITLE_DESC;
                    sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_up_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByTitle(1));

                }
                else if (currentSort == SortBy.TITLE_DESC)
                {
                    currentSort= SortBy.TITLE_ASC;
                    sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByTitle(-1));
                }
                else {
                    sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    currentSort= SortBy.TITLE_ASC;
                    sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByTitle(-1));
                }
                musicListAdapter.notifyDataSetChanged();
                break;
            case R.id.albumBtn:
                if(currentSort==SortBy.ALBUM_ASC)
                {
                    currentSort= SortBy.ALBUM_DESC;
                    sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_up_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByAlbum(1));

                }
                else if (currentSort == SortBy.ALBUM_DESC)
                {
                    currentSort= SortBy.ALBUM_ASC;
                    sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByAlbum(-1));
                }
                else {
                    sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    currentSort= SortBy.ALBUM_ASC;
                    sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByAlbum(-1));
                }
                musicListAdapter.notifyDataSetChanged();
                break;
            case R.id.artistBtn:
                if(currentSort==SortBy.ARTIST_ASC)
                {
                    currentSort= SortBy.ARTIST_DESC;
                    sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_up_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByArtist(1));

                }
                else if (currentSort == SortBy.ARTIST_DESC)
                {
                    currentSort= SortBy.ARTIST_ASC;
                    sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByArtist(-1));
                }
                else {
                    sortByAlbumBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    sortByTitleBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
                    currentSort= SortBy.ARTIST_ASC;
                    sortByArtistBtn.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.icons8_sort_down_24,0);
                    Collections.sort(queryList, new QueryObject.SorterByArtist(-1));
                }
                musicListAdapter.notifyDataSetChanged();
                break;
//                case R.id.allSelector:
//                    allSelector= (CheckBox) v;
//                    if (allSelector.isChecked()){
//                        for (QueryObject object: queryList) {
//                            object.setSelected(true);
//                        }
//                    }
//                    else {
//                        for (QueryObject object: queryList) {
//                            object.setSelected(false);
//                        }
//                    }
//                    musicListAdapter.notifyDataSetChanged();
//                    break;
            case R.id.toPlayListButton:
               // Log.d("Gomb", "katt");
                //int sorok_szama = Log.d("Sorok szama", Integer.toString(queryList.size()));
                fileListReadyListener.onFileListReady(queryList);
                break;
        }
    }
}
