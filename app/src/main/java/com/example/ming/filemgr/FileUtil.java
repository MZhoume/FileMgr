package com.example.ming.filemgr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.util.Hashtable;

public class FileUtil {

    public static String resolveFileName(String path) {
        if (path == null) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) return null;

        return file.getName();
    }

    public static void showFileChooser(String fileType, Activity activity, int fileSelectCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(getMIME(fileType));
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(Intent.createChooser(intent, "Select a file"), fileSelectCode);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(activity, "Please install a file manager.", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getMIME(String fileType) {
        String mime = "*/*";
        if (mMIMETable.containsKey(fileType)) {
            mime = mMIMETable.get(fileType);
        }
        return mime;
    }

    @SuppressLint("NewApi")
    public static String getPath(Context context, Uri uri) {
        String path = null;

        if (isInKitKat() && DocumentsContract.isDocumentUri(context, uri)) {
            String docID = DocumentsContract.getDocumentId(uri);

            if (isExternalStorageDocument(uri)) {
                String[] splitted = docID.split(":");
                String type = splitted[0];

                if (type.equalsIgnoreCase("primary")) {
                    path = Environment.getExternalStorageDirectory() + "/" + splitted[1];
                }
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docID));
                path = resolveContentUri(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String[] splited = docID.split(":");
                String type = splited[0];

                Uri contentUri = null;
                if (type.equalsIgnoreCase("image")) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equalsIgnoreCase("video")) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equalsIgnoreCase("audio")) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                String selection = "_id=?";
                String[] selectionArgs = new String[] {splited[1]};

                path = resolveContentUri(context, contentUri, selection, selectionArgs);
            }
        } else if (uri.getScheme().equalsIgnoreCase("content")) {
            path = resolveContentUri(context, uri, null, null);
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            path = uri.getPath();
        }

        return path;
    }

    private static String resolveContentUri(Context context, Uri uri,
                                            String selection, String[] selectionArgs) {
        String resolved = null;

        String[] projection = {"_data"};
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            int column_index = cursor.getColumnIndexOrThrow("_data");
            if (cursor.moveToFirst()) {
                resolved = cursor.getString(column_index);
            }
        } catch (Exception ex) {
            // Eat it
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return resolved;
    }

    private static boolean isInKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return uri.getAuthority().equalsIgnoreCase("com.android.externalstorage.documents");
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return uri.getAuthority().equalsIgnoreCase("com.android.providers.downloads.documents");
    }

    private static boolean isMediaDocument(Uri uri) {
        return uri.getAuthority().equalsIgnoreCase("com.android.providers.media.documents");
    }

    private static Hashtable<String, String> mMIMETable = new Hashtable<>();

    static {
        mMIMETable.put("3gp", "video/3gpp");
        mMIMETable.put("apk", "application/vnd.android.package-archive");
        mMIMETable.put("asf", "video/x-ms-asf");
        mMIMETable.put("avi", "video/x-msvideo");
        mMIMETable.put("bin", "application/octet-stream");
        mMIMETable.put("bmp", "image/bmp");
        mMIMETable.put("c", "text/plain");
        mMIMETable.put("class", "application/octet-stream");
        mMIMETable.put("conf", "text/plain");
        mMIMETable.put("cpp", "text/plain");
        mMIMETable.put("doc", "application/msword");
        mMIMETable.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mMIMETable.put("xls", "application/vnd.ms-excel");
        mMIMETable.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mMIMETable.put("exe", "application/octet-stream");
        mMIMETable.put("gif", "image/gif");
        mMIMETable.put("gtar", "application/x-gtar");
        mMIMETable.put("gz", "application/x-gzip");
        mMIMETable.put("h", "text/plain");
        mMIMETable.put("htm", "text/html");
        mMIMETable.put("html", "text/html");
        mMIMETable.put("jar", "application/java-archive");
        mMIMETable.put("java", "text/plain");
        mMIMETable.put("jpeg", "image/jpeg");
        mMIMETable.put("jpg", "image/jpeg");
        mMIMETable.put("js", "application/x-javascript");
        mMIMETable.put("log", "text/plain");
        mMIMETable.put("m3u", "audio/x-mpegurl");
        mMIMETable.put("m4a", "audio/mp4a-latm");
        mMIMETable.put("m4b", "audio/mp4a-latm");
        mMIMETable.put("m4p", "audio/mp4a-latm");
        mMIMETable.put("m4u", "video/vnd.mpegurl");
        mMIMETable.put("m4v", "video/x-m4v");
        mMIMETable.put("mov", "video/quicktime");
        mMIMETable.put("mp2", "audio/x-mpeg");
        mMIMETable.put("mp3", "audio/x-mpeg");
        mMIMETable.put("mp4", "video/mp4");
        mMIMETable.put("mpc", "application/vnd.mpohun.certificate");
        mMIMETable.put("mpe", "video/mpeg");
        mMIMETable.put("mpeg", "video/mpeg");
        mMIMETable.put("mpg", "video/mpeg");
        mMIMETable.put("mpg4", "video/mp4");
        mMIMETable.put("mpga", "audio/mpeg");
        mMIMETable.put("msg", "application/vnd.ms-outlook");
        mMIMETable.put("ogg", "audio/ogg");
        mMIMETable.put("pdf", "application/pdf");
        mMIMETable.put("png", "image/png");
        mMIMETable.put("pps", "application/vnd.ms-powerpoint");
        mMIMETable.put("ppt", "application/vnd.ms-powerpoint");
        mMIMETable.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mMIMETable.put("prop", "text/plain");
        mMIMETable.put("rc", "text/plain");
        mMIMETable.put("rmvb", "audio/x-pn-realaudio");
        mMIMETable.put("rtf", "application/rtf");
        mMIMETable.put("sh", "text/plain");
        mMIMETable.put("tar", "application/x-tar");
        mMIMETable.put("tgz", "application/x-compressed");
        mMIMETable.put("txt", "text/plain");
        mMIMETable.put("wav", "audio/x-wav");
        mMIMETable.put("wma", "audio/x-ms-wma");
        mMIMETable.put("wmv", "audio/x-ms-wmv");
        mMIMETable.put("wps", "application/vnd.ms-works");
        mMIMETable.put("xml", "text/plain");
        mMIMETable.put("z", "application/x-compress");
        mMIMETable.put("zip", "application/x-zip-compressed");
    }
}
