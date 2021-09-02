package com.sk.weichat.view;import android.content.BroadcastReceiver;import android.content.Context;import android.content.Intent;import android.content.IntentFilter;import android.content.res.TypedArray;import android.graphics.drawable.Drawable;import android.os.Bundle;import android.text.Spannable;import android.text.SpannableString;import android.text.style.ImageSpan;import android.util.AttributeSet;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.ImageView;import android.widget.RadioButton;import android.widget.RadioGroup;import android.widget.RadioGroup.OnCheckedChangeListener;import android.widget.RelativeLayout;import android.widget.Toast;import androidx.annotation.NonNull;import androidx.viewpager.widget.PagerAdapter;import androidx.viewpager.widget.ViewPager;import com.google.android.material.tabs.TabLayout;import com.sk.weichat.MyApplication;import com.sk.weichat.R;import com.sk.weichat.bean.collection.Collectiion;import com.sk.weichat.broadcast.OtherBroadcast;import com.sk.weichat.helper.ImageLoadHelper;import com.sk.weichat.ui.message.ManagerEmojiActivity;import com.sk.weichat.util.DisplayUtil;import com.sk.weichat.util.SmileyParser;import com.sk.weichat.util.filter.EmojiInputFilter;import java.io.Serializable;import java.lang.ref.SoftReference;import java.util.List;import co.ceryle.fitgridview.FitGridAdapter;import co.ceryle.fitgridview.FitGridView;/** * 表情界面 * * @author Administrator */public class ChatFaceView extends RelativeLayout {    private Context mContext;    private ViewPager mViewPager;    private RadioGroup mFaceRadioGroup;// 切换不同组表情的RadioGroup    private boolean mHasGif;    // 表情总数据    private EmotionClickListener mEmotionClickListener;    private BroadcastReceiver refreshCollectionListBroadcast = new BroadcastReceiver() {        @Override        public void onReceive(Context context, Intent intent) {            switchViewPager3();        }    };    public ChatFaceView(Context context) {        super(context);        init(context);    }    public ChatFaceView(Context context, AttributeSet attrs) {        super(context, attrs);        initAttrs(attrs);        init(context);    }    public ChatFaceView(Context context, AttributeSet attrs, int defStyle) {        super(context, attrs, defStyle);        initAttrs(attrs);        init(context);    }    private static int dip_To_px(Context context, int dipValue) {        final float scale = context.getResources().getDisplayMetrics().density;        return (int) (dipValue * scale + 0.5f);    }    private void initAttrs(AttributeSet attrs) {        if (attrs == null) {            return;        }        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChatFaceView);// TypedArray是一个数组容器        mHasGif = a.getBoolean(R.styleable.ChatFaceView_hasGif, true);        a.recycle();    }    @SuppressWarnings("deprecation")    private void init(Context context) {        mContext = context;        LayoutInflater.from(mContext).inflate(R.layout.chat_face_view, this);        mViewPager = (ViewPager) findViewById(R.id.view_pager);        ((TabLayout) findViewById(R.id.tabDots)).setupWithViewPager(mViewPager, false);        mFaceRadioGroup = (RadioGroup) findViewById(R.id.face_btn_layout);        RadioButton rg1 = (RadioButton) findViewById(R.id.default_face);        RadioButton rg2 = (RadioButton) findViewById(R.id.moya_face_gif);        rg1.setText(mContext.getString(R.string.emoji_vc_emoji));        rg2.setText(mContext.getString(R.string.emoji_vc_anma));        mFaceRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {            @Override            public void onCheckedChanged(RadioGroup group, int checkedId) {                switch (checkedId) {                    case R.id.default_face:                        // 表情                        switchViewPager1();                        break;                    case R.id.moya_face_gif:                        // gif                        switchViewPager2();                        break;                    default:                        // 自定义表情                        switchViewPager3();                        break;                }            }        });        mFaceRadioGroup.check(R.id.default_face);        if (!mHasGif) {            mFaceRadioGroup.setVisibility(View.GONE);        }    }    @Override    protected void onAttachedToWindow() {        super.onAttachedToWindow();        getContext().registerReceiver(                refreshCollectionListBroadcast, new IntentFilter(OtherBroadcast.CollectionRefresh_ChatFace));    }    @Override    protected void onDetachedFromWindow() {        super.onDetachedFromWindow();        getContext().unregisterReceiver(refreshCollectionListBroadcast);    }    /*    Emotion     */    private void switchViewPager1() {        EmojiPager1Adapter emojiAdapter = new EmojiPager1Adapter(                getContext(),                SmileyParser.Smilies.getIds(),                SmileyParser.Smilies.getTexts(),                ss -> {                    mEmotionClickListener.onNormalFaceClick(ss);                }        );        mViewPager.setAdapter(emojiAdapter);    }    /*    Gif     */    public void switchViewPager2() {        String[][] strArray = SmileyParser.Gifs.getTexts();        int[][] pngId = SmileyParser.Gifs.getPngIds();        mViewPager.setAdapter(new EmojiPager2Adapter(                getContext(),                pngId,                strArray,                text -> {                    mEmotionClickListener.onGifFaceClick(text);                }        ));    }    /*    Collections     */    public void switchViewPager3() {        if (MyApplication.mCollection == null || MyApplication.mCollection.size() == 0) {            Toast.makeText(mContext, R.string.tip_emoji_empty, Toast.LENGTH_SHORT).show();            return;        }        mViewPager.setAdapter(new EmojiPager3Adapter(                getContext(),                MyApplication.mCollection,                c -> {                    if (c.getType() == 7) {                        Intent intent = new Intent(getContext(), ManagerEmojiActivity.class);                        Bundle bundle = new Bundle();                        bundle.putSerializable("list", (Serializable) MyApplication.mCollection);                        intent.putExtras(bundle);                        mContext.startActivity(intent);                    } else {                        // 发送自定义表情                        mEmotionClickListener.onCollecionClick(c.getUrl());                    }                }        ));    }    public void setEmotionClickListener(EmotionClickListener listener) {        mEmotionClickListener = listener;    }    public interface EmotionClickListener {        void onNormalFaceClick(SpannableString ss);        void onGifFaceClick(String resName);        void onCollecionClick(String collection);    }    interface OnEmojiClickListener {        void onEmojiClick(SpannableString ss);    }    interface OnGifClickListener {        void onGifClick(String text);    }    interface OnCollectionClickListener {        void onCollectionClick(Collectiion c);    }    static class EmojiAdapter extends FitGridAdapter {        private final Context ctx;        private int[] idList;        public EmojiAdapter(Context ctx, int[] idList) {            super(ctx, R.layout.item_face_emotion);            this.ctx = ctx;            this.idList = idList;        }        @Override        public int getCount() {            return idList.length;        }        @Override        public Object getItem(int position) {            return idList[position];        }        @Override        public long getItemId(int position) {            return position;        }        @Override        public boolean hasStableIds() {            return true;        }        @Override        public void onBindView(int position, View view) {            ImageView ivEmoji = (ImageView) view;            if (position >= idList.length) {                ivEmoji.setImageDrawable(null);                return;            }            int res = idList[position];            ivEmoji.setImageResource(res);        }    }    static class GifAdapter extends FitGridAdapter {        private final Context ctx;        private final int[] idList;        GifAdapter(Context ctx, int[] idList) {            super(ctx, R.layout.item_face_gif);            this.ctx = ctx;            this.idList = idList;        }        @Override        public int getCount() {            return idList.length;        }        @Override        public Object getItem(int position) {            return idList[position];        }        @Override        public long getItemId(int position) {            return position;        }        @Override        public void onBindView(int position, View view) {            ImageView ivEmoji = (ImageView) view;            int res = idList[position];            ivEmoji.setImageResource(res);        }    }    static class CollectionAdapter extends FitGridAdapter {        private final Context ctx;        private final List<Collectiion> collectionList;        CollectionAdapter(Context ctx, List<Collectiion> collectionList) {            super(ctx, R.layout.item_face_collection);            this.ctx = ctx;            this.collectionList = collectionList;        }        @Override        public int getCount() {            return collectionList.size();        }        @Override        public Object getItem(int position) {            return collectionList.get(position);        }        @Override        public long getItemId(int position) {            return position;        }        @Override        public void onBindView(int position, View view) {            ImageView ivEmoji = (ImageView) view;            Collectiion c = collectionList.get(position);            // 保留旧代码，            if (c.getType() == 7) {                ivEmoji.setImageResource(R.mipmap.add_emoli_icon);            } else {                String url = c.getUrl();                if (url.endsWith(".gif")) {                    ImageLoadHelper.showGif(                            ctx,                            url,                            ivEmoji                    );                } else {                    ImageLoadHelper.showImageDontAnimateWithPlaceHolder(                            ctx,                            url,                            R.drawable.ffb,                            R.drawable.fez,                            ivEmoji                    );                }            }        }    }    static class CollectionAdapterInit extends FitGridAdapter {        CollectionAdapterInit(Context ctx) {            super(ctx, R.layout.item_face_collection_init);        }        @Override        public int getCount() {            return 1;        }        @Override        public Object getItem(int position) {            return 0;        }        @Override        public long getItemId(int position) {            return position;        }        @Override        public void onBindView(int position, View view) {            ImageView ivEmoji = view.findViewById(R.id.iv_collecton);            ivEmoji.setImageResource(R.mipmap.add_emoli_icon);        }    }    static class EmojiPager1Adapter extends PagerAdapter {        // 弱引用缓存表情第一页，用来加速加载，        private static SoftReference<FitGridView> softFirstPage = new SoftReference<>(null);        private int[][] idMatrix;        // 表情符号所代表的英文字符        private String[][] strMatrix;        private OnEmojiClickListener listener;        private Context ctx;        EmojiPager1Adapter(Context ctx, int[][] idMatrix, String[][] strMatrix, OnEmojiClickListener listener) {            this.ctx = ctx;            this.idMatrix = idMatrix;            this.strMatrix = strMatrix;            this.listener = listener;        }        @Override        public int getCount() {            return idMatrix.length;        }        @Override        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {            container.removeView((View) object);        }        @NonNull        @Override        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {            int[] idList = idMatrix[pagePosition];            String[] strList = strMatrix[pagePosition];            softFirstPage.clear();            FitGridView gridView = null;            if (0 == pagePosition) {                gridView = softFirstPage.get();            }            if (gridView == null) {                gridView = (FitGridView) LayoutInflater.from(ctx).inflate(R.layout.emotion_gridview, container, false);                gridView.setSelector(R.drawable.chat_face_bg);                EmojiAdapter emojiAdapter = new EmojiAdapter(ctx, idList);                emojiAdapter.notifyDataSetChanged();                gridView.setFitGridAdapter(emojiAdapter);                if (0 == pagePosition) {                    softFirstPage = new SoftReference<>(gridView);                }            }            container.addView(gridView);            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {                if (listener != null) {                    int res = idList[itemPosition];                    String text = strList[itemPosition];                    SpannableString ss = new SpannableString(text);                    Drawable d = ctx.getResources().getDrawable(res);                    // 设置表情图片的显示大小                    d.setBounds(0, 0,                            DisplayUtil.dip2px(view.getContext(), EmojiInputFilter.EMOJI_DRAWABLE_BOUND_SIZE_DP),                            DisplayUtil.dip2px(view.getContext(), EmojiInputFilter.EMOJI_DRAWABLE_BOUND_SIZE_DP));                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);                    ss.setSpan(span, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);                    listener.onEmojiClick(ss);                }            });            return gridView;        }        @Override        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {            return view == object;        }    }    static class EmojiPager2Adapter extends PagerAdapter {        private int[][] idMatrix;        // 表情符号所代表的英文字符        private String[][] strMatrix;        private OnGifClickListener listener;        private Context ctx;        EmojiPager2Adapter(Context ctx, int[][] idMatrix, String[][] strMatrix, OnGifClickListener listener) {            this.ctx = ctx;            this.idMatrix = idMatrix;            this.strMatrix = strMatrix;            this.listener = listener;        }        @Override        public int getCount() {            return idMatrix.length;        }        @Override        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {            container.removeView((View) object);        }        @NonNull        @Override        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {            int[] idList = idMatrix[pagePosition];            String[] strList = strMatrix[pagePosition];            FitGridView gridView = (FitGridView) LayoutInflater.from(ctx).inflate(R.layout.chat_face_gridview, container, false);            container.addView(gridView);            gridView.setSelector(R.drawable.chat_face_bg);            gridView.setFitGridAdapter(new GifAdapter(ctx, idList));            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {                if (listener != null) {                    String text = strList[itemPosition];                    listener.onGifClick(text);                }            });            return gridView;        }        @Override        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {            return view == object;        }    }    class EmojiPager3Adapter extends PagerAdapter {        // collections_gridview里的列数乘以两行，        private final static int size = 10;        private List<Collectiion> collectionList;        private OnCollectionClickListener listener;        private Context ctx;        EmojiPager3Adapter(Context ctx, List<Collectiion> collectionList, OnCollectionClickListener listener) {            this.ctx = ctx;            this.collectionList = collectionList;            this.listener = listener;        }        @Override        public int getCount() {            // 编辑按钮已经加在list开头了，            // 0舍1入，除以每页10个，            return (collectionList.size() + (size - 1)) / size;        }        @Override        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {            container.removeView((View) object);        }        @NonNull        @Override        public Object instantiateItem(@NonNull ViewGroup container, int pagePosition) {            FitGridView gridView;            List<Collectiion> currentPageItemList = collectionList.subList(                    pagePosition * size,                    Math.min((pagePosition + 1) * size, collectionList.size())            );            if (collectionList.size() > 1) {                gridView = (FitGridView) LayoutInflater.from(ctx).inflate(R.layout.collections_gridview, container, false);                gridView.setFitGridAdapter(new CollectionAdapter(mContext, currentPageItemList));            } else {                gridView = (FitGridView) LayoutInflater.from(ctx).inflate(R.layout.collections_gridview_init, container, false);                gridView.setFitGridAdapter(new CollectionAdapterInit(mContext));            }            container.addView(gridView);            gridView.setSelector(R.drawable.chat_face_bg);            gridView.setOnItemClickListener((parent, view, itemPosition, id) -> {                if (listener != null) {                    listener.onCollectionClick(currentPageItemList.get(itemPosition));                }            });            return gridView;        }        @Override        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {            return view == object;        }    }}