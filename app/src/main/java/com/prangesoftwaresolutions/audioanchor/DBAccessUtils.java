package com.prangesoftwaresolutions.audioanchor;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.prangesoftwaresolutions.audioanchor.data.AnchorContract;

import java.io.File;
import java.util.ArrayList;

class DBAccessUtils {

    /*
     * Get the string for the album completion time given the album id
     */
    static String getAlbumCompletionString(Context context, long albumID)
    {
        // Check whether to return time string as percentage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean progressInPercent = prefs.getBoolean(context.getResources().getString(R.string.settings_progress_percentage_key), Boolean.getBoolean(context.getResources().getString(R.string.settings_progress_percentage_default)));

        // Query the database for the track completion times
        String[] columns = new String[]{AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME, AnchorContract.AudioEntry.COLUMN_TIME};
        String sel = AnchorContract.AudioEntry.COLUMN_ALBUM + "=?";
        String[] selArgs = {Long.toString(albumID)};

        Cursor c = context.getContentResolver().query(AnchorContract.AudioEntry.CONTENT_URI,
                columns, sel, selArgs, null, null);

        if(c == null) {
            return "";
        }  else if (c.getCount() < 1) {
            c.close();
            return "";
        }

        // Loop through the database rows and sum up the audio durations and completed times
        int sumDuration = 0;
        int sumCompletedTime = 0;
        while (c.moveToNext()) {
            sumDuration += c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TIME));
            sumCompletedTime += c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME));
        }
        c.close();

        // Generate the string for the album completion time
        String timeStr;
        if (progressInPercent) {
            int percent = Math.round(((float)sumCompletedTime / sumDuration) * 100);
            timeStr = context.getResources().getString(R.string.time_completed_percent, percent);
        } else {
            String durationStr = Utils.formatTime(sumDuration, sumDuration);
            String completedTimeStr = Utils.formatTime(sumCompletedTime, sumDuration);
            timeStr = context.getResources().getString(R.string.time_completed, completedTimeStr, durationStr);
        }

        return timeStr;
    }


    /*
     * Get the audio file for the specified uri
     */
    static AudioFile getAudioFile(Context context, Uri AudioFileAlbumUri, String baseDirectory) {
        String[] projection = {
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry._ID,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_TITLE,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_ALBUM,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_TIME,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME,
                AnchorContract.AlbumEntry.TABLE_NAME + "." + AnchorContract.AlbumEntry.COLUMN_TITLE,
                AnchorContract.AlbumEntry.TABLE_NAME + "." + AnchorContract.AlbumEntry.COLUMN_COVER_PATH};
        Cursor c = context.getContentResolver().query(AudioFileAlbumUri, projection, null, null, null);

        if (c == null) {
            throw new SQLException();
        } else if (c.getCount() < 1) {
            c.close();
            throw new SQLException();
        }

        AudioFile audioFile;
        if (c.moveToFirst()) {
            int id = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry._ID));
            String title = c.getString(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TITLE));
            int albumId = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_ALBUM));
            int completedTime = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME));
            int time = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TIME));
            String albumTitle = c.getString(c.getColumnIndex(AnchorContract.AlbumEntry.TABLE_NAME + AnchorContract.AlbumEntry.COLUMN_TITLE));
            String albumCoverPath = c.getString(c.getColumnIndex(AnchorContract.AlbumEntry.TABLE_NAME + AnchorContract.AlbumEntry.COLUMN_COVER_PATH));
            audioFile =  new AudioFile(id, title, albumId, time, completedTime, albumTitle, albumCoverPath, baseDirectory);
        } else {
            c.close();
            throw new SQLException();
        }
        c.close();
        return audioFile;
    }

    /*
     * Get all audio files for the specified album
     */
    static ArrayList<AudioFile> getAllAudioFilesFromAlbum(Context context, int albumId, String sortOrder, String baseDirectory) {
        String[] projection = {
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry._ID,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_TITLE,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_ALBUM,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_TIME,
                AnchorContract.AudioEntry.TABLE_NAME + "." + AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME,
                AnchorContract.AlbumEntry.TABLE_NAME + "." + AnchorContract.AlbumEntry.COLUMN_TITLE,
                AnchorContract.AlbumEntry.TABLE_NAME + "." + AnchorContract.AlbumEntry.COLUMN_COVER_PATH};

        String sel = AnchorContract.AudioEntry.COLUMN_ALBUM + "=?";
        String[] selArgs = {Long.toString(albumId)};
        Cursor c = context.getContentResolver().query(AnchorContract.AudioEntry.CONTENT_URI_AUDIO_ALBUM, projection, sel, selArgs, sortOrder);

        if (c == null) {
            throw new SQLException();
        } else if (c.getCount() < 1) {
            c.close();
            throw new SQLException();
        }

        ArrayList<AudioFile> audioFiles = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry._ID));
                String title = c.getString(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TITLE));
                int completedTime = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME));
                int time = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TIME));
                String albumTitle = c.getString(c.getColumnIndex(AnchorContract.AlbumEntry.TABLE_NAME + AnchorContract.AlbumEntry.COLUMN_TITLE));
                String albumCoverPath = c.getString(c.getColumnIndex(AnchorContract.AlbumEntry.TABLE_NAME + AnchorContract.AlbumEntry.COLUMN_COVER_PATH));
                audioFiles.add(new AudioFile(id, title, albumId, time, completedTime, albumTitle, albumCoverPath, baseDirectory));
            } while (c.moveToNext());

        } else {
            c.close();
            throw new SQLException();
        }
        c.close();
        return audioFiles;
    }


    /*
     * Delete track with the specified id from the database
     */
    static boolean deleteTrackFromDB(Context context, long trackId) {
        Uri uri = ContentUris.withAppendedId(AnchorContract.AudioEntry.CONTENT_URI_AUDIO_ALBUM, trackId);
        Uri deleteUri = ContentUris.withAppendedId(AnchorContract.AudioEntry.CONTENT_URI, trackId);

        // Don't allow delete action if the track still exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String directory = prefs.getString(context.getResources().getString(R.string.preference_filename), null);
        AudioFile audio = getAudioFile(context, uri, directory);
        if (! (new File(audio.getPath())).exists()) {
            // Delete track from database
            context.getContentResolver().delete(deleteUri, null, null);
            return true;
        }
        return false;
    }


    /*
     * Delete album with the specified id from the database
     */
    static boolean deleteAlbumFromDB(Context context, long albumId) {
        // Get the title of the album to check if the album still exists in the file system
        Uri uri = ContentUris.withAppendedId(AnchorContract.AlbumEntry.CONTENT_URI, albumId);
        String[] proj = new String[]{AnchorContract.AlbumEntry.COLUMN_TITLE};
        Cursor c = context.getContentResolver().query(uri, proj, null, null, null);

        // Bail early if the cursor is null
        if (c == null) {
            return false;
        } else if (c.getCount() < 1) {
            c.close();
            return false;
        }

        String title = null;
        if (c.moveToNext()) {
            title = c.getString(c.getColumnIndex(AnchorContract.AlbumEntry.COLUMN_TITLE));
        }
        c.close();

        if (title == null) return false;

        // Don't allow delete action if the album still exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String directory = prefs.getString(context.getResources().getString(R.string.preference_filename), null);
        if (!(new File(directory, title)).exists()) {
            // Delete album from database
            context.getContentResolver().delete(uri, null, null);
            return true;
        }
        return false;
    }


    /*
     * Delete bookmarks for the specified track from the database
     */
    static void deleteBookmarksForTrack(Context context, long trackId) {
        // Get all bookmarks associated with the trackId
        String[] columns = new String[]{AnchorContract.BookmarkEntry._ID, AnchorContract.BookmarkEntry.COLUMN_AUDIO_FILE};
        String sel = AnchorContract.BookmarkEntry.COLUMN_AUDIO_FILE + "=?";
        String[] selArgs = {Long.toString(trackId)};

        Cursor c = context.getContentResolver().query(AnchorContract.BookmarkEntry.CONTENT_URI,
                columns, sel, selArgs, null, null);

        // Bail early if the cursor is null
        if (c == null) {
            return;
        } else if (c.getCount() < 1) {
            c.close();
            return;
        }

        while (c.moveToNext()) {
            // Delete bookmarks associated with the track from the database
            int bookmarkId = c.getInt(c.getColumnIndex(AnchorContract.BookmarkEntry._ID));
            Uri deleteUri = ContentUris.withAppendedId(AnchorContract.BookmarkEntry.CONTENT_URI, bookmarkId);
            context.getContentResolver().delete(deleteUri, null, null);
        }
        c.close();
    }


    /*
     * Mark track with the specified id as not started, i.e. set completedTime to 0 in the db
     */
    static void markTrackAsNotStarted(Context context, long trackId) {
        Uri uri = ContentUris.withAppendedId(AnchorContract.AudioEntry.CONTENT_URI, trackId);
        ContentValues values = new ContentValues();
        values.put(AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME, 0);
        context.getContentResolver().update(uri, values, null, null);
        context.getContentResolver().notifyChange(uri, null);
    }


    /*
     * Mark track with the specified id as completed, i.e. set completedTime to totalTime in the db
     */
    static void markTracksAsCompleted(Context context, long trackId) {
        // Get total time for the specified track
        String[] columns = new String[]{AnchorContract.AudioEntry.COLUMN_TIME};
        String sel = AnchorContract.AudioEntry._ID + "=?";
        String[] selArgs = {Long.toString(trackId)};

        Cursor c = context.getContentResolver().query(AnchorContract.AudioEntry.CONTENT_URI,
                columns, sel, selArgs, null, null);

        // Bail early if the cursor is null
        if (c == null) {
            return;
        } else if (c.getCount() < 1) {
            c.close();
            return;
        }

        int totalTime = 0;
        while (c.moveToNext()) {
            totalTime = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry.COLUMN_TIME));
        }
        c.close();

        // Set completedTime to totalTime
        Uri uri = ContentUris.withAppendedId(AnchorContract.AudioEntry.CONTENT_URI, trackId);
        ContentValues values = new ContentValues();
        values.put(AnchorContract.AudioEntry.COLUMN_COMPLETED_TIME, totalTime);
        context.getContentResolver().update(uri, values, null, null);
        context.getContentResolver().notifyChange(uri, null);
    }


    /*
     * Get all track ids for the specified album
     */
    static ArrayList<Long> getTrackIdsForAlbum(Context context, long albumId) {
        ArrayList<Long> trackIds = new ArrayList<>();

        String[] columns = new String[]{AnchorContract.AudioEntry._ID};
        String sel = AnchorContract.AudioEntry.COLUMN_ALBUM + "=?";
        String[] selArgs = {Long.toString(albumId)};

        Cursor c = context.getContentResolver().query(AnchorContract.AudioEntry.CONTENT_URI,
                columns, sel, selArgs, null, null);

        // Bail early if the cursor is null
        if (c == null) {
            return trackIds;
        } else if (c.getCount() < 1) {
            c.close();
            return trackIds;
        }

        while (c.moveToNext()) {
            long trackId = c.getInt(c.getColumnIndex(AnchorContract.AudioEntry._ID));
            trackIds.add(trackId);
        }
        c.close();

        return trackIds;
    }
}