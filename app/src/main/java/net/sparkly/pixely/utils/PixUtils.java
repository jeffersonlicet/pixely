package net.sparkly.pixely.utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class PixUtils {
    public static int dpTopx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static String contentToPath(Context context, Uri content) {
        Cursor vidCursor = context.getContentResolver().query(content, null, null,
            null, null);

        if (vidCursor != null && vidCursor.moveToFirst()) {
            int column_index =
                vidCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            Uri filePathUri = Uri.parse(vidCursor.getString(column_index));
            vidCursor.close();
            return filePathUri.getPath();
        }

        return null;
    }
}
