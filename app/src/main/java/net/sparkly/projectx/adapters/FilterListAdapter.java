package net.sparkly.projectx.adapters;

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
import android.widget.TextView;

import com.yarolegovich.discretescrollview.DiscreteScrollView;

import net.sparkly.projectx.R;
import net.sparkly.projectx.models.FilterItem;
import net.sparkly.projectx.models.SingleModeItem;

import java.util.List;

public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.ViewHolder>
{
    private List<FilterItem> filters;
    private Context context;
    private DiscreteScrollView recyclerView;
    private List<Integer> selectedFilter;
    private int selected;
    private FilterItemClickListener filterItemClickListener;

    public FilterListAdapter(Context context, List<FilterItem> filters,
                             DiscreteScrollView modeSelector, List<Integer> selectedFilters, FilterItemClickListener filterItemClickListener)
    {
        this.context = context;
        this.filters = filters;
        recyclerView = modeSelector;
        this.selectedFilter = selectedFilters;
        this.filterItemClickListener = filterItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_filter_item, parent, false));
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
                    recyclerView.smoothScrollToPosition(holder.getAdapterPosition());
                    if (selected == filters.get(holder.getAdapterPosition()).getId())
                        filterItemClickListener.onClickSelectedItemListener(selected);

                    else selected = filters.get(holder.getAdapterPosition()).getId();
                }
            }
        });

        try
        {
            Bitmap original = null;

            if (filters.get(holder.getAdapterPosition()).getId() == 0)
            {
                holder.filterItemThumbnail.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_shutter_inside));

            } else
            {
                original = BitmapFactory.decodeResource(context.getResources(), filters.get(holder.getAdapterPosition()).getThumbnail());
                Bitmap b = Bitmap.createScaledBitmap(original, 100, 100, false);

                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), b);
                roundedBitmapDrawable.setCircular(true);
                holder.filterItemThumbnail.setImageDrawable(roundedBitmapDrawable);
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

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout filterWrapper;
        ImageView filterItemThumbnail;

        public ViewHolder(View itemView)
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
