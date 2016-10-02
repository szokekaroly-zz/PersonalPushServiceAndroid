package org.szoke.karoly.ppstest.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.szoke.karoly.ppstest.R;
import org.szoke.karoly.ppstest.data.Message;

import java.util.ArrayList;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class MessagesListAdapter extends RecyclerView.Adapter<MessagesListAdapter.MessageViewHolder>{
    public static final String TAG_CALLED_METHOD = "TAG_CALLED_METHOD";
    private ArrayList<Message> messages = new ArrayList<>();
    Context context;

    public MessagesListAdapter(ArrayList<Message> messages) {
        super();
        this.messages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG_CALLED_METHOD, "onCreateViewHolder: ");
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, null));
    }

    public void addMessages(Message message){
        messages.add(0,message);
        notifyItemInserted(0);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Log.d(TAG_CALLED_METHOD, "onBindViewHolder: ");
        Message message = messages.get(position);

        holder.tvMessageTitle.setText(message.getTitle());
        holder.tvMessageText.setText(message.getMessageText());
    }

    @Override
    public int getItemCount() {
        Log.d(TAG_CALLED_METHOD, "getItemCount: ");
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder{
        TextView tvMessageTitle;
        TextView tvMessageText;

        public MessageViewHolder(View itemView) {
            super(itemView);

            tvMessageTitle = (TextView) itemView.findViewById(R.id.tvMessageTitle);
            tvMessageText = (TextView) itemView.findViewById(R.id.tvMessageText);
        }
    }
}
