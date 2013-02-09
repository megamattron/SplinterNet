/**
 *
 */
package com.larvalabs.sneaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.media.ExifInterface;

/**
 * @author Mihai Badescu
 */
public class ExifHandler {

    public static String scrubExif(String path) {
        String newfilename = path.replace(".jpg",
                "_SCRUBBED.jpg");
        File working = new File(path);
        File copy = new File(newfilename);
        try {
            working = copy(working, copy);
            ExifInterface exif = new ExifInterface(working.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_MAKE, "");
            exif.setAttribute(ExifInterface.TAG_MODEL, "");
//    	exif.setAttribute(ExifInterface.TAG_DATETIME, "");
//    	exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "");
//   	exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "");
            exif.saveAttributes();
            return working.getAbsolutePath();

        } catch (IOException e) {
            Util.error("Error setting EXIF attributes.", e);
            return null;
        }

    }

    public static ExifInterface getExif(String path) throws IOException {
        ExifInterface exif = new ExifInterface(path);

        return exif;
    }

    public static String printExif(ExifInterface exif) {

        String myAttribute = "Exif information ---\n";
        myAttribute += getTagString(ExifInterface.TAG_DATETIME, exif);
        myAttribute += getTagString(ExifInterface.TAG_FLASH, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
        myAttribute += getTagString(ExifInterface.TAG_IMAGE_LENGTH, exif);
        myAttribute += getTagString(ExifInterface.TAG_IMAGE_WIDTH, exif);
        myAttribute += getTagString(ExifInterface.TAG_MAKE, exif);
        myAttribute += getTagString(ExifInterface.TAG_MODEL, exif);
        myAttribute += getTagString(ExifInterface.TAG_WHITE_BALANCE, exif);

        return myAttribute;
    }

    private static String getTagString(String tag, ExifInterface exif) {
        return (tag + " : " + exif.getAttribute(tag) + "\n");
    }

    private static File copy(File source, File destination) throws IOException {
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(destination);

        // Transfer bytes from in to out
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();

        return destination;
    }

}
