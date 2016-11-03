package com.ly.citypickerview.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ly.citypickerview.R;
import com.ly.citypickerview.model.City;
import com.ly.citypickerview.model.County;
import com.ly.citypickerview.model.Province;
import com.ly.citypickerview.widget.wheel.OnWheelChangedListener;
import com.ly.citypickerview.widget.wheel.WheelView;
import com.ly.citypickerview.widget.wheel.adapters.ArrayWheelAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CityPicker implements OnWheelChangedListener {

    private Context mContext;
    private PopupWindow mPwHome;
    private View mPwView;
    private WheelView mViewProvince;
    private WheelView mViewCity;
    private WheelView mViewCounty;
    private TextView mTvOK;
    private TextView mTvCancel;
    private List<Province> mProvinceList = new ArrayList<>();
    private List<City> mCityList = new ArrayList<>();
    private List<County> mCountyList = new ArrayList<>();
    /**
     * 当前省的名称
     */
    protected String mCurrentProvinceName;
    /**
     * 当前市的名称
     */
    protected String mCurrentCityName;
    /**
     * 当前区的名称
     */
    protected String mCurrentCountyName = "";
    /**
     * 当前区的ID
     */
    protected String mCurrentID = "";
    private OnCityItemClickListener listener;

    public interface OnCityItemClickListener {
        void onSelected(String... citySelected);
    }

    public void setOnCityItemClickListener(OnCityItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Default text color
     */
    public static final int DEFAULT_TEXT_COLOR = 0xFF333333;
    /**
     * Default text size
     */
    public static final int DEFAULT_TEXT_SIZE = 18;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int textSize = DEFAULT_TEXT_SIZE;
    /**
     * 滚轮显示的item个数
     */
    private static final int DEF_VISIBLE_ITEMS = 7;
    private int visibleItems = DEF_VISIBLE_ITEMS;
    /**
     * 省滚轮是否循环滚动
     */
    private boolean isProvinceCyclic = false;
    /**
     * 市滚轮是否循环滚动
     */
    private boolean isCityCyclic = false;
    /**
     * 区滚轮是否循环滚动
     */
    private boolean isCountyCyclic = false;
    /**
     * item间距
     */
    private int padding = 10;
    /**
     * 取消按钮颜色
     */
    private String cancelTextColorStr = "#000000";
    /**
     * 确认按钮颜色
     */
    private String confirmTextColorStr = "#16b3f4";
    /**
     * 第一次默认显示的省份，一般配合定位使用
     */
    private String defaultProvinceName = "北京市";
    /**
     * 第一次默认显示的城市，一般配合定位使用
     */
    private String defaultCityName = "东城区";
    /**
     * 第一次默认显示的区县，一般配合定位使用
     */
    private String defaultCounty = "";
    /**
     * 两级联动
     */
    private boolean showProvinceAndCity = false;

    private CityPicker(Builder builder) {
        this.textColor = builder.textColor;
        this.textSize = builder.textSize;
        this.visibleItems = builder.visibleItems;
        this.isProvinceCyclic = builder.isProvinceCyclic;
        this.isCountyCyclic = builder.isCountyCyclic;
        this.isCityCyclic = builder.isCityCyclic;
        this.mContext = builder.mContext;
        this.padding = builder.padding;

        this.confirmTextColorStr = builder.confirmTextColorStr;
        this.cancelTextColorStr = builder.cancelTextColorStr;

        this.defaultCounty = builder.defaultCounty;
        this.defaultCityName = builder.defaultCityName;
        this.defaultProvinceName = builder.defaultProvinceName;

        this.showProvinceAndCity = builder.showProvinceAndCity;

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        mPwView = layoutInflater.inflate(R.layout.pop_citypicker, null);

        mViewProvince = (WheelView) mPwView.findViewById(R.id.id_province);
        mViewCity = (WheelView) mPwView.findViewById(R.id.id_city);
        mViewCounty = (WheelView) mPwView.findViewById(R.id.id_county);
        mTvOK = (TextView) mPwView.findViewById(R.id.tv_confirm);
        mTvCancel = (TextView) mPwView.findViewById(R.id.tv_cancel);

        mPwHome = new PopupWindow(mPwView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mPwHome.setBackgroundDrawable(new ColorDrawable(0x80000000));
        mPwHome.setTouchable(true);
        mPwHome.setOutsideTouchable(true);
        mPwHome.setFocusable(true);

        //设置确认按钮文字颜色
        if (!TextUtils.isEmpty(this.confirmTextColorStr)) {
            mTvOK.setTextColor(Color.parseColor(this.confirmTextColorStr));
        }

        //设置取消按钮文字颜色
        if (!TextUtils.isEmpty(this.cancelTextColorStr)) {
            mTvCancel.setTextColor(Color.parseColor(this.cancelTextColorStr));
        }

        //只显示省市两级联动
        if (this.showProvinceAndCity) {
            mViewCounty.setVisibility(View.GONE);
        } else {
            mViewCounty.setVisibility(View.VISIBLE);
        }

        //初始化城市数据
        initAllDatas(mContext);

        mViewProvince.addChangingListener(this);
        mViewCity.addChangingListener(this);
        mViewCounty.addChangingListener(this);
        // 添加onclick事件
        mTvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        mTvOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showProvinceAndCity) {
                    listener.onSelected(mCurrentProvinceName, mCurrentCityName, "", mCurrentID);
                } else {
                    listener.onSelected(mCurrentProvinceName, mCurrentCityName, mCurrentCountyName, mCurrentID);
                }
                hide();
            }
        });

    }

    public static class Builder {
        /**
         * Default text color
         */
        public static final int DEFAULT_TEXT_COLOR = 0xFF333333;
        /**
         * Default text size
         */
        public static final int DEFAULT_TEXT_SIZE = 18;
        private int textColor = DEFAULT_TEXT_COLOR;
        private int textSize = DEFAULT_TEXT_SIZE;
        /**
         * 滚轮显示的item个数
         */
        private static final int DEF_VISIBLE_ITEMS = 7;
        private int visibleItems = DEF_VISIBLE_ITEMS;
        /**
         * 省滚轮是否循环滚动
         */
        private boolean isProvinceCyclic = false;
        /**
         * 市滚轮是否循环滚动
         */
        private boolean isCityCyclic = false;
        /**
         * 区滚轮是否循环滚动
         */
        private boolean isCountyCyclic = false;
        private Context mContext;
        /**
         * item间距
         */
        private int padding = 10;
        /**
         * 取消按钮颜色
         */
        private String cancelTextColorStr = "#000000";
        /**
         * 确认按钮颜色
         */
        private String confirmTextColorStr = "#16b3f4";
        /**
         * 第一次默认显示的省份，一般配合定位，使用
         */
        private String defaultProvinceName = "北京市";
        /**
         * 第一次默认显示的城市，一般配合定位，使用
         */
        private String defaultCityName = "东城区";
        /**
         * 第一次默认显示的区县，一般配合定位，使用
         */
        private String defaultCounty = "";
        /**
         * 两级联动
         */
        private boolean showProvinceAndCity = false;

        public Builder(Context context) {
            this.mContext = context;
        }

        /**
         * 是否只显示省市两级联动
         *
         * @param flag
         * @return
         */
        public Builder onlyShowProvinceAndCity(boolean flag) {
            this.showProvinceAndCity = flag;
            return this;
        }

        /**
         * 第一次默认的显示省份，一般配合定位，使用
         *
         * @param defaultProvinceName
         * @return
         */
        public Builder province(String defaultProvinceName) {
            this.defaultProvinceName = defaultProvinceName;
            return this;
        }

        /**
         * 第一次默认得显示城市，一般配合定位，使用
         *
         * @param defaultCityName
         * @return
         */
        public Builder city(String defaultCityName) {
            this.defaultCityName = defaultCityName;
            return this;
        }

        /**
         * 第一次默认地区显示，一般配合定位，使用
         *
         * @param defaultCounty
         * @return
         */
        public Builder county(String defaultCounty) {
            this.defaultCounty = defaultCounty;
            return this;
        }

        /**
         * 确认按钮文字颜色
         *
         * @param color
         * @return
         */
        public Builder confirTextColor(String color) {
            this.confirmTextColorStr = color;
            return this;
        }

        /**
         * 取消按钮文字颜色
         *
         * @param color
         * @return
         */
        public Builder cancelTextColor(String color) {
            this.cancelTextColorStr = color;
            return this;
        }

        /**
         * item文字颜色
         *
         * @param textColor
         * @return
         */
        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        /**
         * item文字大小
         *
         * @param textSize
         * @return
         */
        public Builder textSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        /**
         * 滚轮显示的item个数
         *
         * @param visibleItems
         * @return
         */
        public Builder visibleItemsCount(int visibleItems) {
            this.visibleItems = visibleItems;
            return this;
        }

        /**
         * 省滚轮是否循环滚动
         *
         * @param isProvinceCyclic
         * @return
         */
        public Builder provinceCyclic(boolean isProvinceCyclic) {
            this.isProvinceCyclic = isProvinceCyclic;
            return this;
        }

        /**
         * 市滚轮是否循环滚动
         *
         * @param isCityCyclic
         * @return
         */
        public Builder cityCyclic(boolean isCityCyclic) {
            this.isCityCyclic = isCityCyclic;
            return this;
        }

        /**
         * 区滚轮是否循环滚动
         *
         * @param isCountyCyclic
         * @return
         */
        public Builder countyCyclic(boolean isCountyCyclic) {
            this.isCountyCyclic = isCountyCyclic;
            return this;
        }

        /**
         * item间距
         *
         * @param itemPadding
         * @return
         */
        public Builder itemPadding(int itemPadding) {
            this.padding = itemPadding;
            return this;
        }

        public CityPicker build() {
            CityPicker cityPicker = new CityPicker(this);
            return cityPicker;
        }

    }

    /**
     * 解析省市区的JSON数据
     */

    protected void initAllDatas(Context context) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open("area.txt"));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            String result = "";
            while ((line = bufReader.readLine()) != null)
                result += line;
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Province province = new Province();
                province.setAreaId(jsonObject.getString("areaId"));
                province.setAreaName(jsonObject.getString("areaName"));
                JSONArray jsonArray2 = new JSONArray(jsonObject.getString("cities"));
                for (int j = 0; j < jsonArray2.length(); j++) {
                    JSONObject jsonObject2 = jsonArray2.getJSONObject(j);
                    City city = new City();
                    city.setAreaId(jsonObject2.getString("areaId"));
                    city.setAreaName(jsonObject2.getString("areaName"));
                    JSONArray jsonArray3 = new JSONArray(jsonObject2.getString("counties"));
                    for (int k = 0; k < jsonArray3.length(); k++) {
                        JSONObject jsonObject3 = jsonArray3.getJSONObject(k);
                        County county = new County();
                        county.setAreaId(jsonObject3.getString("areaId"));
                        county.setAreaName(jsonObject3.getString("areaName"));
                        city.counties.add(county);
                    }
                    province.cities.add(city);
                }
                mProvinceList.add(province);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新省WheelView的信息
     */
    private void updateProvinces() {
        int provinceDefault = -1;
        if (!TextUtils.isEmpty(defaultProvinceName) && mProvinceList.size() > 0) {
            for (int i = 0; i < mProvinceList.size(); i++) {
                if (mProvinceList.get(i).getAreaName().contains(defaultProvinceName)) {
                    provinceDefault = i;
                    break;
                }
            }
        }
        List<String> list = new ArrayList<>();
        for (Province province : mProvinceList) {
            list.add(province.getAreaName());
        }
        String[] provinces = list.toArray(new String[list.size()]);
        ArrayWheelAdapter arrayWheelAdapter = new ArrayWheelAdapter<>(mContext, provinces);
        mViewProvince.setViewAdapter(arrayWheelAdapter);
        //获取所设置的省的位置，直接定位到该位置
        if (-1 != provinceDefault) {
            mViewProvince.setCurrentItem(provinceDefault);
        }
        // 设置可见条目数量
        mViewProvince.setVisibleItems(visibleItems);
        mViewCity.setVisibleItems(visibleItems);
        mViewCounty.setVisibleItems(visibleItems);
        mViewProvince.setCyclic(isProvinceCyclic);
        mViewCity.setCyclic(isCityCyclic);
        mViewCounty.setCyclic(isCountyCyclic);
        arrayWheelAdapter.setPadding(padding);
        arrayWheelAdapter.setTextColor(textColor);
        arrayWheelAdapter.setTextSize(textSize);
        updateCities();
    }

    /**
     * 根据当前的省，更新市WheelView的信息
     */
    private void updateCities() {
        int pCurrent = mViewProvince.getCurrentItem();
        mCurrentProvinceName = mProvinceList.get(pCurrent).getAreaName();
        mCityList = mProvinceList.get(pCurrent).getCities();
        List<String> list = new ArrayList<>();
        for (City city : mCityList) {
            list.add(city.getAreaName());
        }
        String[] cities = list.toArray(new String[list.size()]);
        int cityDefault = -1;
        if (!TextUtils.isEmpty(defaultCityName) && cities.length > 0) {
            for (int i = 0; i < cities.length; i++) {
                if (cities[i].contains(defaultCityName)) {
                    cityDefault = i;
                    break;
                }
            }
        }
        ArrayWheelAdapter cityWheel = new ArrayWheelAdapter<>(mContext, cities);
        // 设置可见条目数量
        cityWheel.setTextColor(textColor);
        cityWheel.setTextSize(textSize);
        mViewCity.setViewAdapter(cityWheel);
        if (-1 != cityDefault) {
            mViewCity.setCurrentItem(cityDefault);
            mCurrentCityName = mCityList.get(cityDefault).getAreaName();
            mCurrentID = mCityList.get(cityDefault).getAreaId();
        } else {
            mViewCity.setCurrentItem(0);
            mCurrentCityName = mCityList.get(0).getAreaName();
            mCurrentID = mCityList.get(0).getAreaId();
        }
        cityWheel.setPadding(padding);
        if (!showProvinceAndCity) {
            updateCounties();
        }
    }

    /**
     * 根据当前的市，更新区WheelView的信息
     */
    private void updateCounties() {
        // 清除之前的
        mCurrentCountyName = "";
        mCurrentID = "";

        int pCurrent = mViewCity.getCurrentItem();
        mCurrentCityName = mCityList.get(pCurrent).getAreaName();
        mCurrentID = mCityList.get(pCurrent).getAreaId();
        mCountyList = mCityList.get(pCurrent).getCounties();
        List<String> list = new ArrayList<>();
        for (County county : mCountyList) {
            list.add(county.getAreaName());
        }
        String[] counties = list.toArray(new String[list.size()]);
        if (counties.length == 0) {
            counties = new String[]{""};
        }
        int countyDefault = -1;
        if (!TextUtils.isEmpty(defaultCounty) && counties.length > 0) {
            for (int i = 0; i < counties.length; i++) {
                if (counties[i].contains(defaultCounty)) {
                    countyDefault = i;
                    break;
                }
            }
        }
        ArrayWheelAdapter countyWheel = new ArrayWheelAdapter<>(mContext, counties);
        // 设置可见条目数量
        countyWheel.setTextColor(textColor);
        countyWheel.setTextSize(textSize);
        mViewCounty.setViewAdapter(countyWheel);
        if (-1 != countyDefault) {
            mViewCounty.setCurrentItem(countyDefault);
            mCurrentCountyName = mCountyList.get(countyDefault).getAreaName();
            mCurrentID = mCountyList.get(countyDefault).getAreaId();
        } else {
            mViewCounty.setCurrentItem(0);
            if (mCountyList.size() > 0) {
                mCurrentCountyName = mCountyList.get(0).getAreaName();
                mCurrentID = mCountyList.get(0).getAreaId();
            }
        }
        countyWheel.setPadding(padding);
    }

    public void show() {
        if (!mPwHome.isShowing()) {
            updateProvinces();
            mPwHome.showAtLocation(mPwView, Gravity.BOTTOM, 0, 0);
        }
    }

    public void hide() {
        if (mPwHome.isShowing()) {
            mPwHome.dismiss();
        }
    }

    @Override
    public void onChanged(WheelView wheel, int oldValue, int newValue) {
        if (wheel == mViewProvince) {
            updateCities();
        } else if (wheel == mViewCity) {
            updateCounties();
        } else if (wheel == mViewCounty) {
            int current = mViewCounty.getCurrentItem();
            mCurrentCountyName = mCountyList.get(current).getAreaName();
            mCurrentID = mCountyList.get(current).getAreaId();
        }
    }
}
