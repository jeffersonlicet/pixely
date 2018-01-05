package net.sparkly.pixely.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import net.sparkly.pixely.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends BaseActivity
{

    @BindView(R.id.frontalSmallImage)
    RadioButton frontalSmallImage;

    @BindView(R.id.frontalMediumImage)
    RadioButton frontalMediumImage;

    @BindView(R.id.frontalBestImage)
    RadioButton frontalBestImage;

    @BindView(R.id.rearSmallImage)
    RadioButton rearSmallImage;

    @BindView(R.id.rearMediumImage)
    RadioButton rearMediumImage;

    @BindView(R.id.rearBestImage)
    RadioButton rearBestImage;

    @OnClick({R.id.frontalSmallImage, R.id.frontalMediumImage, R.id.frontalBestImage,
            R.id.rearSmallImage, R.id.rearMediumImage, R.id.rearBestImage})
    public void onClickManager(View view)
    {
        switch (view.getId())
        {
            case R.id.frontalSmallImage:
            case R.id.frontalMediumImage:
            case R.id.frontalBestImage:
                changeFrontalImageQuality(view);
                break;

            case R.id.rearSmallImage:
            case R.id.rearMediumImage:
            case R.id.rearBestImage:
                changeRearImageQuality(view);
                break;
        }
    }

    private void changeFrontalImageQuality(View view)
    {
        if (((RadioButton) view).isChecked())
        {
            setBoolean("frontalQualityChanged", true);

            switch (view.getId())
            {
                case R.id.frontalSmallImage:
                    setInteger("frontalImageQuality", 0);
                    break;
                case R.id.frontalMediumImage:
                    setInteger("frontalImageQuality", 1);
                    break;
                case R.id.frontalBestImage:
                    setInteger("frontalImageQuality", 2);
                    break;
            }
        }
    }

    private void changeRearImageQuality(View view)
    {
        if (((RadioButton) view).isChecked())
        {
            setBoolean("rearQualityChanged", true);

            switch (view.getId())
            {
                case R.id.rearSmallImage:
                    setInteger("rearImageQuality", 0);
                    break;
                case R.id.rearMediumImage:
                    setInteger("rearImageQuality", 1);
                    break;
                case R.id.rearBestImage:
                    setInteger("rearImageQuality", 2);
                    break;
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);

        if (getBoolean("frontalQualityChanged"))
        {
            int frontalImageQuality = getInteger("frontalImageQuality");

            switch (frontalImageQuality)
            {
                case 0:
                    frontalSmallImage.setChecked(true);
                    break;
                case 1:
                    frontalMediumImage.setChecked(true);
                    break;
                case 2:
                    frontalBestImage.setChecked(true);
                    break;
            }
        } else frontalBestImage.setChecked(true);


        if (getBoolean("rearQualityChanged"))
        {
            int rearImageQuality = getInteger("rearImageQuality");

            switch (rearImageQuality)
            {
                case 0:
                    rearSmallImage.setChecked(true);
                    break;
                case 1:
                    rearMediumImage.setChecked(true);
                    break;
                case 2:
                    rearBestImage.setChecked(true);
                    break;
            }
        } else rearMediumImage.setChecked(true);

    }

    @Override
    public void onBackPressed()
    {
        startActivity(new Intent(this, CameraActivity.class));
        finish();
    }
}