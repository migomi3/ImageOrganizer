package com.example.imageorganizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class FilterDialogHelper {
    static SQLiteManager dbManager;

    public interface ShowFilterAction {
        void onOkButtonPressed (Dialog dialog, ChipGroup chipGroup);
        void onNegativeButtonPressed();
    }

    public interface InputFilterAction {
        void onPositiveButtonPressed(String str);
    }

    public static void showCheckableFilters(Context context, ShowFilterAction showFilterAction) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.filter_layout_w_ok);
        dbManager = new SQLiteManager(context);

        ChipGroup filterChips = dialog.findViewById(R.id.filter_chips);

        Button negativeButton = dialog.findViewById(R.id.cancel_button);
        Button okButton = dialog.findViewById(R.id.ok_button);

        negativeButton.setOnClickListener(view -> {
            showFilterAction.onNegativeButtonPressed();
            dialog.dismiss();
        });
        okButton.setOnClickListener(view -> {
            showFilterAction.onOkButtonPressed(dialog, filterChips);
            dialog.dismiss();
        });

        generateCheckableFilters(filterChips);
        dialog.show();
    }

    private static void generateCheckableFilters(ChipGroup chipGroup) {
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

    public static void filterTextInputBox (Context context, InputFilterAction inputFilterAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("New Filter");

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);

        builder.setPositiveButton("OK", (dialogInterface, i) -> inputFilterAction.onPositiveButtonPressed(input.getText().toString()));
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();
    }
}