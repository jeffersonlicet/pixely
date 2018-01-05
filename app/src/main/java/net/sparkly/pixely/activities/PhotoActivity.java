package net.sparkly.pixely.activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.chrisbanes.photoview.PhotoView;

import net.sparkly.pixely.GlideApp;
import net.sparkly.pixely.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PhotoActivity extends AppCompatActivity {

    @BindView(R.id.imagePreview)
    PhotoView imagePreview;

    @OnClick(R.id.shareButton)
    public void sharePhoto()
    {

        Log.d("Shareing", url);
        final File photoFile = new File(url);
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);

        url = getIntent().getStringExtra("photoUri");

        GlideApp
                .with(this)
                .load(new File(url))
                .into(imagePreview);
    }
}
