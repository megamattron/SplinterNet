package com.larvalabs.sneaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author John Watkinson
 */
public class Util {

    private static boolean debugMode = false;

    private static String tag = "";
    private static final String CHANGE_FLAG = "__CHANGE";

    public static void setProjectName(String t) {
        tag = t;
    }

    public static String getProjectTag() {
        return tag;
    }

    public static void setDebugMode(boolean debugMode) {
        Util.debugMode = debugMode;
    }

    public static void debug(String message) {
        if (debugMode) {
            Log.d(tag, message);
        }
    }

    public static void error(String message, Throwable t) {
        Log.e(tag, message, t);
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bout);
        return bout.toByteArray();
    }

    public static Bitmap bytesToBitmap(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Bitmap bytesToBitmapWithSampling(byte[] bytes, int targetSize) {
        //decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);

        //Find the correct scale value. It should be the power of 2.
        final int REQUIRED_SIZE = targetSize;
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        //decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o2);
    }

    public static byte[] getBytesFromFile(String path) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(path);
            return getBytesFromStream(is);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static byte[] getBytesFromAsset(Context context, String path) {
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
            return getBytesFromStream(is);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static byte[] getBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public static Bitmap decodeBitmap(String path, int maxSize) {
        File f = new File(path);
        Bitmap b = null;
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > maxSize || o.outWidth > maxSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }

    private static final DateFormat TODAY_FORMAT = new SimpleDateFormat("hh:mm");
    private static final DateFormat THIS_YEAR_FORMAT = new SimpleDateFormat("MMM dd, hh:mm");
    private static final DateFormat OLDER_FORMAT = new SimpleDateFormat("MMM dd, yyy");

    // todo - support proper localization of dates/times
    public static String formatDate(Date date) {
        Date now = Calendar.getInstance().getTime();
        if (date.getYear() != now.getYear()) {
            return OLDER_FORMAT.format(date);
        } else if (date.getDay() != now.getDay()) {
            return THIS_YEAR_FORMAT.format(date);
        } else {
            return TODAY_FORMAT.format(date) + " Today";
        }
    }

    public static void handlePanic(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(R.string.panic_confirm))
                .setCancelable(false)
                .setPositiveButton(activity.getString(R.string.panic_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Database.getDB().deleteAll();
                        if (!(activity instanceof Sneaker)) {
                            // todo - include request code that indicates that a reload should take place
                            activity.finish();
                        } else {
                            ((Sneaker) activity).reload();
                        }
                    }
                })
                .setNegativeButton(activity.getString(R.string.panic_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static Loader<Cursor> createContentLoader(String searchText, Activity activity, boolean includeImages) {
        String select = Database.COL_STATUS + " != ?";
        String[] params;
        if (searchText != null && searchText.length() > 0) {
            select += " AND " + Database.COL_TEXT + " like ?";
            params = new String[]{"" + Status.DELETED.ordinal(), "%" + searchText + "%"};
        } else {
            params = new String[]{"" + Status.DELETED.ordinal()};
        }
        String[] projection = null;
        if (!includeImages) {
            projection = new String[] {Database.COL_ID, Database.COL_DATE, Database.COL_KEY, Database.COL_SCORE, Database.COL_HAS_IMAGE, Database.COL_STATUS, Database.COL_TEXT};
        }
        if (activity != null) {
            return new CursorLoader(activity, SneakerContent.CONTENT_URI,
                    projection, select, params,
                    Database.COL_DATE + " DESC");
        } else {
            return null;
        }
    }

    public static void writeInChunks(byte[] data, DataOutputStream out, int chunkSize) throws IOException {
        int n = data.length;
        int i = 0;
        while (i < n) {
            int c = Math.min(chunkSize, n - i);
            out.write(data, i, c);
            i += c;
        }
    }

    public static void setChangeFlag(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(CHANGE_FLAG, true).commit();
    }

    public static boolean getAndClearChangeFlag(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean result = preferences.getBoolean(CHANGE_FLAG, false);
        if (result) {
            preferences.edit().remove(CHANGE_FLAG).commit();
        }
        return result;
    }
}
