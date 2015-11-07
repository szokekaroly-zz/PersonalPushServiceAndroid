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
    //private static final String SERVICE_URL = "http://192.168.0.6/pps/index.php";
    private static final String SERVICE_URL = "https://pps-szokekaroly.rhcloud.com/index.php";
    //private static final String CHANNEL_URL = "http://192.168.0.6:3000";
    private static final String CHANNEL_URL = "https://ppsnodejs-szokekaroly.rhcloud.com";
    private static final String CHANNEL_EVENT = "pps_channel";
    private static final String REGISTER = "/login/register_device";
    private static final String SEND = "/home/send_by_device";
    private static final String GET_ALL = "/home/get_all_messages";
    private static final String BROADCAST_REGISTER = "REGISTER";
    private static final String BROADCAST_SEND = "SEND";
    private static final String BROADCAST_GET_ALL = "GETALL";

    private TextView tvEmail;
    private TextView tvPassword;
    private  TextView tvDevice;

    private EditText edEmail;
    private EditText edPassword;
    private EditText edDevice;
    private EditText edMessage;

    private Button btLogin;
    private Button btSend;

    private ProgressBar pbLogin;

    private ListView lvMessages;

    private String mChannel;
    private String mDevice;
    private ArrayAdapter mAdapter;
    private ArrayList<String> mMessages;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(CHANNEL_URL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEmail = (TextView) findViewById(R.id.tvEmail);
        tvDevice = (TextView) findViewById(R.id.tvDevice);
        tvPassword = (TextView) findViewById(R.id.tvPassword);

        edEmail = (EditText) findViewById(R.id.edEmail);
        edPassword = (EditText) findViewById(R.id.edPassword);
        edDevice = (EditText) findViewById(R.id.edDevice);
        edMessage = (EditText) findViewById(R.id.edMessage);

        btLogin = (Button) findViewById(R.id.btLogin);
        btSend = (Button) findViewById(R.id.btSend);

        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);

        lvMessages = (ListView) findViewById(R.id.lvMessages);
        mMessages = new ArrayList<>();
        mAdapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1, mMessages);
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

    private void setVisibility() {
        if (mDevice != null) {
            tvEmail.setVisibility(View.GONE);
            edEmail.setVisibility(View.GONE);
            tvPassword.setVisibility(View.GONE);
            edPassword.setVisibility(View.GONE);
            tvDevice.setVisibility(View.GONE);
            edDevice.setVisibility(View.GONE);
            btLogin.setVisibility(View.GONE);
            pbLogin.setVisibility(View.GONE);
            lvMessages.setVisibility(View.VISIBLE);
            edMessage.setVisibility(View.VISIBLE);
            btSend.setVisibility(View.VISIBLE);
            if ( ! isNetworkAvailable() ) {
                btSend.setEnabled(false);
            }
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
        } else {
            edDevice.setText(Build.MODEL);
            if ( ! isNetworkAvailable() ) {
                btLogin.setEnabled(false);
            }
            btLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edEmail.getText() == null) {
                        Toast.makeText(getBaseContext(), "Email címet kötelező kitölteni!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (edPassword.getText() == null) {
                        Toast.makeText(getBaseContext(), "Jelszó mezőt kötelező kitölteni!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (edDevice.getText() == null) {
                        Toast.makeText(getBaseContext(), "Eszköz nevét kötelező kitölteni!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String params = null;
                    try {
                        params = "email=" + URLEncoder.encode(edEmail.getText().toString(), "UTF-8")
                         + "&password=" + URLEncoder.encode(edPassword.getText().toString(), "UTF-8") +
                                "&name=" + URLEncoder.encode(edDevice.getText().toString(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mRegisterReceiver, new IntentFilter(BROADCAST_REGISTER));
                    new AsyncHttpTask(BROADCAST_REGISTER).execute(SERVICE_URL + REGISTER,params);
                }
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            Toast.makeText(MainActivity.this, "Az internet nem elérhető, kapcsolaja be a hálózatot, és indítsa újra az alkalmazást!", Toast.LENGTH_SHORT).show();
        }
        return  networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mSendReceiver, new IntentFilter(BROADCAST_SEND));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mRegisterReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mSendReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSocket.connected()) {
            mSocket.disconnect();
            mSocket.off(mChannel, onNewMessage);
        }
    }

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

    private BroadcastReceiver mSendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("response");
            JSONObject response;
            try {
                response = new JSONObject(result);
                if (response.getString("status").equals("OK")) {
                    edMessage.setText(null);
                    mSocket.emit(CHANNEL_EVENT, result);
                } else {
                    Toast.makeText(getBaseContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

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
                            if (status.equals("OK")) {
                                String msg = data.getString("msg");
                                mMessages.add(0, msg);
                                mAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    };

    private void getPreferences() {
        SharedPreferences pref = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);
        mChannel = pref.getString(CHANNEL, null);
        mDevice = pref.getString(DEVICE, null);
    }

    public class AsyncHttpTask extends AsyncTask<String, Void, String> {

        private String localBrodcast;

        public AsyncHttpTask(String broadcast) {
            super();
            this.localBrodcast = broadcast;
        }

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
