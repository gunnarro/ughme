package com.gunnarro.android.ughme.sms;

public class SMS {

    private boolean isRead;
    private String status;
    private String seen;
    private String date;
    private String address;
    private String body;
    private String type;

    private String period;
    private int numberOfReceived;
    private int numberOfSent;
    private int numberOfBlocked;
    private int count;

    public SMS(String period, int count) {
        this.period = period;
        this.count = count;
    }

    public String getPeriod() {
        return period;
    }

    public int getCount() {
        return count;
    }

    public SMS(int read, String status, String seen, String date, String address, String body, String type) {
        this.isRead = read == 1;
        this.status = status;
        this.seen = seen;
        this.date = date;
        this.address = address;
        this.body = body;
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getStatus() {
        return status;
    }

    public String getSeen() {
        return seen;
    }

    public String getDate() {
        return date;
    }

    public String getAddress() {
        return address;
    }

    public String getBody() {
        return body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNumberOfReceived() {
        return numberOfReceived;
    }

    public int getNumberOfSent() {
        return numberOfSent;
    }

    public int getNumberOfBlocked() {
        return numberOfBlocked;
    }

    public void increaseCount() {
        this.count++;
    }

    public void increaseNumberOfSent() {
        this.numberOfSent++;
    }

    public void increaseNumberOfReceived(int received) {
        this.numberOfReceived += received;
    }

    public void increaseNumberOfSent(int sent) {
        this.numberOfSent += sent;
    }

    public void increaseNumberOfReceived() {
        this.numberOfReceived++;
    }

    public void increaseNumberOfBlocked(int blocked) {
        this.numberOfBlocked += blocked;
    }

    public void increaseNumberOfBlocked() {
        this.numberOfBlocked++;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(date).append(" ").append(address).append("\n");
        s.append(body).append("\n");
        return s.toString();
    }
}
