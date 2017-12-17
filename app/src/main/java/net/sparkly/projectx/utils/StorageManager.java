package net.sparkly.projectx.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.snatik.storage.Storage;

import java.io.File;
import java.util.List;

public class StorageManager {

    private Context context;
    private Storage storage;
    private String storagePath;

    public StorageManager(Context context) {
        storage = new Storage(context);

        if (Storage.isExternalWritable()) {
            storagePath = storage.getExternalStorageDirectory(Environment.DIRECTORY_PICTURES) + File.separator + context.getString(org.wysaid.library.R.string.app_name);
        } else {
            storagePath = storage.getInternalFilesDirectory();
        }
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
