package org.szoke.karoly.ppstest.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.szoke.karoly.ppstest.MainActivity;
import org.szoke.karoly.ppstest.R;
import org.szoke.karoly.ppstest.adapter.MessagesListAdapter;
import org.szoke.karoly.ppstest.data.Message;

import java.net.URLEncoder;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class MainFragment extends Fragment {
    private static final String TAG = "PersonalPushServiceTest";
    public static final String DEVICE_PREF = "device_pref";
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

    BroadcastReceiverManager broadcastReceiverManager;

    RecyclerView rvMessages;
    MessagesListAdapter mMessagesListAdapter;
    ArrayList<Message> mMessages = new ArrayList<>();


    EditText edTitle;
    EditText edMessage;

    Button btnSend;

    Activity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        rvMessages = (RecyclerView) rootView.findViewById(R.id.rvMessages);

        edTitle = (EditText) rootView.findViewById(R.id.edTitle);
        edMessage = (EditText) rootView.findViewById(R.id.edMessage);

        btnSend = (Button) rootView.findViewById(R.id.btnSend);



        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMessagesListAdapter = new MessagesListAdapter(mMessages);

        //create layoutManager
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        initSendButtonClickListener();


        rvMessages.setAdapter(mMessagesListAdapter);
        rvMessages.setLayoutManager(llm);
        rvMessages.setHasFixedSize(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastReceiverManager.registSendReceiver(mSendReceiver);

    }

    @Override
    public void onPause() {
        super.onPause();
        broadcastReceiverManager.unRegistSendReceiver(mSendReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        broadcastReceiverManager.unRegistOnNewMessagesListener(onNewMessage);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


        broadcastReceiverManager = (BroadcastReceiverManager)activity;
        this.activity = activity;

        broadcastReceiverManager.registGetAllMessagesReciever(mGetAllMessages);
        broadcastReceiverManager.registOnNewMessagesListener(onNewMessage);



    }

    private void initSendButtonClickListener() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edTitle.getText().toString().equals("")){
                    edTitle.setError("Kötelező cím");
                }
                else if (edMessage.getText().toString().equals("")){
                    edMessage.setError("Kötelező üzenet");
                } else {
                    Message message = new Message(edTitle.getText().toString(), edMessage.getText().toString());
                    broadcastReceiverManager.sendNewMessagesListener(message);
                }
            }
        });
    }

    public interface BroadcastReceiverManager{
        public void registGetAllMessagesReciever(BroadcastReceiver getAllMessages);
        public void registOnNewMessagesListener(Emitter.Listener onNewMessage);
        public void registSendReceiver(BroadcastReceiver sendReceiver);
        public void unRegistSendReceiver(BroadcastReceiver sendReceiver);
        public void unRegistOnNewMessagesListener(Emitter.Listener onNewMessage);
        public void sendNewMessagesListener(Message message);
        public void emitSocket(String result);
    }

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
                    broadcastReceiverManager.emitSocket(result);
                } else {
                    Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
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
                    String messageText = msg.getString("message");
                    String messageTitle = msg.getString("title");
                    Message message = new Message(messageTitle, messageText);
                    Log.d("TAG_MS", "onReceive: " + messageText);
                    mMessagesListAdapter.addMessages(message);
                }
                //mMessagesListAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(mGetAllMessages);
        }
    };


    /**
     * Kommunikációs csatorna kliens felíratkozása a csatornára. új üzenet
     * érkezését a run kezeli le, kiírja a listára a szöveget
     */
    private Emitter.Listener onNewMessage = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            activity.runOnUiThread(new Runnable() {
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
                                    broadcastReceiverManager.registGetAllMessagesReciever(mGetAllMessages);
                                    return;
                                }
                                String msg = data.getString("msg");
                                String title = data.getString("title");
                                Message message = new Message(title, msg);
                                mMessages.add(0, message);
                                mMessagesListAdapter.notifyDataSetChanged();
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
}
