package com.larvalabs.sneaker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

/**
 * @author John Watkinson
 */
public class PostStatusDialog {

    public static final Status[] STATUSES = {Status.STARRED, Status.READ, Status.DELETED};
    public static final int[] INDICES = {2, 1, 1, 0};

    private static class Selection {
        int index;
    }

    public static final AlertDialog createPostStatusDialog(Context context, final String key, int index, final PostStatusListener listener) {
        final CharSequence[] items = {context.getString(R.string.status_important), context.getString(R.string.status_normal), context.getString(R.string.status_delete)};
        final Selection selection = new Selection();
        selection.index = INDICES[index];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.status_dialog_title));
        builder.setSingleChoiceItems(items, selection.index, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                selection.index = item;
            }
        });
        builder.setPositiveButton(R.string.status_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                listener.statusChanged(key, STATUSES[selection.index]);
                dialog.dismiss();
            }
        });
        return builder.create();
    }

}
