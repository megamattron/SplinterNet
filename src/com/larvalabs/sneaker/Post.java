package com.larvalabs.sneaker;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * @author John Watkinson
 */
public class Post implements Streamable, Parcelable {

    private long id;
    private String key;
    private String text;
    private byte[] imageData;
    private int score;
    private boolean hasImage;
    private Status status;
    private Date date;

    public Post(String text, byte[] imageData, int score, Status status, Date date) {
        this.text = text;
        this.imageData = imageData;
        hasImage = imageData != null;
        this.score = score;
        this.status = status;
        this.date = date;
        key = UUID.randomUUID().toString();
    }

    public Post(String key, String text, byte[] imageData, int score, int statusInt, long time) {
        this.key = key;
        this.text = text;
        this.imageData = imageData;
        hasImage = imageData != null;
        this.score = score;
        date = new Date(time);
        status = Status.values()[statusInt];
    }

    public Post() {
    }

    public Post(Parcel in) {
        id = in.readLong();
        key = in.readString();
        text = in.readString();
        if (in.readByte() != 0) {
            imageData = in.createByteArray();
            //in.readByteArray(imageData);
        }
        score = in.readInt();
        hasImage = in.readInt() != 0;
        status = Status.values()[in.readInt()];
        date = new Date(in.readLong());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(key);
        out.writeString(text);
        if (imageData != null) {
            out.writeByte((byte) 1);
            out.writeByteArray(imageData);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeInt(score);
        out.writeInt(hasImage ? 1 : 0);
        out.writeInt(status.ordinal());
        out.writeLong(date.getTime());
    }

    public void read(DataInputStream in) throws IOException {
        id = in.readLong();
        key = in.readUTF();
        Util.debug("READING: " + key);
        text = in.readUTF();
        int size = in.readInt();
        Util.debug("READING " + size + " bytes...");
        if (size == 0) {
            imageData = null;
        } else {
            imageData = new byte[size];
            in.readFully(imageData, 0, size);
            Util.debug("Last byte: " + imageData[size - 1]);
        }
        score = in.readInt();
        hasImage = in.readInt() != 0;
        Util.debug("READ score: " + score);
        status = Status.values()[in.readInt()];
        date = new Date(in.readLong());
    }

    public void write(DataOutputStream out) throws IOException {
        Util.debug("WRITING: " + key);
        out.writeLong(id);
        out.writeUTF(key);
        out.writeUTF(text == null ? "" : text);
        if (imageData != null) {
            int size = imageData.length;
            Util.debug("WRITING " + size + " bytes.");
            out.writeInt(size);
            Util.writeInChunks(imageData, out, 8192);
            //out.write(imageData, 0, size);
            Util.debug("Last byte: " + imageData[size-1]);
        } else {
            out.writeInt(0);
        }
        Util.debug("WRITING score: " + score + ".");
        out.writeInt(score);
        out.writeInt(hasImage ? 1 : 0);
        out.writeInt(status.ordinal());
        out.writeLong(date.getTime());
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int describeContents() {
        return 0;
    }

    public boolean isHasImage() {
        return hasImage;
    }
}
