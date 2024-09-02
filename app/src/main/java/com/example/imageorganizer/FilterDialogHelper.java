package com.example.imageorganizer;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.widget.Button;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class FilterDialogHelper {
    static SQLiteManager dbManager;

    public interface FilterAction {
        void onOkButtonPressed (Dialog dialog, ChipGroup chipGroup);
        void negativeButtonPressed ();
    }

    public static void showFilters(Context context, FilterAction filterAction) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.filter_layout);
        dbManager = new SQLiteManager(context);

        ChipGroup filterChips = dialog.findViewById(R.id.filter_chips);

        Button negativeButton = dialog.findViewById(R.id.clear_filter_button);
        Button okButton = dialog.findViewById(R.id.ok_button);

        negativeButton.setOnClickListener(view -> {
            filterAction.negativeButtonPressed();
            dialog.dismiss();
        });
        okButton.setOnClickListener(view -> {
            filterAction.onOkButtonPressed(dialog, filterChips);
            dialog.dismiss();
        });

        generateFilters(filterChips);
        dialog.show();
    }

    private static void generateFilters(ChipGroup chipGroup) {
        //TODO: Rewrite logic
        Cursor cursor = dbManager.selectFromFilterTable(new String[]{TableClasses.Filter.FILTER_COL}, null, null, null, null);
        String[] filters = dbManager.extractFromCursor(cursor, TableClasses.Filter.FILTER_COL);
        if ( cursor != null) { cursor.close(); }

        for (String tag : filters) {
            Chip chip = new Chip(chipGroup.getContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chipGroup.addView(chip);
        }
    }
}