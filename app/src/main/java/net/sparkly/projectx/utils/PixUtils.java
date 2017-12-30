package net.sparkly.projectx.utils;

import android.content.res.Resources;

public class PixUtils
{
    public static int dpTopx(int dp)
    {
        return (int)(dp* Resources.getSystem().getDisplayMetrics().density);
    }
}
