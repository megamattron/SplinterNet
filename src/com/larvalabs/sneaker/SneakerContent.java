package com.larvalabs.sneaker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * @author John Watkinson
 */
public class SneakerContent extends ContentProvider {

    private static final String AUTHORITY = "com.larvalabs.sneaker";
    private static final int CODE_ALL_POSTS = 1;
    private static final int CODE_POST = 2;
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.larvalabs.post";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/posts");

    private static UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, Database.DATABASE_TABLE, CODE_ALL_POSTS);
        URI_MATCHER.addURI(AUTHORITY, Database.DATABASE_TABLE + "/#", CODE_POST);
    }

    private Database database;

    @Override
    public boolean onCreate() {
        database = Database.initialize(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int offset = 0;
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL_POSTS:
                qb.setTables(Database.DATABASE_TABLE);
                break;
            case CODE_POST:
                qb.setTables(Database.DATABASE_TABLE);
                offset = Integer.parseInt(uri.getLastPathSegment());

                // Use if you want to limit the returned projection
                //qb.setProjectionMap(notesProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (offset > 0) {
            c.move(offset);
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL_POSTS:
            case CODE_POST:
                return CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (URI_MATCHER.match(uri) != CODE_ALL_POSTS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = database.getWritableDatabase();
        long rowId = db.insert(Database.DATABASE_TABLE, Database.COL_TEXT, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new RuntimeException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL_POSTS:
                count = db.delete(Database.DATABASE_TABLE, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL_POSTS:
                count = db.update(Database.DATABASE_TABLE, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
