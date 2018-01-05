package net.sparkly.pixely.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.sparkly.pixely.GlideApp;
import net.sparkly.pixely.R;
import net.sparkly.pixely.utils.PixUtils;
import net.sparkly.pixely.activities.EditorActivity;

import java.util.ArrayList;
import java.util.List;

public class LocalMasonryGrid extends RecyclerView.Adapter<LocalMasonryGrid.MasonryView>

{

    private Context context;
    private List<String> mfeed = new ArrayList<>();

    public LocalMasonryGrid(Context context, List<String> mfeed, FragmentActivity activity)
    {
        this.context = context;
        this.mfeed = mfeed;
    }

    @Override
    public MasonryView onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.local_photo_item, parent, false);
        return new MasonryView(layoutView);
    }

    @Override
    public void onBindViewHolder(final MasonryView holder, @SuppressLint("RecyclerView") final int position)
    {
        final String current = mfeed.get(position);

        GlideApp.with(context)
                .load("file://" + current)
                .override(PixUtils.dpTopx(118), PixUtils.dpTopx(118))
                .dontAnimate()
                .skipMemoryCache(true)
                .listener(new RequestListener<Drawable>()
                {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
                    {
                        holder.loading.setVisibility(View.GONE);
                        mfeed.remove(position);
                        LocalMasonryGrid.super.notifyItemRemoved(position);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource)
                    {
                        holder.loading.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.photo);

        holder.photo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent activity = new Intent(context, EditorActivity.class);
                activity.putExtra("photoUri", current);
                activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activity);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return mfeed.size();
    }

    class MasonryView extends RecyclerView.ViewHolder
    {
        ProgressBar loading;
        ImageView photo;

        MasonryView(View itemView)
        {
            super(itemView);
            photo = itemView.findViewById(R.id.photo);
            loading = itemView.findViewById(R.id.loading);
        }

    }
}
