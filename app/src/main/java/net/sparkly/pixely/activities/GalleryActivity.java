package net.sparkly.pixely.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import net.sparkly.pixely.R;
import net.sparkly.pixely.adapters.LocalMasonryGrid;
import net.sparkly.pixely.helpers.EndlessRecyclerViewScrollListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private LocalMasonryGrid mAdapter;
    private List<String> mPhotos = new ArrayList<>();

    private int nPage = 0;
    private boolean hasLoaded;
    private final static int nItems = 30;

    @BindView(R.id.mRecycler)
    RecyclerView mRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ButterKnife.bind(this);

        final GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 3);

        mRecycler.setLayoutManager(layoutManager);
        mAdapter = new LocalMasonryGrid(getApplicationContext(), mPhotos, this);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.setHasFixedSize(false);

        mRecycler.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (nPage > 5 || !hasLoaded)
                    return;

                nPage++;

                fetchMedia();
            }
        });

        if (!hasLoaded) {
            hasLoaded = true;
            fetchMedia();
        }

    }

    private void fetchMedia() {
        getSupportLoaderManager().restartLoader(1, null, this);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, CameraActivity.class));
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String[] projection = {
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        };

        final String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
        final String orderBy = MediaStore.Images.Media.DATE_ADDED + " desc limit " + nItems + " offset " + nItems * nPage;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        return new CursorLoader(this, queryUri, projection, selection, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        while (cursor != null && cursor.moveToNext()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            final String imagePath = cursor.getString(columnIndex);
            File file = new File(imagePath);

            if (file.exists()) {
                mRecycler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPhotos.add(imagePath);
                        mAdapter.notifyItemInserted(mPhotos.size() - 1);
                    }
                });

            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
