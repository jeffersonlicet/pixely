package net.sparkly.pixely.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yarolegovich.discretescrollview.DiscreteScrollView;

import net.sparkly.pixely.R;
import net.sparkly.pixely.models.SingleModeItem;

import java.util.List;

public class ModeListAdapter extends RecyclerView.Adapter<ModeListAdapter.ViewHolder>
{
    private List<SingleModeItem> modes;
    private Context context;
    private DiscreteScrollView recyclerView;
    private List<Integer> selectedCameraModes;

    public ModeListAdapter(Context context, List<SingleModeItem> modes, DiscreteScrollView modeSelector, List<Integer> selectedCameraModes)
    {
        this.context = context;
        this.modes = modes;
        recyclerView = modeSelector;
        this.selectedCameraModes = selectedCameraModes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_mode_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        SingleModeItem item = modes.get(position);
        holder.title.setText(item.getTitle());

        holder.modeWrapper.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (recyclerView != null)
                {
                    recyclerView.smoothScrollToPosition(holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return modes == null ? 0 : modes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {

        TextView title;
        RelativeLayout modeWrapper;

        public ViewHolder(View itemView)
        {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            modeWrapper = itemView.findViewById(R.id.modeWrapper);
        }


    }
}
