package hu.example.jani.ap_all;

import android.database.Cursor;
import android.util.Log;

import java.util.Comparator;

public class QueryObject {
    private String queryTitle;
    private String queryAlbum;
    private String queryArtist;
    private String queryDuration;
    private String filepath;
    private String _id;
    private boolean selected;


    public QueryObject(Cursor cursor) {
        queryTitle = cursor.getString(0);
        queryArtist = cursor.getString(1);
        queryAlbum = cursor.getString(2);
        String origDurationMS = cursor.getString(3);
        int origDurationS = Integer.parseInt(origDurationMS)/1000;
        if (origDurationMS != null) {
            String seconds = String.valueOf(origDurationS % 60);
            String minutes = String.valueOf(origDurationS / 60);
            queryDuration = minutes + ":" + seconds;
        }
        else{
            queryDuration = "-";
        }
        filepath = cursor.getString(4);
        _id = cursor.getString(5);
        selected = false;
    }

    public String getQueryTitle() {
        return queryTitle;
    }

    public String getQueryAlbum() {
        return queryAlbum;
    }

    public String getQueryArtist() {
        return queryArtist;
    }

    public String getQueryDuration() {
        return queryDuration;
    }

    public String getFilepath() {
        return filepath;
    }

    public String get_id() {
        return _id;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    // getter and setter methods

    static class SorterByTitle implements Comparator<QueryObject> {
        int order=-1;
        SorterByTitle(int order){
            this.order=order;
            Log.d("Order", String.valueOf(this.order));
        }
        public int compare(QueryObject ob1,QueryObject ob2){
            if(ob1.getQueryTitle().compareTo(ob2.getQueryTitle())==0) return 0;
            else if(ob1.getQueryTitle().compareTo(ob2.getQueryTitle())<0)
                return order;
            else
                return(-1*order);
        }
    }

    static class SorterByArtist implements Comparator<QueryObject> {
        int order=-1;
        SorterByArtist(int order){
            this.order=order;
        }
        public int compare(QueryObject ob1,QueryObject ob2){
            if(ob1.getQueryArtist().compareTo(ob2.getQueryArtist())==0) return 0;
            else if(ob1.getQueryArtist().compareTo(ob2.getQueryArtist())<0)
                return order;
            else
                return(-1*order);
        }
    }

    static class SorterByAlbum implements Comparator<QueryObject> {
        int order=-1;
        SorterByAlbum(int order){
            this.order=order;
        }
        public int compare(QueryObject ob1,QueryObject ob2){
            if(ob1.getQueryAlbum().compareTo(ob2.getQueryAlbum())==0) return 0;
            else if(ob1.getQueryAlbum().compareTo(ob2.getQueryAlbum())<0)
                return order;
            else
                return(-1*order);
        }
    }

}
