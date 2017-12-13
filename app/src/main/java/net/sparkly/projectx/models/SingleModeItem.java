package net.sparkly.projectx.models;

public class SingleModeItem
{
    private int id;
    private String title;

    public SingleModeItem(int i, String portrait)
    {
        id = i;
        title = portrait;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {

    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
