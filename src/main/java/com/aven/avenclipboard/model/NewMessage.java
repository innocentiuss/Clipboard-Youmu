package com.aven.avenclipboard.model;

public class NewMessage {
    private int fromId;
    private String message;

    public NewMessage(int fromId, String message) {
        this.fromId = fromId;
        this.message = message;
    }

    public NewMessage() {
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
