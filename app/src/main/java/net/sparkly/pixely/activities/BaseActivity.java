package net.sparkly.pixely.activities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import net.sparkly.pixely.helpers.Constants;
import net.sparkly.pixely.utils.PreferenceManager;


public class BaseActivity extends AppCompatActivity implements PreferenceManager {
    private static String TAG = "BaseActivity";

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

    @Override
    public SharedPreferences getPreferences() {
        try {
            return getSharedPreferences(Constants.session_preferences, MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public SharedPreferences.Editor editPreferences() {
        try {
            return getPreferences().edit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void setString(String key, String value) {
        try {
            editPreferences().putString(key, value).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getString(String key) {
        try {
            return getPreferences().getString(key, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public void setBoolean(String key, boolean value) {
        try {
            editPreferences().putBoolean(key, value).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean getBoolean(String key) {
        try {
            return getPreferences().getBoolean(key, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void setInteger(String key, int value) {
        try {
            editPreferences().putInt(key, value).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getInteger(String key) {
        try {
            return getPreferences().getInt(key, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void setFloat(String key, float value) {
        try {
            editPreferences().putFloat(key, value).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public float getFloat(String key) {
        try {
            return getPreferences().getFloat(key, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}