package org.szoke.karoly.ppstest.data;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class Message {
    private String title;
    private String messageText;
    private String link;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Message(String title, String messageText, String link) {
        this.title = title;
        this.messageText = messageText;
        this.link = link;

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
