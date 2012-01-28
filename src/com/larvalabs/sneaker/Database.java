package com.larvalabs.sneaker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Manages the data for the app.
 *
 * @author John Watkinson
 */
public class Database {

    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "sneaker";

    public static final String DATABASE_TABLE = "posts";

    public static final String COL_ID = "_id";
    public static final String COL_KEY = "key";
    public static final String COL_TEXT = "text";
    public static final String COL_HAS_IMAGE = "hasImage";
    public static final String COL_IMAGE = "image";
    public static final String COL_SCORE = "score";
    public static final String COL_STATUS = "status";
    public static final String COL_DATE = "date";
    // todo - other fields to consider: location, number of hops (although hops could be too revealing)

    private static final String DATABASE_TABLE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_KEY + " TEXT, " +
            COL_TEXT + " TEXT, " +
            COL_IMAGE + " BLOB, " +
            COL_HAS_IMAGE + " INTEGER, " +
            COL_SCORE + " INTEGER, " +
            COL_STATUS + " INTEGER, " +
            COL_DATE + " INTEGER" +
            ");";

    private static Database database = null;

    public static Database initialize(Context context) {
        if (database == null) {
            database = new Database(context);
        }
        return database;
    }

    public static Database getDB() {
        return database;
    }

    private SQLiteOpenHelper openHelper;

    private Database(Context context) {
        openHelper = new SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(DATABASE_TABLE_CREATE);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE " + DATABASE_TABLE + ";");
                db.execSQL(DATABASE_TABLE_CREATE);
            }
        };
    }

    public SQLiteDatabase getReadableDatabase() {
        return openHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
        return openHelper.getWritableDatabase();
    }

    public boolean addPost(Post post) {
        // Abort if post exists
        if (exists(post.getKey())) {
            Util.debug("  Post already exists, skipping.");
            return false;
        }
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_KEY, post.getKey());
        cv.put(COL_TEXT, post.getText());
        cv.put(COL_IMAGE, post.getImageData());
        cv.put(COL_SCORE, post.getScore());
        cv.put(COL_HAS_IMAGE, post.isHasImage() ? 1 : 0);
        cv.put(COL_STATUS, post.getStatus().ordinal());
        cv.put(COL_DATE, post.getDate().getTime());
        final long row = db.insert(DATABASE_TABLE, null, cv);
        // todo - if row is -1, freak out
        Post inserted = getPost(post.getKey());
        post.setId(inserted.getId());
        return true;
    }

    public boolean setPostStatus(String key, Status status) {
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status.ordinal());
        if (status == Status.DELETED) {
            // Also blank out the image and text (to save space)
            values.put(COL_IMAGE, (String) null);
            values.put(COL_TEXT, (String) null);
        }
        int rows = db.update(DATABASE_TABLE, values, COL_KEY + " = ?", new String[]{key});
        return rows == 1;
    }

    public boolean deletePost(String key) {
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = db.delete(DATABASE_TABLE, COL_KEY + " = ?", new String[]{key});
        return count > 0;
    }

    public void deleteAll() {
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        db.delete(DATABASE_TABLE, null, null);
    }



    // Creates screenshot-worthy data
    public void createWelcomeData(Context context) {
        long millis = Calendar.getInstance().getTime().getTime();
        {
            byte[] welcomeImage = Util.getBytesFromAsset(context, "icon.jpg");
            Date time = new Date(millis);
            final Post welcomePost = new Post(
                    context.getString(R.string.welcome),
                    welcomeImage,
                    2000,
                    Status.UNREAD,
                    time
            );
            // Special one-time key for welcome post.
            welcomePost.setKey("1");
            addPost(welcomePost);
        }
    }

    public void createDemoData(Context context) {
        long millis = Calendar.getInstance().getTime().getTime();
        //// Welcome post
        {
            byte[] welcomeImage = Util.getBytesFromAsset(context, "icon.jpg");
            Date time = new Date(millis);
            final Post welcomePost = new Post(
                    context.getString(R.string.welcome),
                    welcomeImage,
                    2000,
                    Status.UNREAD,
                    time
            );
            // Special one-time key for welcome post.
            welcomePost.setKey("1");
            addPost(welcomePost);
        }
        millis -= 20 * 60 * 1000;
        //// Demo post
        {
            byte[] image = Util.getBytesFromAsset(context, "demo1.jpg");
            Date time = new Date(millis);
            addPost(new Post(
                    "There was a large explosion early this morning in rebel-held town of Zintan, Libya.",
                    image,
                    1200,
                    Status.UNREAD,
                    time
            ));
        }
        millis -= 124 * 60 * 1000;
        //// Demo post
        {
            byte[] image = Util.getBytesFromAsset(context, "demo2.jpg");
            Date time = new Date(millis);
            addPost(new Post(
                    "Rebels appear to be approaching Tripoli with little resistance.",
                    image,
                    200,
                    Status.UNREAD,
                    time
            ));
        }
        millis -= 503 * 60 * 1000;
        //// Demo post
        {
            Date time = new Date(millis);
            addPost(new Post(
                    "Libyan rebels said on Monday they had seized a second strategic town near Tripoli within 24 hours, completing the encirclement of the capital in the boldest advances of their six-month uprising against Muammar Gaddafi,\" reports Michael Georgy at Reuters. \"Gaddafi's forces fired mortars and rockets at the coastal town of Zawiyah a day after rebels captured its center in a thrust that severed the vital coastal highway from Tripoli to the Tunisian border, a potential turning point in the war.\".",
                    null,
                    1500,
                    Status.UNREAD,
                    time
            ));
        }
        millis -= 1388 * 60 * 1000;
        //// Demo post
        {
            byte[] image = Util.getBytesFromAsset(context, "demo4.jpg");
            Date time = new Date(millis);
            addPost(new Post(
                    "Presented here is a linguistic map of Afghanistan.",
                    image,
                    1100,
                    Status.STARRED,
                    time
            ));
        }
        millis -= 1318 * 60 * 1000;
        //// Demo post
        {
            byte[] image = Util.getBytesFromAsset(context, "demo6.jpg");
            Date time = new Date(millis);
            addPost(new Post(
                    "Football tournaments get underway in Southern Iraq. Several local favorites were on the pitch Saturday afternoon for friendly games lasting well in to the evening.",
                    image,
                    900,
                    Status.UNREAD,
                    time
            ));
        }
    }

    // Creates test-worthy data
    public void createTestData(Context context) {
        byte[] image1 = Util.getBytesFromAsset(context, "test01.jpg");
        byte[] image2 = Util.getBytesFromAsset(context, "test02.jpg");
        byte[] image3 = Util.getBytesFromAsset(context, "test03.jpg");
        Date now = Calendar.getInstance().getTime();
        addPost(new Post(
                "Test Post #1. This is a test post, so just deal with it, y'all!",
                image1,
                500,
                Status.UNREAD,
                now
        ));
        addPost(new Post(
                "Test Post #2. This is a test post, so just deal with it, y'all!",
                image2,
                800,
                Status.READ,
                now
        ));
        addPost(new Post(
                "Test Post #3. This is a test post, so just deal with it, y'all!",
                image3,
                200,
                Status.STARRED,
                now
        ));
        addPost(new Post(
                "Test Post #4. This is a test post, so just deal with it, y'all!",
                null,
                1500,
                Status.UNREAD,
                now
        ));
        addPost(new Post(
                "Test Post #5. This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again.\n\nA new line! " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. " +
                        "This is a super long test post, with a whole bunch of text, that will be repeated again and again. The end!",
                image1,
                1500,
                Status.READ,
                now
        ));
        addPost(new Post(
                "Test Post #6. This is a test post, so just deal with it, y'all!",
                null,
                500,
                Status.READ,
                now
        ));
        addPost(new Post(
                "Test Post #7. This is a test post, so just deal with it, y'all!",
                null,
                1150,
                Status.READ,
                now
        ));
        addPost(new Post(
                "Test Post #8. This is a test post, so just deal with it, y'all!",
                image2,
                1888,
                Status.STARRED,
                now
        ));
        addPost(new Post(
                "Test Post #9. This is a test post, so just deal with it, y'all!",
                null,
                400,
                Status.READ,
                now
        ));
        addPost(new Post(
                "Test Post #10. This is a test post, so just deal with it, y'all!",
                image3,
                1300,
                Status.READ,
                now
        ));
    }

    public Post getPost(String key) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        Cursor c = qb.query(db, null, COL_KEY + " = ?", new String[]{key}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return null;
            } else {
                return getPostFromCursor(c);
            }
        } finally {
            c.close();
        }
    }

    public byte[] getImageData(long id) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        Cursor c = qb.query(db, new String[] {COL_IMAGE}, COL_ID + " = ?", new String[]{"" + id}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return null;
            } else {
                return c.getBlob(0);
            }
        } finally {
            c.close();
        }
    }

    public Post getPost(long id) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        Cursor c = qb.query(db, null, COL_ID + " = ?", new String[]{"" + id}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return null;
            } else {
                return getPostFromCursor(c);
            }
        } finally {
            c.close();
        }
    }

    public static Post getPostFromCursor(Cursor c) {
        long id = c.getLong(c.getColumnIndex(COL_ID));
        String key = c.getString(c.getColumnIndex(COL_KEY));
        String text = c.getString(c.getColumnIndex(COL_TEXT));
        byte[] imageData = c.getBlob(c.getColumnIndex(COL_IMAGE));
        int score = c.getInt(c.getColumnIndex(COL_SCORE));
        int statusInt = c.getInt(c.getColumnIndex(COL_STATUS));
        long time = c.getLong(c.getColumnIndex(COL_DATE));
        final Post post = new Post(key, text, imageData, score, statusInt, time);
        post.setId(id);
        return post;
    }

    public int getNumberOfPosts() {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        final SQLiteStatement statement = db.compileStatement("select count(*) from " + DATABASE_TABLE + ";");
        return (int) (statement.simpleQueryForLong());
    }

    public boolean exists(String key) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        Cursor c = qb.query(db, new String[]{COL_KEY}, COL_KEY + " = ?", new String[]{key}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /**
     * Gets a cursor sorted by score first, then by date.
     */
    public Cursor getMainCursor() {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        return qb.query(db, null, COL_STATUS + " != ?", new String[]{"" + Status.DELETED.ordinal()}, null, null, COL_SCORE + " DESC, " + COL_DATE + " DESC", null);
    }

    /**
     * Gets the list of keys to sync, sorted by starred, then score, then by date.
     */
    public ArrayList<String> getSyncList() {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);
        // todo - not quite right ordering in this query (unread are taken before read regardless of score).
        final Cursor cursor = qb.query(db, new String[]{COL_KEY}, COL_STATUS + " != ?", new String[]{"" + Status.DELETED.ordinal()}, null, null, COL_STATUS + " DESC, " + COL_SCORE + " DESC, " + COL_DATE + " DESC", null);
        final ArrayList<String> keys = new ArrayList<String>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    keys.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return keys;
    }

    /**
     * Remove keys that we already have in the DB.
     *
     * @param keys keys offered by a syncing partner.
     * @return the list of keys in the same order, but with existing keys removed.
     */
    public ArrayList<String> filterSyncList(ArrayList<String> keys) {
        ArrayList<String> filtered = new ArrayList<String>();
        for (String key : keys) {
            if (!exists(key)) {
                filtered.add(key);
            }
        }
        return filtered;
    }
}
