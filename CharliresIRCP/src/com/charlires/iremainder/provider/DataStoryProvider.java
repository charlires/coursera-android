package com.charlires.iremainder.provider;

import android.content.*;
import android.content.ContentProvider;import android.content.ContentUris;import android.content.ContentValues;import android.content.Context;import android.content.UriMatcher;import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.lang.Override;import java.lang.String;import static com.charlires.iremainder.provider.MoocSchema.*;
import static com.charlires.iremainder.provider.MoocSchema.TAG;

/**
 * Created by carlos_andonaegui on 3/21/14.
 */
public class DataStoryProvider extends ContentProvider {

    static final int DATABASE_VERSION = 2;
    private static final String TAG_LOG = DataStoryProvider.class.getCanonicalName();
    private static final String DATABASE_NAME = "databasename.db";

    private static final UriMatcher mUriMatcher = MoocSchema.sURIMatcher;

//    private boolean MEMORY_ONLY_DB = false;
    private SQLiteDatabase database;

    private static class DatabaseHelper extends SQLiteOpenHelper {


        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(Story.DATABASE_CREATE_STORY);
            db.execSQL(Tags.DATABASE_CREATE_TAGS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + Story.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Tags.TABLE_NAME);
            onCreate(db);
        }

    }

    @Override
    public boolean onCreate() {
        Log.i(TAG_LOG, "BASE DE DATOS CREADA");
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        database = dbHelper.getWritableDatabase();
        return (database != null);
//        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(getType(uri));

        Cursor cursor = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        Log.i(TAG_LOG, "BASE DE CONSULTADA");
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        Log.i(TAG_LOG, "OBTENER TIPO");
        switch (mUriMatcher.match(uri)) {
            case MoocSchema.STORY:
            case MoocSchema.STORY_ID:
                return Story.TABLE_NAME;
            case MoocSchema.TAG:
            case MoocSchema.TAG_ID:
                return Tags.TABLE_NAME;
            default:
                return "";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.i(TAG_LOG, "INSERTAR");
        long rowID = database.insert(getType(uri), null, values);
        /**
         * If record is added successfully
         */
        if (rowID > 0)
        {
            Uri _uri = ContentUris.withAppendedId(Story.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.i(TAG_LOG, "ELIMINAR");
        int count = database.delete(getType(uri), selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.i(TAG_LOG, "ACTUALIZAR");
        int count = database.update(getType(uri), values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
