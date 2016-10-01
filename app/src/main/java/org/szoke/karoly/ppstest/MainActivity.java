package org.szoke.karoly.ppstest;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.szoke.karoly.ppstest.fragment.LoginFragment;
import org.szoke.karoly.ppstest.fragment.MainFragment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PersonalPushServiceTest";
    public static final String DEVICE_PREF = "device_pref";
    public static final String CHANNEL = "channel";
    public static final String DEVICE = "device";
    private static final String BROADCAST_REGISTER = "REGISTER";
    //private static final String SERVICE_URL = "http://192.168.0.6/pps/index.php";
    private static final String SERVICE_URL = "https://pps-szokekaroly.rhcloud.com/index.php";
    //private static final String CHANNEL_URL = "http://192.168.0.6:3000";
    private static final String CHANNEL_URL = "https://ppsnodejs-szokekaroly.rhcloud.com";
    private static final String CHANNEL_EVENT = "pps_channel";
    private static final String REGISTER = "/login/register_device";
    private static final String SEND = "/home/send_by_device";
    private static final String GET_ALL = "/home/get_all_messages";

    private static final String BROADCAST_SEND = "SEND";
    private static final String BROADCAST_GET_ALL = "GETALL";

    private TextView tvEmail;
    private TextView tvPassword;
    private  TextView tvDevice;

    private EditText edMessage;
    private EditText edTitle;

    private Button btLogin;
    private Button btSend;

    private ProgressBar pbLogin;

    private ListView lvMessages;

    private String mChannel;
    private String mDevice;
    private ArrayAdapter mAdapter;
    private ArrayList<String> mMessages;
    private Socket mSocket; //kommunikációs csatorna kliens inicializálása
    {
        try {
            mSocket = IO.socket(CHANNEL_URL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Activity felület létrehozása, inicializálása
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEmail = (TextView) findViewById(R.id.tvEmail);
        tvDevice = (TextView) findViewById(R.id.tvDevice);
        tvPassword = (TextView) findViewById(R.id.tvPassword);

        edMessage = (EditText) findViewById(R.id.edMessage);
        edTitle = (EditText) findViewById(R.id.edTitle);

        btLogin = (Button) findViewById(R.id.btLogin);
        btSend = (Button) findViewById(R.id.btSend);

        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);

        lvMessages = (ListView) findViewById(R.id.lvMessages);
        mMessages = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1, mMessages);
        lvMessages.setAdapter(mAdapter);
        lvMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mAdapter.getItem(position).toString()));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, mAdapter.getItem(position).toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        getPreferences();
        setVisibility();
        getAllMessages();

    }

    /**
     * Külön szálon elindítja az összes üzenet letöltését. Regisztrál egy BroadcastReceivert a letöltés
     * elkészülése eseményre.
     */
    private void getAllMessages() {
        String params;
        if (mChannel != null && mDevice != null) {
            try {
                LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mGetAllMessages, new IntentFilter(BROADCAST_GET_ALL));
                params = "channel=" + URLEncoder.encode(mChannel, "UTF-8") + "&device=" +
                        URLEncoder.encode(mDevice, "UTF-8");
                new AsyncHttpTask(BROADCAST_GET_ALL).execute(SERVICE_URL + GET_ALL, params);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void setFragment(){
        Fragment fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(mDevice!=null){
            fragment = new MainFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
        else {
            fragment = new LoginFragment();
            ((LoginFragment)fragment).setOnLoginButtonClickListener(new LoginFragment.OnLoginButtonClickListener() {
                @Override
                public void onClick(String params) {
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mRegisterReceiver, new IntentFilter(BROADCAST_REGISTER));
                    new AsyncHttpTask(BROADCAST_REGISTER).execute(SERVICE_URL + REGISTER,params);
                }
            });
        }
    }

    /**
     * Attól függően, hogy az eszköz regisztrálva van-e, vagy a regisztrációs részt, vagy az üzenet küldő részt mutatja meg.
     */
    private void setVisibility() {
        //regisztráció elrejtése
        if (mDevice != null) {
            tvEmail.setVisibility(View.GONE);
            tvPassword.setVisibility(View.GONE);
            tvDevice.setVisibility(View.GONE);
            btLogin.setVisibility(View.GONE);
            pbLogin.setVisibility(View.GONE);
            lvMessages.setVisibility(View.VISIBLE);
            edMessage.setVisibility(View.VISIBLE);
            edTitle.setVisibility(View.VISIBLE);
            btSend.setVisibility(View.VISIBLE);
            if ( ! isNetworkAvailable() ) {
                btSend.setEnabled(false);
            }
            //üzenet küldő gomb eseménykezelője, külön szálon tölti fel a szerverre az üzenetet
            btSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edMessage.getText() != null) {
                        String params;
                        try {
                            params = "channel=" + URLEncoder.encode(mChannel, "UTF-8") + "&device=" +
                                    URLEncoder.encode(mDevice, "UTF-8") + "&msg=" + URLEncoder.encode(edMessage.getText().toString(), "UTF-8");
                            new AsyncHttpTask(BROADCAST_SEND).execute(SERVICE_URL + SEND, params);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            Toast.makeText(getBaseContext(),"Csatorna hiba, nem lehet üzenetet küldeni.",Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
            mSocket.on(mChannel, onNewMessage);
            mSocket.connect();
        } else {    //regisztráció

        }
    }

    /**
     * Ellenőrzi, hogy a hálózat elérhető-e?
     * @return TRUE, ha elérhető
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            Toast.makeText(MainActivity.this, "Az internet nem elérhető, kapcsolaja be a hálózatot, és indítsa újra az alkalmazást!", Toast.LENGTH_SHORT).show();
        }
        return  networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Ha a program újra az előtérbe kerül, feliratkozás
     */
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mSendReceiver, new IntentFilter(BROADCAST_SEND));
    }

    /**
     * Ha aprogram háttérbe kerül leíratkozás,  kímélve az erőforrásokat
     */
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mRegisterReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mSendReceiver);
    }

    /**
     * Ha az alkalmazás megsemmisül, bontja a kapcsolatot a kommunikációs csatornával
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSocket.connected()) {
            mSocket.disconnect();
            mSocket.off(mChannel, onNewMessage);
        }
    }

    /**
     * Ez a BroadcastReceiver fogadja az üzenetet, ha a külön szálon indított regisztráció véget ért,
     * és meg van az eredmény. A kapott adatokat permanensen elmenti, és megjeleníti az üzenetküldést.
     */
    private BroadcastReceiver mRegisterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("response");
            JSONObject response;
            try {
                response = new JSONObject(result);
                if (response.getString("status").equals("OK")) {
                    mChannel = response.getString("channel");
                    mDevice = response.getString("device");

                    SharedPreferences pref = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(CHANNEL, mChannel);
                    editor.putString(DEVICE, mDevice);
                    editor.commit();
                    LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mRegisterReceiver);
                    setVisibility();
                    getAllMessages();
                } else {
                    Toast.makeText(getBaseContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Üzenet küldés BroadcastReceivere, a külön szálon történt üzenet feltöltés a szerverre
     * eredménye aktivizálja, siker esetén elküldi a csatorna kliens segítségével
     * a kommunikációs csatornába
     */
    private BroadcastReceiver mSendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("response");
            JSONObject response;
            try {
                response = new JSONObject(result);
                if (response.getString("status").equals("OK")) {
                    edMessage.setText(null);
                    edTitle.setText(null);
                    mSocket.emit(CHANNEL_EVENT, result);
                } else {
                    Toast.makeText(getBaseContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Az összes üzenet letöltését végző szál ha kész, ezt hívja meg az üzenetek kiírására.
     */
    private BroadcastReceiver mGetAllMessages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("response");
            try {
                JSONArray messages = new JSONArray(result);
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject msg = messages.getJSONObject(i);
                    String message = msg.getString("message");
                    mMessages.add(0, message);
                }
                mAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mGetAllMessages);
        }
    };

    /**
     * Kommunikációs csatorna kliens felíratkozása a csatornára. új üzenet
     * érkezését a run kezeli le, kiírja a listára a szöveget
     */
    private Emitter.Listener onNewMessage = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data;

                    try {
                        data = new JSONObject(args[0].toString());
                        try {
                            String status = data.getString("status");
                            String id = data.getString("id");
                            if (status.equals("OK")) {
                                if (id.equals("delete")) {
                                    getAllMessages();
                                    return;
                                }
                                String msg = data.getString("msg");
                                mMessages.add(0, msg);
                                mAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    };

    /**
     * A program indulásakor beolvassa az adatokat, ha már le vannak tárolva.
     */
    private void getPreferences() {
        SharedPreferences pref = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);
        mChannel = pref.getString(CHANNEL, null);
        mDevice = pref.getString(DEVICE, null);
    }

    /**
     * Belső osztály a külön szálon történő hálózati kommunikáció megvalósítására
     */
    public class AsyncHttpTask extends AsyncTask<String, Void, String> {

        private String localBrodcast;

        /**
         * Konstruktor
         * @param broadcast eltárolja, hogy melyik BroadcastReceiverre kell értesítést küldenie
         */
        public AsyncHttpTask(String broadcast) {
            super();
            this.localBrodcast = broadcast;
        }

        /**
         * A külön szálon futó hálózati kommunikáció
         * @param params az URL
         * @return a kommunikáció eredménye
         */
        @Override
        protected String doInBackground(String... params) {
            InputStream inputStream;
            String post;
            String result = null;

            post = params[1];
            HttpURLConnection urlConnection;

            try {
                URL url = new URL(params[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(post.length()));
                urlConnection.setUseCaches(false);
                urlConnection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(post);
                wr.flush();
                wr.close();

                int statusCode = urlConnection.getResponseCode();

                /* 200 HTTP OK */
                if (statusCode ==  200) {
                    inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    result = convertInputStreamToString(inputStream);
                }else{
                    Log.e("HTTP POST", params[0] + " hiba:" + statusCode);
                    result = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        /**
         * A HTTP kommunikáció során olvasott nyers adatokból szöveget készít.
         * @param inputStream hálózati csatorna
         * @return az összeállított szöveg
         * @throws IOException
         */
        private String convertInputStreamToString(InputStream inputStream) throws IOException {

            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));

            String line;
            String result = "";

            while((line = bufferedReader.readLine()) != null){
                result += line;
            }
            inputStream.close();

            return result;
        }

        /**
         * Miután lefuttott a külön szálon a kommunikáció, itt visszatér az alkalmazás fő szálába,
         * és Intenten keresztül elküldi az eredményt a BroadcastReceivernek.
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {

            if (result != null) {
                Intent intent = new Intent(localBrodcast);
                intent.putExtra("response", result);
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
            } else {
                Toast.makeText(MainActivity.this, "Hiba az adatok fogadásakor", Toast.LENGTH_LONG).show();
            }
        }
    }

}
