package org.szoke.karoly.ppstest.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.szoke.karoly.ppstest.MainActivity;
import org.szoke.karoly.ppstest.R;
import org.szoke.karoly.ppstest.data.Message;

import java.util.ArrayList;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class MessagesListAdapter extends RecyclerView.Adapter<MessagesListAdapter.MessageViewHolder> {
    private ArrayList<Message> messages = new ArrayList<>();
    Context context;
    OnLinkClickListener onLinkClickListener;

    public MessagesListAdapter(ArrayList<Message> messages, Context context) {
        super();
        this.context = context;
        this.messages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, null));
    }

    public void addMessages(Message message) {
        messages.add(0, message);
        notifyItemInserted(0);
    }

    public interface OnLinkClickListener {
        public void onLinkClick(Intent intent);

        public void onFileClick(String path, String fileName);
    }

    public void setOnLinkClickListener(OnLinkClickListener onLinkClickListener) {
        this.onLinkClickListener = onLinkClickListener;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        final Message message = messages.get(position);

        holder.tvMessageTitle.setText(message.getTitle());
        holder.tvMessageText.setText(message.getMessageText());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMessageText()));
                try {
                    if (onLinkClickListener != null) {
                        onLinkClickListener.onLinkClick(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    if (!message.getLink().equals("null")) {
                        onLinkClickListener.onFileClick(message.getLink(), message.getMessageText());
                    } else {
                        Toast.makeText(context, message.getMessageText(), Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageTitle;
        TextView tvMessageText;

        public MessageViewHolder(View itemView) {
            super(itemView);

            tvMessageTitle = (TextView) itemView.findViewById(R.id.tvMessageTitle);
            tvMessageText = (TextView) itemView.findViewById(R.id.tvMessageText);
        }
    }
}
