package com.example.imageorganizer;

import android.provider.BaseColumns;

public class TableClasses {
    private TableClasses() {}

    public static class Image implements BaseColumns {
        public static final String TABLE_NAME = "images";
        public static final String PATH_COL = "path";
        public static final String NAME_COL = "name";
    }
    public static class ImageFilter implements BaseColumns {
        public static final String TABLE_NAME = "imageFilter";
        public static final String IMAGE_ID_COL = "imageId";
        public static final String FILTER_ID_COL = "filterId";
    }
    public static class Filter implements BaseColumns {
        public static final String TABLE_NAME = "filters";
        public static final String FILTER_COL = "filter";
    }
}
