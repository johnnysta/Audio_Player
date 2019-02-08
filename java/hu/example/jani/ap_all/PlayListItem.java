package hu.example.jani.ap_all;

import java.io.File;

/**
 * Created by Janó on 2017.02.20..
 */

public class PlayListItem {

    File file;
    int playOrderNO;
    private String title;
    private String album;
    private String artist;
    private String duration;

    public PlayListItem(File aFile, int aPlayOrderNO){
        file = new File(aFile.getPath());
        playOrderNO = aPlayOrderNO;
    }


    public PlayListItem(String file, int playOrderNO, String title, String album, String artist, String duration){
        this.file = new File(file);
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration = duration;
        this.playOrderNO = playOrderNO;
    }

    public File getFile(){
        return file;
    }

    public int getPlayOrderNO(){
        return playOrderNO;
    }


}
