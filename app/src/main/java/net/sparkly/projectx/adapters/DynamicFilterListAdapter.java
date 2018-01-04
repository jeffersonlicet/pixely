package net.sparkly.projectx.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.yarolegovich.discretescrollview.DiscreteScrollView;

import net.sparkly.projectx.R;
import net.sparkly.projectx.models.FilterItem;

import org.wysaid.nativePort.CGEImageHandler;
import org.wysaid.view.ImageGLSurfaceView;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DynamicFilterListAdapter extends RecyclerView.Adapter<DynamicFilterListAdapter.ViewHolder>
{
    private List<FilterItem> filters;
    private Context context;
    private DiscreteScrollView recyclerView;
    private int selected;
    private FilterItemClickListener filterItemClickListener;
    private String photoUri;
    private Activity activity;

    public DynamicFilterListAdapter(Context context, List<FilterItem> filters,
                                    DiscreteScrollView modeSelector,
                                    List<Integer> selectedFilters,
                                    FilterItemClickListener filterItemClickListener,
                                    String photoUri,
                                    Activity activity) {
        this.context = context;
        this.filters = filters;
        recyclerView = modeSelector;
        this.filterItemClickListener = filterItemClickListener;
        this.photoUri = photoUri;
        this.activity = activity;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_filter_surface_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {

        holder.filterWrapper.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (recyclerView != null)
                {
                    if (selected == holder.getAdapterPosition())
                        filterItemClickListener.onClickSelectedItemListener(selected);

                    else
                    {
                        selected = holder.getAdapterPosition();
                        recyclerView.smoothScrollToPosition(selected);
                    }
                }
            }
        });

        try
        {
                if (filters.get(holder.getAdapterPosition()).getId() == 0) {
                    final FutureTarget<Bitmap> futureTarget =
                            Glide.with(context)
                                    .asBitmap()
                                    .apply(RequestOptions.circleCropTransform())
                                    .load(context.getResources().getDrawable(R.drawable.camera_shutter_inside))
                                    .submit();

                    holder.filterItemThumbnail.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_shutter_inside));

                } else {

                    final FutureTarget<Bitmap> futureTarget =
                            Glide.with(context)
                                    .asBitmap()
                                    .apply(RequestOptions.circleCropTransform())
                                    .load(new File(photoUri))
                                    .submit(100, 100);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final Bitmap bmp = futureTarget.get();
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        holder.filterItemThumbnail.setSurfaceCreatedCallback(new ImageGLSurfaceView.OnSurfaceCreatedCallback() {
                                            @Override
                                            public void surfaceCreated() {
                                                holder.filterItemThumbnail.setImageBitmap(bmp);
                                                holder.filterItemThumbnail.setFilterWithConfig("");
                                            }
                                        });
                                    }
                                });

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();



                    //holder.filterItemThumbnail.setImageBitmap(bitmap);

                   /* new Thread(new Runnable() {
                        @Override
                        public void run() {
                            holder.filterItemThumbnail.queueEvent(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.d("DynamicFilter", currentFilter.getParams() + " filters params");
                                    Log.d("DynamicFilter", currentFilter.getIntensity() + " filters intensity");

                                    CGEImageHandler handler = holder.filterItemThumbnail.getImageHandler();
                                    handler.setFilterWithConfig(currentFilter.getParams(), true, false);
                                    handler.setFilterIntensity(currentFilter.getIntensity() , true);

                                    handler.revertImage();
                                    handler.processFilters();

                                    holder.filterItemThumbnail.requestRender();
                                }
                            });
                        }
                    }).run();*/

                }

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public int getItemCount()
    {
        return filters == null ? 0 : filters.size();
    }

    public void notifyChangedItem(int adapterPosition)
    {
        selected = adapterPosition;
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout filterWrapper;
        ImageGLSurfaceView filterItemThumbnail;

        ViewHolder(View itemView)
        {
            super(itemView);
            filterWrapper = itemView.findViewById(R.id.filterItemWrapper);
            filterItemThumbnail = itemView.findViewById(R.id.filterItemThumbnail);
        }

    }

    public interface FilterItemClickListener
    {
        void onClickSelectedItemListener(int selected);
    }
}
