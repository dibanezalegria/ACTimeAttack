package com.pbluedotsoft.actimeattack;

import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pbluedotsoft.actimeattack.data.LapContract.LapEntry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<Cursor>, TrackConfigDialog.DialogPositiveListener {

    private final static String LOG = MainActivity.class.getSimpleName();

    private static final int LAPTIME_LOADER = 0;    /* Identifier for Cursor Loader */

    private String AC_SERVER_IP;                /* AC Server */
    private static final int PORT = 9996;       /* AC UDP port */
    private WifiManager.MulticastLock mLock;    /* Allows app to receive Wifi multicast packets */
    private DatagramSocket mSocket;

    private AlertDialog mInfoDialog;

    private ToggleButton pauseToggleBtn;
    private boolean mAppOn;         /* Pause flag for PacketHandlerAsyncTask */
    private boolean mHSKready;      /* Handshake finished, client/server are connected */
    private PacketParser mParser;   /* Used by PacketHandlerAsyncTask */
    private Timer mTimer;           /* Timer runs the TimeTask that runs the AsyncTasks */
    private int mConnectionTries;   /* Handshake tries counter */

    private ListView mSessionListView, mDatabaseListView;
    private TextView mCarTV, mTrackTV, mTopSpeedTV, mSpeedTV, mLaptimeTV, mLastlapTV,
            mBestlapTV, mRecordlapTV;

    private String mActualTrack, mActualCar;
    private int mRecord;
    private float mTopSpeed;
    private int mCurLap;     /* Current lap number */

    private List<Lap> mSessionLapList;

    private TrackCursorAdapter mTrackCursorAdapter;

    private boolean mSoundOn;   /* Flag connected to option's menu sound checkbox */


//    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(LOG, "CREATE");

//        if (savedInstanceState != null) {
//            mActualTrack = savedInstanceState.getString("actualTrack");
//            Log.d(LOG, "Restoring state: " + mActualTrack);
//        }

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTrackTV = findViewById(R.id.track_tv);
        mCarTV = findViewById(R.id.car_tv);
        mTopSpeedTV = findViewById(R.id.topSpeed_tv);

        mSpeedTV = findViewById(R.id.speed_tv);
        mLaptimeTV = findViewById(R.id.laptime_tv);
        mLastlapTV = findViewById(R.id.lastlap_tv);
        mBestlapTV = findViewById(R.id.bestlap_tv);
        mRecordlapTV = findViewById(R.id.recordlap_tv);

        mTopSpeedTV.setTypeface(Typeface.MONOSPACE);
        mSpeedTV.setTypeface(Typeface.MONOSPACE);
        mLaptimeTV.setTypeface(Typeface.MONOSPACE);
        mLastlapTV.setTypeface(Typeface.MONOSPACE);
        mBestlapTV.setTypeface(Typeface.MONOSPACE);
        mRecordlapTV.setTypeface(Typeface.MONOSPACE);

        // Initialize variables
        mCurLap = -1;
        mRecord = Integer.MAX_VALUE;
        mHSKready = false;
        mConnectionTries = 0;

        // Session laptime list
        mSessionLapList = new ArrayList<>();
        mSessionListView = findViewById(R.id.session_list_view);
        mSessionListView.setEmptyView(findViewById(R.id.session_empty_list_view));

        // Database Cursor Loader
        mTrackCursorAdapter = new TrackCursorAdapter(getApplicationContext(), null);
        mDatabaseListView = findViewById(R.id.database_list_view);
        mDatabaseListView.setAdapter(mTrackCursorAdapter);
        mDatabaseListView.setEmptyView(findViewById(R.id.database_empty_list_view));
        getSupportLoaderManager().initLoader(LAPTIME_LOADER, null, this);

        // Restart button
        Button restartBtn = findViewById(R.id.restart_btn);
        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//               new RestartAppAsyncTask().execute();   // Soft reset (dismiss connection with AC)
                restart();
            }
        });

        // Pause button
        pauseToggleBtn = findViewById(R.id.pause_toggle_button);
        pauseToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mAppOn = !mAppOn;
            }
        });
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        if (mActualTrack != null) {
//            outState.putString("actualTrack", mActualTrack);
//        }
//        Log.d(LOG, "Saving state: " + mActualTrack);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        Log.d(LOG, "RESUME");
        // Set Default Preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSoundOn = sharedPref.getBoolean(getResources().getString(R.string.pref_sound_key), true);
        AC_SERVER_IP = sharedPref.getString(getResources().getString(R.string.pref_ip_key), getResources().getString(R.string.pref_ip_default));
        int udpRate = Integer.parseInt(sharedPref.getString(getResources().getString(R.string
                        .pref_udp_key), getResources().getString(R.string.pref_udp_default)));
        udpRate = (udpRate < 10 || udpRate > 50) ? 15 : udpRate;

        //
        // Coming back from edit database activity? -----------------------> SKIP Network Setup
        //
        if (mSocket != null) {
            mAppOn = true;
            return;
        }

        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return;
        }

        // Network is OK

        mLock = wifiManager.createMulticastLock("lock");

        try {
            mSocket = new DatagramSocket(PORT);
            mSocket.setBroadcast(true);
            mAppOn = true;
        } catch (IOException ex) {
            mSocket = null;
            ex.printStackTrace();
            showInfoDialog(R.string.error_connecting);
            return;
        }

        // Connect to AC server
        new HandshakeAsyncTask().execute();

        // ToggleButton ON
        pauseToggleBtn.setChecked(true);

        // Start packet receiver on client
        mParser = new PacketParser();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mAppOn && mActualTrack != null)
                    new PacketHandlerAsyncTask().execute();  // AsyncTask class
            }
        };
        mTimer = new Timer();
        // Lower period better sync with game but too low might crash slow devices (high cpu usage)
        mTimer.schedule(timerTask, 0, udpRate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mInfoDialog != null) {
            mInfoDialog.dismiss();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Log.d(LOG, "DESTROY");
    }


    /**
     * Update database (and record if laptime faster than previous or first lap for combo car/track)
     */
    public void updateDatabase(int laptime) {
        String selection = LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                LapEntry.COLUMN_LAP_CAR + " LIKE ?";
        String[] selArgs = new String[]{mActualTrack, mActualCar};
        Cursor cursor = getContentResolver().query(LapEntry
                        .CONTENT_URI,
                null,
                selection,
                selArgs,
                null);

        ContentValues values = new ContentValues();
        values.put(LapEntry.COLUMN_LAP_TRACK, mActualTrack);
        values.put(LapEntry.COLUMN_LAP_CAR, mActualCar);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int recordDB = cursor.getInt(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME));
            if (laptime > 0 && laptime < recordDB) {
                values.put(LapEntry.COLUMN_LAP_TOP_SPEED, (int)mTopSpeed);
                values.put(LapEntry.COLUMN_LAP_TIME, laptime);
                mRecord = laptime;
                mRecordlapTV.setText(Lap.format(mRecord));
                // Beep sound
                if (mSoundOn) {
                    new ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                            .startTone(ToneGenerator.TONE_PROP_BEEP2);
                }
            }
            int nlaps = cursor.getInt(cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS));
            values.put(LapEntry.COLUMN_LAP_NLAPS, nlaps + 1);
            getContentResolver().update(LapEntry.CONTENT_URI, values, selection, selArgs);
//            Log.d(LOG, "Database UPDATE");
        } else {
            values.put(LapEntry.COLUMN_LAP_TOP_SPEED, mTopSpeed);
            values.put(LapEntry.COLUMN_LAP_TIME, laptime);
            values.put(LapEntry.COLUMN_LAP_NLAPS, 1);
            getContentResolver().insert(LapEntry.CONTENT_URI, values);
            mRecord = laptime;
            mRecordlapTV.setText(Lap.format(mRecord));
//            Log.d(LOG, "Database INSERT");
        }

        // Inform DBCursor is laptime better than fastest car on this track
        if (mRecord < TrackCursorAdapter.getLapFastestCar()) {
            TrackCursorAdapter.setLapFastestCar(mRecord);
        }

        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }


    /**
     * Checks network and returns WifiManager object
     *
     * @return WifiManager object
     */
    private WifiManager getWifiManager() {
        // Network
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
//            Log.d(LOG, "Connectivity manager error");
            showInfoDialog(R.string.error_connecting);
            return null;
        }

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
//            Log.d(LOG, "No network detected");
            showInfoDialog(R.string.error_connecting);
            return null;
        }

        // WiFi
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
//            Log.d(LOG, "No WiFi detected");
            showInfoDialog(R.string.error_connecting);
            return null;
        }

        return wifiManager;
    }



    /**
     * Menu method implementation
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_mainl file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sound = sharedPref.getBoolean(getResources().getString(R.string.pref_sound_key),
                true);
        MenuItem soundItem = menu.findItem(R.id.sound_cb);
        soundItem.setChecked(sound);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.sound_cb:
                mSoundOn = !item.isChecked();
                item.setChecked(mSoundOn);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.pref_sound_key), mSoundOn);
                editor.apply();
                if (mSoundOn) {
                    Toast.makeText(getApplicationContext(), "Sound ON", Toast
                            .LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(), "Sound OFF", Toast
                            .LENGTH_SHORT)
                            .show();
                }
//                Log.d(TAG, "mSound: " + mSoundOn);
                return true;

            case R.id.action_edit_database:
                // Pause packet processing async task
                mAppOn = false;
                Intent intentDB = new Intent(this, DBActivity.class);
                startActivity(intentDB);
                return true;

            case R.id.action_settings:
                // Pause packet processing async task
                mAppOn = false;
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                return true;
            case R.id.action_help: {
                showInfoDialog(R.string.help);
                return true;
            }
            case R.id.action_about:
                showInfoDialog(R.string.about);
                return true;
            case R.id.exit_app:
                this.finishAffinity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Helps restart loader after handshake when actual track is available.
     */
    public void restartLoader() {
//        Log.d(LOG, "RESTART Loader");
        getSupportLoaderManager().restartLoader(LAPTIME_LOADER, null, this);
    }

    /**
     * CURSOR LOADER methods
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selArgs = null;
        if (mActualTrack != null) {
            selection = LapEntry.COLUMN_LAP_TRACK + " LIKE ?";
            selArgs = new String[] {mActualTrack};
        }
        switch (id) {
            case LAPTIME_LOADER:
                return new CursorLoader(this,
                        LapEntry.CONTENT_URI,
                        null,
                        selection,
                        selArgs,
                        LapEntry.COLUMN_LAP_TIME + " ASC");
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LAPTIME_LOADER:
                if (mActualTrack != null) {
                    mTrackCursorAdapter.swapCursor(data);
                    // Inject laptime for fastest car on this track in TrackCursorAdapter
                    Cursor cursor = getContentResolver().query(LapEntry.CONTENT_URI,
                            null,
                            LapEntry.COLUMN_LAP_TRACK + " LIKE ?",
                            new String[] {mActualTrack},
                            LapEntry.COLUMN_LAP_TIME + " ASC");

                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int laptime = cursor.getInt(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME));
                        TrackCursorAdapter.setLapFastestCar(laptime);
                    }
                    if (cursor != null && !cursor.isClosed())
                        cursor.close();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LAPTIME_LOADER:
                mTrackCursorAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * Restart app.
     */
    private void restart() {
        // Restart app (code from stack overflow)
        Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context
                .ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }


    /**
     * INNER AsyncTask Classe
     */
    public class PacketHandlerAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private DatagramPacket packet;
        private byte[] message;

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!mHSKready || !mAppOn) {
                return false;
            }

            mLock.acquire();
            message = new byte[1000];
            packet = new DatagramPacket(message, message.length);
//            Log.d(LOG, "Listening...");

            try {
                mSocket.receive(packet);
//                Log.d(LOG, "Response from AC (packet size): " + packet.getLength());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mLock.isHeld())
                    mLock.release();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean validPacket) {
            if (!validPacket) {
                return;
            }

            mParser.parse(packet.getData());

            if (mParser.speed > mTopSpeed) {
                mTopSpeed = mParser.speed;
            }

            mSpeedTV.setText(String.format(Locale.ENGLISH, "%.0f", mParser.speed));
            mTopSpeedTV.setText(String.format(Locale.ENGLISH, "%.0f", mTopSpeed));
            mLaptimeTV.setText(Lap.format(mParser.lapTime));
            mLastlapTV.setText(Lap.format(mParser.lastLap));
            mBestlapTV.setText(Lap.format(mParser.bestLap));

            // Passing finish line?
            if (mCurLap != mParser.lapCount) {
                // Sometimes lastLap is a few ms (garbage data) when starting practice
                if (mCurLap >= 0 && mParser.lastLap > 10000) {
                    // Check if same or higher lap number already in list (user restarted session)
                    for (Lap lap : mSessionLapList) {
                        if (lap.getLapNum() >= mParser.lapCount) {
                            // Clear list
                            mSessionLapList.clear();
                            break;
                        }
                    }
                    // Insert new laptime in session list
                    mSessionLapList.add(0, new Lap(mParser.lapCount, mParser.lastLap));
                    Lap[] lapArr = mSessionLapList.toArray(new Lap[mSessionLapList.size()]);
                    SessionLapAdapter adapter = new SessionLapAdapter(getApplicationContext(), lapArr);
                    adapter.setBestLap(mParser.bestLap);
                    mSessionListView.setAdapter(adapter);
                    // Update database
                    updateDatabase(mParser.lastLap);
                }

                mTopSpeed = 0;
                mCurLap = mParser.lapCount;
            }
        }
    }



    /**
     * Dialogs interface method implementation.
     *
     * @param selectedItem
     */
    @Override
    public void onPositiveClick(String selectedItem) {
        mActualTrack = selectedItem;
        mTrackTV.setText(mActualTrack);

        // Inject actual car in TrackCursorAdapter
        TrackCursorAdapter.setActualCar(mActualCar);

        // Call method in outer class to restart loader
        restartLoader();

        // Get record for car/track combo from DB
        Cursor cursor = getContentResolver().query(LapEntry.CONTENT_URI,
                null,
                LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                        LapEntry.COLUMN_LAP_CAR + " LIKE ?",
                new String[]{mActualTrack, mActualCar},
                null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            mRecord = cursor.getInt(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME));
            mRecordlapTV.setText(Lap.format(mRecord));
        }

        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }



    /**
     * INNER AsyncTask Classe
     */
    public class HandshakeAsyncTask extends AsyncTask<Void, Void, Boolean> {
        // Type of update required from the server
        private static final int SUBSCRIBE_UPDATE = 1;
        private static final int SUBSCRIBE_SPOT = 2;

        private byte[] identifier, version, operationId, hand;
        private HShakeParser hsParser;

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Prepare handshake packet
            identifier = intToLittleEndian(1);
            version = intToLittleEndian(1);
            operationId = intToLittleEndian(0);

            hand = appendByteArray(identifier, version);
            hand = appendByteArray(hand, operationId);

            mLock.acquire();
            try {
                // Contact AC Server
//                Log.d(LOG, "HandshakeAsyncTask->doInBackground AC_SERVER_IP: " + AC_SERVER_IP);
                InetAddress server = InetAddress.getByName(AC_SERVER_IP);
                DatagramPacket packet = new DatagramPacket(hand, hand.length, server,
                        PORT);
                mSocket.send(packet);

                // AC Server responds
                byte[] message = new byte[4096];
                packet = new DatagramPacket(message, message.length);
                mSocket.receive(packet);
                hsParser = new HShakeParser();
                hsParser.parse(packet.getData());

                // Client confirms connection
                operationId = intToLittleEndian(SUBSCRIBE_UPDATE);
                hand = appendByteArray(identifier, version);
                hand = appendByteArray(hand, operationId);
                packet = new DatagramPacket(hand, hand.length, server,
                        PORT);
                mSocket.send(packet);
                Log.d(LOG, "Client has been added as a listener to AC server.");
                return true;

            } catch (SocketException e) {
                Log.d(LOG, "Socket Error:", e);
            } catch (IOException e) {
                Log.d(LOG, "IO Error:", e);
            } finally {
                if (mLock.isHeld())
                    mLock.release();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean validPacket) {
            if (!validPacket) {
                return;
            }

            // packet identifier should be 4242 and version 1
            if (hsParser.identifier == 4242 && hsParser.version == 1) {
//                Log.d(LOG, "Connected");
                mHSKready = true;
            } else {
                if (mConnectionTries > 100) {
                    showInfoDialog(R.string.error_connecting);
                } else {
                    mConnectionTries++;
//                    Log.d(LOG, "Trying...");
                    new HandshakeAsyncTask().execute();
                }
                return;
            }

            String location = hsParser.trackName;   // One location has track variations
            mActualCar = hsParser.carName;
            mTrackTV.setText(mActualTrack);
            mCarTV.setText(mActualCar);

            // Get all tracks with variations
            String[] trackConfig = TrackLib.getTrackConfig(location);

            // mActualTrack is not null if coming back from DBActivity (restored via savedInstance)
            if (mActualTrack != null) {
                onPositiveClick(mActualTrack);

            } else if (trackConfig.length == 1) {
                onPositiveClick(trackConfig[0]);

            } else {
                FragmentManager manager = getFragmentManager();
                TrackConfigDialog dialog = new TrackConfigDialog();
                Bundle bundle  = new Bundle();
                bundle.putInt("position", 0);   // Selected item's index
                bundle.putStringArray("trackConfig", trackConfig);
                dialog.setArguments(bundle);
                dialog.show(manager, "track_config_dialog");
            }
        }
    }


//    public class RestartAppAsyncTask extends AsyncTask<Void, Void, Boolean> {
//        // Type of update required from the server
//        private static final int DISMISS_COMMUNICATION = 3;
//
//        private byte[] identifier, version, operationId, hand;
//
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            // Pause async task
//            mAppOn = false;
//
//            // Stop timer running packet reader task
//            mTimer.cancel();
//            mTimer.purge();
//
//            // Prepare handshake
//            identifier = intToLittleEndian(1);
//            version = intToLittleEndian(1);
//            operationId = intToLittleEndian(0);
//
//            hand = appendByteArray(identifier, version);
//            hand = appendByteArray(hand, operationId);
//
//            mLock.acquire();
//            try {
//                // Dismiss communication with AC server
//                InetAddress server = InetAddress.getByName(AC_SERVER_IP);
//                operationId = intToLittleEndian(DISMISS_COMMUNICATION);
//                hand = appendByteArray(identifier, version);
//                hand = appendByteArray(hand, operationId);
//                DatagramPacket packet = new DatagramPacket(hand, hand.length, server,
//                        PORT);
//                mSocket.send(packet);
//                mHSKready = false;
//                return true;
//
//            } catch (SocketException e) {
//                Log.d(LOG, "Socket Error:", e);
//            } catch (IOException e) {
//                Log.d(LOG, "IO Error:", e);
//            } finally {
//                if (mLock.isHeld())
//                    mLock.release();
//            }
//
//            return false;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean validPacket) {
//            if (!validPacket) {
//                return;
//            }
//
//            Log.d(LOG, "Client has been REMOVED as a listener from AC server. Restarting...");
//
//            restart();
//        }
//    }


    /**
     * Shows HTML text in an Information Dialog.
     */
    private void showInfoDialog(int messageID) {
        if (mInfoDialog != null) {
            mInfoDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style
                .MyDialogTheme);
        // fromHtml deprecated for Android N and higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(getString(messageID),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            builder.setMessage(Html.fromHtml(getString(messageID)));
        }
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing
            }
        });
        builder.create();

        mInfoDialog = builder.show();
    }


    /**
     * Help methods
     */
    public static byte[] appendByteArray(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public byte[] intToLittleEndian(int data) {
        byte[] b = new byte[4];
        b[0] = (byte) data;
        b[1] = (byte) ((data >> 8) & 0xFF);
        b[2] = (byte) ((data >> 16) & 0xFF);
        b[3] = (byte) ((data >> 24) & 0xFF);
        return b;
    }



}
