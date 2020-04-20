package com.litao.android.niftytablayout;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.litao.android.lib.NiftyTabLayout;
import com.litao.android.lib.badge.BadgeDrawable;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity {


    private ViewPager mViewPager;
    private NiftyTabLayout mTablayout;
    private Button mButton;

    private MyPageAdapter adapter;

    private List<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTablayout = findViewById(R.id.tab1);
        mViewPager = findViewById(R.id.viewpager);
        mButton = findViewById(R.id.tap);

        adapter = new MyPageAdapter();


        mViewPager.setAdapter(adapter);

        //设置滑块是否可滑动
        mTablayout.setTabMode(NiftyTabLayout.MODE_AUTO);



        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mTablayout.getTabAt(0).setIcon(R.drawable.test_tab).setText("");
//                adapter.addTab();
                list.add("kkk");
                adapter.setData(list);
//                mTablayout.removeAllTabs();
//                mTablayout.addTab(mTablayout.newTab().setText("男生"));
//                mTablayout.addTab(mTablayout.newTab().setText("出版"));
//                mTablayout.addTab(mTablayout.newTab().setText("女生"));
//                mViewPager.setCurrentItem(1);

                mTablayout.addTab(mTablayout.newTab().setIcon(R.drawable.test_tab));

            }
        });


        mTablayout.setupWithViewPager(mViewPager,false);

        mTablayout.addTab(mTablayout.newTab().setIcon(R.drawable.bd_logo1));
        mTablayout.addTab(mTablayout.newTab().setIcon(R.drawable.test_tab2));
        mTablayout.addTab(mTablayout.newTab().setText("男生"));
        mTablayout.addTab(mTablayout.newTab().setText("女生"));


        list.add("12");
        list.add("234");
        list.add("244");
        list.add("2344");

        adapter.setData(list);


        BadgeDrawable badge = mTablayout.getTabAt(0).getOrCreateBadge();
        badge.setVisible(true);
        badge.setNumber(9999);

        BadgeDrawable badge2 = mTablayout.getTabAt(1).getOrCreateBadge();
        badge2.setVisible(true);
        badge2.setNumber(9999);

//        mTablayout.getTabAt(0).setIcon(R.drawable.test_tab).setText("");

    }



    private class MyPageAdapter extends PagerAdapter {

        private List<String> array = new ArrayList<>();


        private void setData(List data){
            array.clear();
            array.addAll(data);
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return array.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            TextView textView = new TextView(container.getContext());
            textView.setText("position --- >" + position);
            textView.setBackgroundColor(Color.parseColor("#f146ff"));
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            textView.setGravity(Gravity.CENTER);
            container.addView(textView);

            return textView;
        }


        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return "男生" + position;
        }


//        @Override
//        public TabLayout.Tab getTab(int position) {
//            boolean isO = position %2 == 0;
//            if (isO){
//                return new TabLayout.Tab().setText("男生" + position);
//            }else {
//                return new TabLayout.Tab().setTabType(TabLayout.Tab.TYPE_IMAGE).setIcon(R.drawable.test_tab);
//            }
//        }
    }
}