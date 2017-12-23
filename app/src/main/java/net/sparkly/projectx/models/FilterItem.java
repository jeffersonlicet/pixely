package net.sparkly.projectx.models;


public class FilterItem
{
    private String params;
    private String name;
    private int thumbnail;
    private int id;
    private float intensity;

    public FilterItem(int filterId, String name, String params, float intensity, int thumbnail)
    {
        id = filterId;
        this.params = params;
        this.intensity = intensity;
        this.thumbnail  = thumbnail;
        this.name = name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public float getIntensity()
    {
        return intensity;
    }

    public void setIntensity(float intensity)
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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
