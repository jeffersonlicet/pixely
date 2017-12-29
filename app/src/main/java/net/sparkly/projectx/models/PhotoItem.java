package net.sparkly.projectx.models;


import android.net.Uri;

public class PhotoItem
{
    private Uri photoUri;
    private String thumbPath;
    private String bigPath;

    public PhotoItem()
    {

    }

    public PhotoItem(String thumbPath)
    {
        this.thumbPath = thumbPath;
    }

    public PhotoItem(String thumbPath, String bigPath)
    {
        this.thumbPath = thumbPath;
        this.bigPath = bigPath;
    }

    public Uri getPhotoUri()
    {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri)
    {
        this.photoUri = photoUri;
    }

    public String getBigPath()
    {
        return bigPath;
    }

    public void setBigPath(String bigPath)
    {
        this.bigPath = bigPath;
    }

    public String getThumbPath()
    {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath)
    {
        this.thumbPath = thumbPath;
    }
}
