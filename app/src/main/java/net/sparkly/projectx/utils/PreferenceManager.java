package net.sparkly.projectx.utils;
import android.content.SharedPreferences;

/**
 * Created by Jefferson on 28/07/2016.
 */
public interface PreferenceManager {

    SharedPreferences getPreferences();
    SharedPreferences.Editor editPreferences();

    void setString(String key, String value);
    String getString(String key);

    void setBoolean(String key, boolean value);
    boolean getBoolean(String key);

    void setInteger(String key, int value);
    int getInteger(String key);

    void setFloat(String key, float value);
    float getFloat(String key);

}