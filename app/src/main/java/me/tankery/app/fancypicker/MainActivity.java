package me.tankery.app.fancypicker;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.widget.TextView;

import me.tankery.lib.fancypicker.FancyPickerItem;


public class MainActivity extends Activity implements FancyPickerItem.OnFancyPickerItemChangeListener {

    @IdRes static final int[] fancyItemIds = {
            R.id.fancy_item_1, R.id.fancy_item_2, R.id.fancy_item_3
    };
    FancyPickerItem[] fancyPickerItems = new FancyPickerItem[fancyItemIds.length];

    TextView fancyValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < fancyItemIds.length; i++) {
            fancyPickerItems[i] = (FancyPickerItem) findViewById(fancyItemIds[i]);
            fancyPickerItems[i].addOnFancyPickerItemChangeListener(this);
        }
        fancyValue = (TextView) findViewById(R.id.text_fancy_value);
    }

    @Override
    public void onProgressChanged(FancyPickerItem pickerItem, float progress, boolean fromUser) {
        pickerItem.setText(fromProgress(progress));
        fancyValue.setText(fromProgress(progress));
    }

    @Override
    public void onStartTrackingTouch(FancyPickerItem pickerItem) {
        fancyValue.setText(fromProgress(pickerItem.getProgress()));
    }

    @Override
    public void onStopTrackingTouch(FancyPickerItem pickerItem) {
    }

    @Override
    public void onEndTrackingAnimation(FancyPickerItem pickerItem) {
        fancyValue.setText(R.string.fancy_item_value);
    }

    private String fromProgress(float progress) {
        return String.valueOf((int) progress);
    }

}
