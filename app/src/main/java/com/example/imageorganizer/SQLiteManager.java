package com.example.imageorganizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class SQLiteManager extends SQLiteOpenHelper {
    public static final String dbName = "ImageOrganizer.db";
    public static final int dbVersion = 1;

    private static final String SQL_CREATE_IMAGE_PATHS_TABLE =
            "CREATE TABLE " + TableClasses.Image.TABLE_NAME + "(" +
                    TableClasses.Image._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TableClasses.Image.PATH_COL + " VARCHAR(255) NOT NULL UNIQUE, " +
                    TableClasses.Image.NAME_COL + " VARCHAR(255), " +
                    MediaStore.Images.Media.DATE_TAKEN + " DATE);";
    private static final String SQL_CREATE_FILTERS_TABLE =
            "CREATE TABLE " + TableClasses.Filter.TABLE_NAME + "(" +
                    TableClasses.Filter._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TableClasses.Filter.FILTER_COL + " VARCHAR(255) NOT NULL UNIQUE);";
    private static final String SQL_CREATE_IMAGE_FILTER_TABLE =
            "CREATE TABLE " + TableClasses.ImageFilter.TABLE_NAME + "(" +
                    TableClasses.ImageFilter._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    TableClasses.ImageFilter.IMAGE_ID_COL + " INT NOT NULL, " +
                    TableClasses.ImageFilter.FILTER_ID_COL + " INT NOT NULL, " +
                    "FOREIGN KEY (" + TableClasses.ImageFilter.IMAGE_ID_COL + ") REFERENCES " + TableClasses.Image.TABLE_NAME + "(" + TableClasses.Image._ID + "), " +
                    "FOREIGN KEY (" + TableClasses.ImageFilter.FILTER_ID_COL + ") REFERENCES " + TableClasses.Filter.TABLE_NAME + "(" + TableClasses.Filter._ID + "), " +
                    "UNIQUE(" + TableClasses.ImageFilter.IMAGE_ID_COL + ", " + TableClasses.ImageFilter.FILTER_ID_COL + "));";


    public SQLiteManager(Context context) {
        super(context, dbName, null, dbVersion);
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

    public String unixInterpreter(String unix) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(Long.parseLong(unix)));
    }

    public long insertToImagePathTable(String path, @Nullable String name, @Nullable String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TableClasses.Image.PATH_COL, path);
        if (name != null) {
            values.put(TableClasses.Image.NAME_COL, name);
        }
        if (date != null){
            values.put(MediaStore.Images.Media.DATE_TAKEN, unixInterpreter(date));
        }

        return db.insert(TableClasses.Image.TABLE_NAME, null, values);
    }

    public long insertToFilterTable(String filter) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TableClasses.Filter.FILTER_COL, filter);

        return db.insert(TableClasses.Filter.TABLE_NAME, null, values);
    }

    public long insertToImageFilterTable(long imageId, long filterId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TableClasses.ImageFilter.IMAGE_ID_COL, imageId);
        values.put(TableClasses.ImageFilter.FILTER_ID_COL, filterId);

        return db.insert(TableClasses.ImageFilter.TABLE_NAME, null, values);
    }

    public Cursor selectFromImagePathTable(String[] columns, String where, String[] whereArgs, @Nullable String sortBy, Boolean asc) {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        if (sortBy != null) {
            orderBy = sortBy + "DESC";

            if (asc) {
                orderBy = sortBy + "ASC";
            }
        }

        if ( where == null || whereArgs == null) {
            return db.query(TableClasses.Image.TABLE_NAME, columns, null, null, null, null, orderBy);
        }

        return db.query(TableClasses.Image.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }

    public Cursor selectFromFilterTable(String[] columns, String where, String[] whereArgs, String sortBy, Boolean asc) {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = null;

        if (sortBy != null) {
            orderBy = sortBy + "DESC";

            if (asc) {
                orderBy = sortBy + "ASC";
            }
        }

        return db.query(TableClasses.Filter.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }

    public Cursor selectFromImageFilterTable(String[] columns, String where, String[] whereArgs, String sortBy, Boolean asc) {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = null;

        if (sortBy != null) {
            orderBy = sortBy + "DESC";

            if (asc) {
                orderBy = sortBy + "ASC";
            }
        }

        return db.query(TableClasses.ImageFilter.TABLE_NAME, columns, where, whereArgs, null, null, orderBy);
    }

    public String buildWhereClause(String col, int count, Boolean in) {
        if (count > 0) {
            StringBuilder where = new StringBuilder(col + (in ? " IN (" : " NOT IN ("));
            for (int i = 0; i < count; i++) {
                where.append("?, ");
            }
            where.setLength(where.length() - 2);
            return where.append(")").toString();
        }
        return null;
    }

    public String[] extractFromCursor(Cursor cursor, String col) {
        int count = cursor != null ? cursor.getCount() : 0;
        String[] dataArr = new String[count];

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int index = cursor.getColumnIndex(col);
            dataArr[i] = cursor.getString(index);
        }
        return dataArr;
    }

    public String[] getFiltersFromImageId(String[] imagesIds) {
        String[] filterArr = null;

        String bridgeSelection = buildWhereClause(TableClasses.ImageFilter.IMAGE_ID_COL, imagesIds.length, true);
        Cursor bridgeCursor = selectFromImageFilterTable(new String[]{TableClasses.ImageFilter.FILTER_ID_COL}, bridgeSelection, imagesIds, null, null);
        String[] filterIds = extractFromCursor(bridgeCursor, TableClasses.ImageFilter.FILTER_ID_COL);
        if (bridgeCursor != null) { bridgeCursor.close(); }

        if (filterIds != null && filterIds.length > 0) {
            String filterSelection = buildWhereClause(TableClasses.Filter._ID, filterIds.length, true);
            Cursor filterCursor = selectFromFilterTable(new String[]{TableClasses.Filter.FILTER_COL}, filterSelection, filterIds, null, null);
            filterArr = extractFromCursor(filterCursor, TableClasses.Filter.FILTER_COL);
            if (filterCursor != null) { filterCursor.close(); }
        }

        return filterArr;
    }

    public int removeFromFilter(String filter) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = TableClasses.Filter.FILTER_COL + "=?";
        String[] whereArg = {filter};
        return db.delete(TableClasses.Filter.TABLE_NAME, where, whereArg);
    }
}
