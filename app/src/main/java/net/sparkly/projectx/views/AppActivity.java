package net.sparkly.projectx.views;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.sparkly.projectx.Constants;
import net.sparkly.projectx.utils.PreferenceManager;


public class AppActivity  extends AppCompatActivity implements PreferenceManager
{
    private static String TAG = "AppActivity";

    /**
     * Helper to get resource strings
     */
    public String _Resource(String name) {
        try {
            int resId = getResources().getIdentifier(name, "string", getPackageName());

            if (resId != 0) {
                return getString(resId);
            }

            return "message";
        } catch (Exception ex) {
            return "message";
        }

    }

    /**
     * SharedPreferences interface
     */
    @Override
    public SharedPreferences getPreferences() {
        try {
            return getSharedPreferences(Constants.session_preferences, MODE_PRIVATE);
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return null;
    }

    @Override
    public SharedPreferences.Editor editPreferences() {
        try {
            return getPreferences().edit();
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return null;
    }

    @Override
    public void setString(String key, String value) {
        try {
            editPreferences().putString(key, value).commit();
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public String getString(String key) {
        try {
            return getPreferences().getString(key, "");
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return "";
    }

    @Override
    public void setBoolean(String key, boolean value) {
        try {
            editPreferences().putBoolean(key, value).commit();
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

    }

    @Override
    public boolean getBoolean(String key) {
        try {
            return getPreferences().getBoolean(key, false);
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return false;
    }

    @Override
    public void setInteger(String key, int value) {
        try {
            editPreferences().putInt(key, value).commit();
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public int getInteger(String key) {
        try {
            return getPreferences().getInt(key, 0);
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return 0;
    }

    @Override
    public void setFloat(String key, float value) {
        try {
            editPreferences().putFloat(key, value).commit();
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public float getFloat(String key) {
        try {
            return getPreferences().getFloat(key, 0);
        } catch (Exception e) {
            if(e.getMessage() != null)
                Log.d(TAG, e.getMessage());
        }

        return 0;
    }
}
