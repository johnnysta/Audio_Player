package hu.example.jani.ap_all;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

public class MusicListAdapter extends ArrayAdapter {
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    ArrayList<QueryObject> list;

    public MusicListAdapter(@NonNull Context context, int resource, ArrayList<QueryObject> musicList) {
        super(context, resource);
        this.layoutResource=resource;
        this.layoutInflater=LayoutInflater.from(context);
        this.list=musicList;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int item) {
        return list.get(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull final ViewGroup parent) {

        ViewHolder viewHolder;

        // Inflate the rowlayout_file_list.xml file if convertView is null
        if (convertView == null) {
            convertView = layoutInflater.inflate(layoutResource, parent, false);
            viewHolder = new ViewHolder(convertView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }



        //Log.d("Sorok szama", Integer.toString(list.size()));
//        for (QueryObject object: list) {
//            Log.d("Sorok adapterben", Boolean.toString(object.isSelected()));
//        }

        QueryObject currentObject = list.get(position);
        viewHolder.artist.setText(currentObject.getQueryArtist());
        viewHolder.album.setText(currentObject.getQueryAlbum());
        viewHolder.title.setText(currentObject.getQueryTitle());
        //turning off change listener for the time setting the check box based on list data
        // (later i'll set a custom listener)
        viewHolder.selected.setOnCheckedChangeListener(null);
        viewHolder.selected.setTag(Integer.valueOf(position));
        viewHolder.selected.setChecked(currentObject.isSelected());

        // viewHolder.duration.setText(currentObject.getQueryDuration());

        // custom listener for check box
        viewHolder.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int actualPosition = (Integer) buttonView.getTag();
                QueryObject actualObject = list.get(actualPosition);
                actualObject.setSelected(isChecked);
                list.set(actualPosition, actualObject);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        final TextView artist;
        final TextView album;
        final TextView title;
        final CheckBox selected;
        //final TextView duration;

        public ViewHolder(View view) {
            this.artist = (TextView) view.findViewById(R.id.artist);
            this.album = (TextView) view.findViewById(R.id.album);
            this.title = (TextView) view.findViewById(R.id.title);
            this.selected = (CheckBox) view.findViewById(R.id.rowSelector);
            //this.duration = (TextView) view.findViewById(R.id.duration);
        }
    }


}
