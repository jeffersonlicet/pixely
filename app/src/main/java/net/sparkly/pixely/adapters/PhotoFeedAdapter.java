package net.sparkly.pixely.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.RequestBuilder;

import net.sparkly.pixely.GlideApp;
import net.sparkly.pixely.R;
import net.sparkly.pixely.models.PhotoItem;
import net.sparkly.pixely.utils.StorageManager;

import java.util.List;

import static com.bumptech.glide.request.RequestOptions.fitCenterTransform;

public class PhotoFeedAdapter extends RecyclerView.Adapter<PhotoFeedAdapter.ViewHolder>
{
    private List<PhotoItem> photos;
    private Context context;
    private StorageManager storageManager;
    private StorageManager privateStorageManager;

    public PhotoFeedAdapter(Context context,  List<PhotoItem> photos, StorageManager privateStorageManager,
                            StorageManager storageManager)
    {
        this.context = context;
        this.photos = photos;
        this.storageManager = storageManager;
        this.privateStorageManager = privateStorageManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_photo_big_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        if(photos.get(holder.getAdapterPosition()).getBigPath() != null)
        {
            RequestBuilder<Drawable> thumbnailRequest = GlideApp
                    .with(context)
                    .load(photos.get(holder.getAdapterPosition()).getThumbPath());

            GlideApp.with(context)
                    .load(photos.get(holder.getAdapterPosition()).getBigPath())
                    .thumbnail(thumbnailRequest)
                    .apply(fitCenterTransform())
                    .into(holder.photoThumbnail);

            holder.progressWorkingIndicator.setVisibility(View.GONE);
        } else {
            GlideApp.with(context)
                    .load(photos.get(holder.getAdapterPosition()).getThumbPath())
                    .apply(fitCenterTransform())
                    .into(holder.photoThumbnail);
        }
    }

    @Override
    public int getItemCount()
    {
        return photos == null ? 0 : photos.size();
    }



     class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView photoThumbnail;
        ProgressBar progressWorkingIndicator;

        ViewHolder(View itemView)
        {
            super(itemView);
            photoThumbnail = itemView.findViewById(R.id.photoThumbnail);
            progressWorkingIndicator = itemView.findViewById(R.id.progressWorkingIndicator);

       }

    }
}
