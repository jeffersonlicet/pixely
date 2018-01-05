package net.sparkly.pixely.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.snatik.storage.Storage;

import net.sparkly.pixely.R;

import java.io.File;
import java.util.List;

public class StorageManager {

    private Context context;
    private Storage storage;
    private String storagePath;

    public StorageManager(Context context, boolean isPublicStorage) {
        storage = new Storage(context);

        if (Storage.isExternalWritable()) {
            if(isPublicStorage)
                storagePath = storage.getExternalStorageDirectory(Environment.DIRECTORY_PICTURES) + File.separator + context.getString(R.string.app_name);
            else storagePath = storage.getExternalStorageDirectory() + File.separator + "." + context.getString(R.string.app_name);

        } else {
            if(isPublicStorage)
                storagePath = storage.getInternalFilesDirectory() + File.separator + context.getString(R.string.app_name);
            else storagePath = storage.getInternalFilesDirectory() + File.separator + "." + context.getString(R.string.app_name);
        }

        if(!folderExists(storagePath))
            createFolder(storagePath);

        if(!fileExist(".nomedia") && !isPublicStorage)
            createFile(".nomedia", "");
    }

    public boolean createFolder(String name)
    {
        return storage.createDirectory(storagePath + File.separator + name);
    }

    public boolean folderExists(String name)
    {
        return storage.isDirectoryExists(storagePath + File.separator + name);
    }

    public void deleteFile(String path)
    {
        storage.deleteFile(path);
    }

    public List<File> getFiles(String name)
    {
        return storage.getFiles(storagePath + File.separator + name);
    }

    public void createFile(String name, byte[] bytes)
    {
        storage.createFile(storagePath + File.separator + name, bytes);
    }

    public void createFile(String name, String bytes)
    {
        storage.createFile(storagePath + File.separator + name, bytes);
    }

    public File getFile(String path) {
        return storage.getFile(storagePath + File.separator + path);
    }

    public boolean fileExist(String s) {
        return storage.isFileExist(storagePath + File.separator + s);
    }

    public void createFile(String name, Bitmap bmp)
    {
        storage.createFile(storagePath + File.separator + name, bmp);
    }

}
