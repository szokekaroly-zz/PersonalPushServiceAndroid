package org.szoke.karoly.ppstest.data;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class Message {
    private String title;
    private String messageText;

    public Message(String title, String messageText) {
        this.title = title;
        this.messageText = messageText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
