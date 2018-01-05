package net.sparkly.pixely.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

import java.io.File;
import java.util.List;

import static com.bumptech.glide.request.RequestOptions.fitCenterTransform;

public class PhotoFeedAdapter extends RecyclerView.Adapter<PhotoFeedAdapter.ViewHolder> {
    private List<PhotoItem> photos;
    private Context context;
    private StorageManager storageManager;
    private StorageManager privateStorageManager;

    public PhotoFeedAdapter(Context context, List<PhotoItem> photos, StorageManager privateStorageManager,
                            StorageManager storageManager) {
        this.context = context;
        this.photos = photos;
        this.storageManager = storageManager;
        this.privateStorageManager = privateStorageManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.single_photo_big_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (photos.get(holder.getAdapterPosition()).getBigPath() != null) {
            RequestBuilder<Drawable> thumbnailRequest = GlideApp
                .with(context)
                .load(photos.get(holder.getAdapterPosition()).getThumbPath());

            GlideApp.with(context)
                .load(photos.get(holder.getAdapterPosition()).getBigPath())
                .thumbnail(thumbnailRequest)
                .apply(fitCenterTransform())
                .into(holder.photoThumbnail);

            holder.shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = photos.get(holder.getAdapterPosition()).getBigPath();
                    Log.d("Shareing", url);
                    final File photoFile = new File(url);
                    final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpg");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
                    context.startActivity(Intent.createChooser(shareIntent, "Share"));
                }
            });

            holder.progressWorkingIndicator.setVisibility(View.GONE);
        } else {
            GlideApp.with(context)
                .load(photos.get(holder.getAdapterPosition()).getThumbPath())
                .apply(fitCenterTransform())
                .into(holder.photoThumbnail);

            holder.shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = photos.get(holder.getAdapterPosition()).getThumbPath();
                    Log.d("Shareing", url);
                    final File photoFile = new File(url);
                    final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpg");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
                    context.startActivity(Intent.createChooser(shareIntent, "Share"));
                }
            });
        }


    }

    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photoThumbnail;
        ProgressBar progressWorkingIndicator;
        FloatingActionButton shareButton;

        ViewHolder(View itemView) {
            super(itemView);
            photoThumbnail = itemView.findViewById(R.id.photoThumbnail);
            progressWorkingIndicator = itemView.findViewById(R.id.progressWorkingIndicator);
            shareButton = itemView.findViewById(R.id.shareButton);
        }

    }
}
