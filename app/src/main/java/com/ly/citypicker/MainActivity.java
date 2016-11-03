package com.ly.citypicker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ly.citypickerview.widget.CityPicker;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button go = (Button) findViewById(R.id.go);
        final TextView tvResult = (TextView) findViewById(R.id.tv_result);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CityPicker cityPicker = new CityPicker.Builder(MainActivity.this).textSize(20)
                        .onlyShowProvinceAndCity(false)
                        .confirmTextColor("#16b3f4")
                        .cancelTextColor("#000000")
                        .province("湖北省")
                        .city("黄冈市")
                        .county("红安县")
                        .textColor(Color.parseColor("#333333"))
                        .provinceCyclic(false)
                        .cityCyclic(false)
                        .countyCyclic(false)
                        .visibleItemsCount(7)
                        .itemPadding(10)
                        .build();

                cityPicker.show();
                cityPicker.setOnCityItemClickListener(new CityPicker.OnCityItemClickListener() {
                    @Override
                    public void onSelected(String... citySelected) {
                        tvResult.setText("选择结果：\n省：" + citySelected[0] + "\n市：" + citySelected[1] + "\n县：" + citySelected[2]
                                + "\nID：" + citySelected[3]);
                    }
                });
            }
        });
    }
}
