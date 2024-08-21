package com.example.imageorganizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.File;

public class SQLiteManager extends SQLiteOpenHelper {
    public static final String dbName = "ImageOrganizer.db";
    public static final int dbVersion = 1;

    private static String SQL_CREATE_IMAGE_PATHS_TABLE =
            "CREATE TABLE " + TableClasses.Image.TABLE_NAME + "(" +
                    TableClasses.Image._ID + " INT PRIMARY KEY," +
                    TableClasses.Image.PATH_COL + " VARCHAR(255) NOT NULL," +
                    TableClasses.Image.NAME_COL + " VARCHAR(255));";
    private static String SQL_CREATE_FILTERS_TABLE =
            "CREATE TABLE " + TableClasses.Filter.TABLE_NAME + "(" +
                    TableClasses.Filter._ID + " INT PRIMARY KEY," +
                    TableClasses.Filter.FILTER_COL + " VARCHAR(255) NOT NULL);";
    private static String SQL_CREATE_IMAGE_FILTER_TABLE =
            "CREATE TABLE " + TableClasses.ImageFilter.TABLE_NAME + "(" +
                    TableClasses.ImageFilter._ID + "INT PRIMARY KEY," +
                    TableClasses.ImageFilter.IMAGE_ID_COL + " VARCHAR(255) NOT NULL, " +
                    TableClasses.ImageFilter.FILTER_ID_COL + " INT NOT NULL," +
                    "FOREIGN KEY (" + TableClasses.ImageFilter.IMAGE_ID_COL + ") REFERENCES " + TableClasses.Image.TABLE_NAME + "(" + TableClasses.Image._ID + "), " +
                    "FOREIGN KEY (" + TableClasses.ImageFilter.FILTER_ID_COL + ") REFERENCES " + TableClasses.Filter.TABLE_NAME + "(" + TableClasses.Filter._ID + "));";

    public SQLiteManager(Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_IMAGE_PATHS_TABLE);
        db.execSQL((SQL_CREATE_FILTERS_TABLE));
        db.execSQL(SQL_CREATE_IMAGE_FILTER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL_CLEAR_IMAGE_PATHS = "DROP TABLE IF EXISTS " + TableClasses.Image.TABLE_NAME;
        String SQL_CLEAR_FILTERS = "DROP TABLE IF EXISTS " + TableClasses.Filter.TABLE_NAME;
        String SQL_CLEAR_IMAGE_FILTERS = "DROP TABLE IF EXISTS " + TableClasses.ImageFilter.TABLE_NAME;

        db.execSQL(SQL_CLEAR_IMAGE_PATHS);
        db.execSQL(SQL_CLEAR_FILTERS);
        db.execSQL(SQL_CLEAR_IMAGE_FILTERS);

        onCreate(db);
    }

    public long insertToImagePathTable(SQLiteDatabase db, String path, @Nullable String name) {
        ContentValues values = new ContentValues();

        values.put(TableClasses.Image.PATH_COL, path);
        values.put(TableClasses.Image.NAME_COL, name);

        return db.insert(TableClasses.Image.TABLE_NAME, null, values);
    }

    public long insertToFilterTable(SQLiteDatabase db, String filter) {
        ContentValues values = new ContentValues();

        values.put(TableClasses.Filter.FILTER_COL, filter);

        return db.insert(TableClasses.Filter.TABLE_NAME, null, values);
    }

    public long insertToImageFilterTable(SQLiteDatabase db, long imageId, long filterId) {
        ContentValues values = new ContentValues();

        values.put(TableClasses.ImageFilter.IMAGE_ID_COL, imageId);
        values.put(TableClasses.ImageFilter.FILTER_ID_COL, filterId);

        return db.insert(TableClasses.ImageFilter.TABLE_NAME, null, values);
    }

    public Cursor selectFromImagePathTable(SQLiteDatabase db, String[] columns, String where, String[] whereArgs, String sortBy, Boolean asc) {
        String orderBy = null;

        if (sortBy != "") {
            if (asc) {
                orderBy = sortBy + "ASC";
            } else {
                orderBy = sortBy + "DESC";
            }
        }

        return db.query(TableClasses.Image.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }

    public Cursor selectFromFilterTable(SQLiteDatabase db, String[] columns, String where, String[] whereArgs, String sortBy, Boolean asc) {
        String orderBy = null;

        if (sortBy != "") {
            if (asc) {
                orderBy = sortBy + "ASC";
            } else {
                orderBy = sortBy + "DESC";
            }
        }

        return db.query(TableClasses.Filter.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }

    public Cursor selectFromImageFilterTable(SQLiteDatabase db, String[] columns, String where, String[] whereArgs, String sortBy, Boolean asc) {
        String orderBy = null;

        if (sortBy != "") {
            if (asc) {
                orderBy = sortBy + "ASC";
            } else {
                orderBy = sortBy + "DESC";
            }
        }

        return db.query(TableClasses.ImageFilter.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }
}
