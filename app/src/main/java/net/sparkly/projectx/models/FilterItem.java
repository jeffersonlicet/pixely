package net.sparkly.projectx.models;

import android.graphics.drawable.Drawable;

public class FilterItem
{
    private String params;
    private int thumbnail;
    private int id;
    private int intensity;

    public FilterItem(int i, String params, int intensity, int thumbnail)
    {
        id = i;
        this.params = params;
        this.intensity = intensity;
        this.thumbnail  = thumbnail;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getIntensity()
    {
        return intensity;
    }

    public void setIntensity(int intensity)
    {
        this.intensity = intensity;
    }

    public int getThumbnail()
    {
        return thumbnail;
    }

    public void setThumbnail(int thumbnail)
    {
        this.thumbnail = thumbnail;
    }

    public String getParams()
    {
        return params;
    }

    public void setParams(String params)
    {
        this.params = params;
    }
}
