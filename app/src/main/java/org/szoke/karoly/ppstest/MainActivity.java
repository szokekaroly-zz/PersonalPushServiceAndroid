package org.szoke.karoly.ppstest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final String SERVICE_URL = "http://192.168.0.6/pps/index.php";
    private static final String CHANNEL_URL = "http://192.168.0.6:3000";
    private static final String CHANNEL_EVENT = "pps_channel";
    private static final String REGISTER = "/login/register_device";
    private static final String SEND = "/home/send_by_device";
    private static final String BROADCAST_REGISTER = "REGISTER";
    private static final String BROADCAST_SEND = "SEND";

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

        getPreferences();
        setVisibility();

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
            btSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edMessage.getText() != null) {
                        String params = null;
                        try {
                            params = "channel=" + URLEncoder.encode(mChannel, "UTF-8") + "&device=" +
                                    URLEncoder.encode(mDevice, "UTF-8") + "&msg=" + URLEncoder.encode(edMessage.getText().toString(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        new AsyncHttpTask(BROADCAST_SEND).execute(SERVICE_URL + SEND, params);
                    }
                }
            });
            mSocket.on(mChannel, onNewMessage);
            mSocket.connect();
        } else {
            edDevice.setText(Build.MODEL);
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
            JSONObject response = null;
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
                    mSocket.on(mChannel, onNewMessage);
                    mSocket.connect();
                    setVisibility();
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
            JSONObject response = null;
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
                                mMessages.add(msg);
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
            InputStream inputStream = null;
            String post= null;
            String result = null;

            post = params[1];
            HttpURLConnection urlConnection = null;

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
                    result = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {

            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));

            String line = "";
            String result = "";

            while((line = bufferedReader.readLine()) != null){
                result += line;
            }

            if(inputStream != null){
                inputStream.close();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {

            if (result != null) {
                Intent intent = new Intent(localBrodcast);
                intent.putExtra("response", result);
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
            } else {
                Log.e(TAG, "Hiba az adatok fogadásakor");
            }
        }
    }

}
