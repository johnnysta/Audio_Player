package hu.example.jani.ap_all;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hu.example.jani.ap_all.MainActivity.Status.INITIAL_NO_FILES;
import static hu.example.jani.ap_all.MainActivity.Status.INITIAL_WITH_FILES;
import static hu.example.jani.ap_all.MainActivity.Status.PAUSED;
import static hu.example.jani.ap_all.MainActivity.Status.PAUSED_BY_INCOMING_CALL;
import static hu.example.jani.ap_all.MainActivity.Status.PLAYING;

public class MainActivity extends AppCompatActivity implements
        FileListerFragment.IFileListReadyListener, PlayListFragment.IFragmentReadyListener,
            IncomingCallBR.Listener {


    private boolean PlayListFragmentToBeUpdated =true;
    private boolean playListFragmentReady = false;


    //In case of incoming call, play is paused
    @Override
    public void onIncomingCall() {
        if (playerStatus==PLAYING )
        {
            playerStatus=PAUSED_BY_INCOMING_CALL;
            invalidateOptionsMenu();
            myMPService.pauseMusic(true);
            Toast.makeText(this, "Bejövő hívás - zene leállítva!", Toast.LENGTH_LONG).show();
        }
    }

    //In case of incoming call ends, play is resumed
    @Override
    public void onCallEnded() {
        if (playerStatus==PAUSED_BY_INCOMING_CALL)
        {
            playerStatus=PLAYING;
            invalidateOptionsMenu();
            myMPService.playMusic(playListInMain, looping, currentTrack);
            Toast.makeText(this, "Hívás vége - zene folytatódik!", Toast.LENGTH_LONG).show();
        }
    }

    //Implementation of interface IFragmentReadyListener, we have to wait until play list fragment
    // ready to receive broadcast from media_player_service with the current play list data in case
    // of activity revoked from service
    @Override
    public void onPlayListFragmentReady() {
        Log.d("Ciklus", "onPlayListFragmentReady***");
        playListFragmentReady = true;
        //If fragment is ready and Fragment update flag is true, we can send broadcast.
        if (PlayListFragmentToBeUpdated) {
            //Toast.makeText(MainActivity.this, "Sending broadcast after PlayListFragmentReady",
           //         Toast.LENGTH_LONG).show();
            sendFileListBroadcastMessage();
            PlayListFragmentToBeUpdated=false;
        }
    }


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 321;

    ArrayList<File> playListInMain = new ArrayList<>();

    private int currentTrack;
    private boolean looping;

    String strPLFilename;

    PlayListFragment playListFragment;
    FileListerFragment fileListerFragment;

    //Instance of Media Player Service
    private MediaPlayerService myMPService;
    private boolean serviceBound = false;

    //Media Player statuses that can exist:
    public enum Status {
        INITIAL_WITH_FILES, INITIAL_NO_FILES, PLAYING, PAUSED,
        PAUSED_BY_INCOMING_CALL, STOPPED
    };

    Status playerStatus;

    BroadcastReceiver mMessageReceiver;


    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MyLocalBinder binder = (MediaPlayerService.MyLocalBinder) service;
            myMPService = binder.getService();
            serviceBound = true;

            //If an instance of the service already exists, we get the current values from it,
            // and set the correct display, of buttons and playlist
            //In case service already has a play list, get values for variables from the service:
            //if (myMPService.getPlayList() != null) {
            //}
            //Play settings can be queried from service even if there is no play list:
            ArrayList<File> temp2 = new ArrayList<>();
            temp2 = myMPService.getPlayList();
            if (temp2!=null) {
                playListInMain.clear();
                playListInMain.addAll(temp2);
            }
            currentTrack = myMPService.getCurrentTrack();
            playerStatus = myMPService.getPlayerStatus();
            looping = myMPService.getLooping();
            Log.d("Ciklus", "onServiceConnected***");
            //If fragment is ready, we can send broadcast, but if not, just set a flag, and
            // wait until fragment ready. This case broadcast
            // will be triggered from onPlayListFragmentReady().
            if (playListFragmentReady){
//                Toast.makeText(MainActivity.this, "Sending broadcast after service connection, PlayListFragment already Ready",
//                        Toast.LENGTH_LONG).show();
                sendFileListBroadcastMessage();
                }
                else {
                PlayListFragmentToBeUpdated = true;
            }
            //sendFileListBroadcastMessage();
            //playListFragment = (PlayListFragment) ((SectionsPagerAdapter)mViewPager.getAdapter()).getItem(0);
            //playListFragment.onUpdateReceived(playListInMain, currentTrack);
            //Then refresh all the UI control elements (this calls onCreatOptionsMenu():
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    //If file list arrives from file lister fragment files to be added to play list
    @Override
    public void onFileListReady(ArrayList<QueryObject> queryList) {
         for (QueryObject item : queryList) {
            if (item.isSelected()) {
                playListInMain.add(new File(item.getFilepath()));
            }
        }
        if (playListInMain.size()==0)
            playerStatus= INITIAL_NO_FILES;
        if (playListInMain.size()!=0 && playerStatus== INITIAL_NO_FILES)
            playerStatus= INITIAL_WITH_FILES;

        invalidateOptionsMenu();

        myMPService.setPlayList(playListInMain);
        myMPService.setPlayerStatus(playerStatus);
        myMPService.setCurrentTrack(currentTrack);

        if (playListInMain.size() > 0){
            //Az alábbi sor megbolondítja a programot, létrejön egy új playlist fragmens, nem a meglévőt vszi elő az adapter, de az fragmens
            // életciklusfüggvényei nem hívódnak meg!
            //PlayListFragment playListFragment2 = (PlayListFragment) ((SectionsPagerAdapter)mViewPager.getAdapter()).getItem(0);
            //playListFragment.onUpdateReceived(playListInMain, currentTrack);
//            Toast.makeText(MainActivity.this, "Sending broadcast after files added to play list",
//                    Toast.LENGTH_LONG).show();
            sendFileListBroadcastMessage();
            //switching to playlist tab
            mViewPager.setCurrentItem(0);
       //     sendFileListBroadcastMessage();
        }
    }

    //Broadcast to update play list from service
    public void sendFileListBroadcastMessage()
    {
        Log.d("Fajllist", "broadcastküldőben");
        Log.d("Ciklus", "broadcastküldőben***");
        Intent intent = new Intent();
        intent.setAction("UPDATE_FILE_LIST");
        // You can also include some extra data.
        intent.putExtra("New Files", playListInMain);
        intent.putExtra("Current Track", currentTrack);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Ciklus", "main - oncerate");
        setContentView(R.layout.activity_main);
        //Handling permissions:
        if(checkAndRequestPermissions()){
            //all permission was granted without explicitly asking, because of eralier app launches
            codeIfPermissionsOK();
        }else{
            //already requires permissions by calling requestPermissions() in checkAndRequestPermissions()
        }


        //Receive local broadcast messages from service
        // if track changed, or end of playing non-looping list:
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                switch (intent.getAction()) {
                    //local broadcast sent by service if a track has been finished
                    // and a new one is coming
                    case "CURRENT_TRACK_CHANGED":
                        // Get extra data included in the Intent
                        currentTrack = intent.getIntExtra("Current track", 0);
                        String playStatus = intent.getStringExtra("Player status");
                        switch (playStatus) {
                            case "PLAYING":
                                playerStatus = PLAYING;
                                break;
                            case "INITIAL_WITH_FILES":
                                playerStatus = INITIAL_WITH_FILES;
                        }
                        invalidateOptionsMenu();
                        //Ez a broadcast valami miatt többször küldődik el.. ellenőrizni kell még
                        //Toast.makeText(MainActivity.this, "Sending broadcast at new track started or end of list.",
                        //        Toast.LENGTH_LONG).show();
                        sendFileListBroadcastMessage();
                        break;
                    //local broadcast sent by adapter, if order in list has been changed
                    case "PLAY_LIST_CHANGED":
                        playListInMain = (ArrayList<File>) intent.getSerializableExtra("Play List");
                        myMPService.setPlayList(playListInMain);
                        currentTrack = intent.getIntExtra("Current track", 0);
                        myMPService.setCurrentTrack(currentTrack);
                        myMPService.setIndex(currentTrack);
                        invalidateOptionsMenu();
                        ///displayPlayList();
                        break;
                }
            }
        };


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CURRENT_TRACK_CHANGED");
        intentFilter.addAction("PLAY_LIST_CHANGED");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, intentFilter);


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Ciklus", "main - onstart");
        //Starts service
        startService(new Intent(this, MediaPlayerService.class));
        //Connecting to service
        bindService(new Intent(this, MediaPlayerService.class),
                myConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Ciklus", "main - onresume");
        //sendFileListBroadcastMessage();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("Ciklus", "main - onsaveinstance");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Ciklus", "main - onstop");
       if (myMPService != null) {
            unbindService(myConnection);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private boolean checkAndRequestPermissions() {
        int storage_read_permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int storage_write_permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int phonestate_permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (storage_read_permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (storage_write_permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (phonestate_permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();

                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                            ) {
                        Log.d("ENGED", "camera & location services permission granted");

                        // here you can do your logic all Permission Success Call
                        codeIfPermissionsOK();

                    } else {
                        //Not all permissions were granted, but "Never ask again" was not selected, so we display some explanation and ask again..
                        Log.d("ENGED", "Some permissions are not granted, ask again ");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                            showDialogOK("Some Permissions are required for All Audio Player:" +
                                            " To read and write storage for play audio and save play lists," +
                                            " and to check phone state for pause playing during incoming calls.)",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    finish();
                                                    dialog.dismiss();
                                                    break;
                                            }
                                        }
                                    });
                        } else {
                            //"Never ask again" was selected, but permission is need to run app, user can set permission only in app settings (if possible):
                            explain("You need to give some mandatory permissions to continue." +
                                    " To read and write storage for play audio and save play lists," +
                                    " and to check phone state for pause playing during incoming calls. " +
                                    "Do you want to go to app settings?");
                        }
                    }
                }
            }
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private void explain(String msg) {
        final android.support.v7.app.AlertDialog.Builder dialog = new android.support.v7.app.AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Utils.startInstalledAppDetailsActivity(MainActivity.this);
                        Toast.makeText(MainActivity.this, "App is quitting now, please restart it, after permission settings are enabled...",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        dialog.create().dismiss();
                        finish();
                    }
                });
        dialog.show();
    }


    public void codeIfPermissionsOK(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        }


    //2 methods for options menu
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem itemPrev = menu.findItem(R.id.icPrev);
        MenuItem itemPlay = menu.findItem(R.id.icPlay);
        MenuItem itemPause = menu.findItem(R.id.icPause);
        MenuItem itemNext = menu.findItem(R.id.icNext);
        MenuItem itemRepeat = menu.findItem(R.id.icRepeat);

        itemRepeat.setChecked(looping);


        switch (playerStatus) {
            case INITIAL_WITH_FILES:
                itemPlay.setVisible(true);
                //itemStop.setVisible(false);

                if (currentTrack == playListInMain.size()-1 && !looping)
                {
                    itemNext.setVisible(false);
                }
                else
                {
                    itemNext.setVisible(true);
                }

                if (currentTrack == 0 && !looping)
                {
                    itemPrev.setVisible(false);
                }
                else
                {
                    itemPrev.setVisible(true);
                }
                break;

            case INITIAL_NO_FILES:
                Log.d("Allapot", "INITIAL_NO_FILES");
                itemPlay.setVisible(false);
                //itemStop.setVisible(false);
                itemNext.setVisible(false);
                itemPrev.setVisible(false);
                break;

            case PLAYING:
                itemPlay.setVisible(false);
                itemPause.setVisible(true);

                //itemStop.setVisible(true);

                if (currentTrack == playListInMain.size()-1 && !looping)
                {
                    itemNext.setVisible(false);
                }
                else
                {
                    itemNext.setVisible(true);
                }

                if (currentTrack == 0 && !looping)
                {
                    itemPrev.setVisible(false);
                }
                else
                {
                    itemPrev.setVisible(true);
                }
                break;

            case PAUSED:
                itemPlay.setVisible(true);
                itemPause.setVisible(false);
                //itemStop.setVisible(true);

                if (currentTrack == playListInMain.size()-1 && !looping)
                {
                    itemNext.setVisible(false);
                }
                else
                {
                    itemNext.setVisible(true);
                }

                if (currentTrack == 0 && !looping)
                {
                    itemPrev.setVisible(false);
                }
                else
                {
                    itemPrev.setVisible(true);
                }
                break;

            case PAUSED_BY_INCOMING_CALL:
                itemPlay.setVisible(true);
                itemPause.setVisible(false);
                //itemStop.setVisible(true);
                if (currentTrack == playListInMain.size()-1 && !looping)
                {
                    itemNext.setVisible(false);
                }
                else
                {
                    itemNext.setVisible(true);
                }

                if (currentTrack == 0 && !looping)
                {
                    itemPrev.setVisible(false);
                }
                else
                {
                    itemPrev.setVisible(true);
                }
                break;

            case STOPPED:
                itemPlay.setVisible(true);
                itemPause.setVisible(false);
                if (currentTrack == playListInMain.size()-1 && !looping)
                {
                    itemNext.setVisible(false);
                }
                else
                {
                    itemNext.setVisible(true);
                }

                if (currentTrack == 0 && !looping)
                {
                    itemPrev.setVisible(false);
                }
                else
                {
                    itemPrev.setVisible(true);
                }
                break;
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.icRepeat:
            {
                looping = !looping;
                myMPService.setLooping(looping);
                invalidateOptionsMenu();
                break;
            }

            case R.id.mnuOpenPlayList:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Play list file to open:");
                final ArrayList<String> files = getFilesList();


                if (files!= null && files.size()>0) {
                    final Spinner spnrFilelist = new Spinner(MainActivity.this);
                    spnrFilelist.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                            android.R.layout.simple_spinner_item, files));
                    builder.setView(spnrFilelist);

                    // Set up the buttons
                    builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openPL(files.get((int) spnrFilelist.getSelectedItemId()));
                            initAfterNewPlayList();
                        }
                    });
                }
                else {
                    final TextView tvNoFilesMessage = new TextView(MainActivity.this);
                    tvNoFilesMessage.setText("\n There are no playlist files to open.");
                    builder.setView(tvNoFilesMessage);
                }

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                break;
            }

            case R.id.mnuSavePlayList:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("File name for play list:");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        strPLFilename = input.getText().toString();
                        savePL(strPLFilename);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                break;

            }
            case R.id.mnuSettings:
            {
                break;
            }
            case R.id.mnuExit:
            {
                if (myMPService!=null)
                {
                    myMPService.stopSelf();
                }
                //setListener(null);
                finish();
                break;
            }

            case R.id.icPrev:
            {
                myMPService.stopMusic();
                currentTrack=myMPService.getCurrentTrack();

                if (currentTrack==0)
                {if (looping ) {currentTrack=playListInMain.size()-1;} }
                else
                {currentTrack--;}
                playerStatus= PLAYING;
                invalidateOptionsMenu();
                myMPService.playMusic(playListInMain, looping, currentTrack);

                break;
            }
            case R.id.icPlay:
            {
                if (playerStatus!=PLAYING )
                {
                    playerStatus=PLAYING;
                    invalidateOptionsMenu();
                    myMPService.playMusic(playListInMain, looping, currentTrack);
                }
                else
                {
                    playerStatus=PAUSED;
                    invalidateOptionsMenu();
                    myMPService.pauseMusic(false);
                }
                break;
            }
            case R.id.icNext:
            {
                Log.d("új PL mérete1:", String.valueOf(playListInMain.size()));
                myMPService.stopMusic();
                currentTrack=myMPService.getCurrentTrack();
                if (currentTrack==playListInMain.size()-1)
                {if (looping ) {currentTrack=0;} }
                else
                {currentTrack++;}
                playerStatus=PLAYING;
                invalidateOptionsMenu();
                Log.d("új PL mérete2:", String.valueOf(playListInMain.size()));
                myMPService.playMusic(playListInMain, looping, currentTrack);
                break;
            }
            case R.id.icPause:
            {
                if (playerStatus!=PLAYING )
                {
                    playerStatus=PLAYING;
                    invalidateOptionsMenu();
                    myMPService.playMusic(playListInMain, looping, currentTrack);
                }
                else
                {
                    playerStatus=PAUSED;
                    invalidateOptionsMenu();
                    myMPService.pauseMusic(false);
                }
                break;
            }
            //Optional stop button:
           /* case R.id.icStop:
            {
                playerStatus=STOPPED;
                invalidateOptionsMenu();
                myMPService.stopMusic();
            }*/
        }
        return super.onOptionsItemSelected(item);
    }



    public void savePL(String filename){
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                // create a file in Music directory
                FileOutputStream fos =
                        new FileOutputStream(
                                new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_MUSIC), filename+".pl")
                        );
                Log.d("utvonal_ment", (Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC)).getPath() +
                        (Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MUSIC)).getName());
                ObjectOutputStream os = new ObjectOutputStream(fos);
                os.writeObject(playListInMain);
                os.close();
                Toast.makeText(this, "File list '"+filename+"' has been saved.", Toast.LENGTH_LONG).show();
            } catch(Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "File list could not been saved.", Toast.LENGTH_LONG).show();
            }
        }

    }


    public void openPL(String filename){
        try {
            // create a file in Music directory
            FileInputStream fis =
                    new FileInputStream(
                            new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MUSIC), filename)
                    );
            ObjectInputStream is = new ObjectInputStream(fis);
            ArrayList<File> temp3 = new ArrayList<File>();
            temp3  = (ArrayList<File>) is.readObject();
            is.close();
            //Check whether readObject returned null:
            if (temp3 != null){
                this.playListInMain.clear();
                this.playListInMain.addAll(temp3);
            }
            else {
                this.playListInMain.clear();
                Log.d("openPL", "temp3 is null");
            }

        } catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Error opening file '"+filename+"'!", Toast.LENGTH_LONG).show();
        }


    }

    ArrayList<String> getFilesList(){
        File[] files;
        ArrayList<String> filelist;
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC);

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".pl")) return true;
                else return false;
            }
        };

        //files[i].isDirectory() || files[i].getName().endsWith(".mp3")
        try {
            files = path.listFiles(filter);
            filelist = new ArrayList<String>(0);
            for (int i = 0; i<files.length; i++){
                filelist.add(files[i].getName());
            }
            return filelist;
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Error reading file list!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    //Todos when a new playlist was opened
    public void initAfterNewPlayList(){
        if (myMPService != null) {
            myMPService.stopMusic();
        }
        currentTrack=0;
        Log.d("PL - iAfterNewPlayList:", String.valueOf(playListInMain.size()));
//        Toast.makeText(MainActivity.this, "Sending broadcast after new playlist opened",
//                Toast.LENGTH_LONG).show();
        sendFileListBroadcastMessage();
        if (playListInMain.size()>0)
        {
            playerStatus=INITIAL_WITH_FILES;
            invalidateOptionsMenu();
        }
        else
        {
            playListInMain.clear();
            currentTrack=0;
            playerStatus=INITIAL_NO_FILES;
            invalidateOptionsMenu();
        }
        myMPService.setPlayList(playListInMain);
        myMPService.setPlayerStatus(playerStatus);
        myMPService.setCurrentTrack(currentTrack);

    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    Log.d("Ciklus", "playlist fragment létrejött");
                    playListFragment = new PlayListFragment();
                    return playListFragment;
                case 1:
                    Log.d("Ciklus", "fajllist fragment létrejött");
                    fileListerFragment = new FileListerFragment();
                    return fileListerFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }

}
