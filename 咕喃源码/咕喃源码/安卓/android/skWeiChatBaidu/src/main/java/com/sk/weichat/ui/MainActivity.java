package com.sk.weichat.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.coloros.mcssdk.PushManager;
import com.example.qrcode.Constant;
import com.example.qrcode.ScannerActivity;
import com.fanjun.keeplive.KeepLive;
import com.fanjun.keeplive.config.KeepLiveService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.sk.weichat.AppConstant;
import com.sk.weichat.BuildConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.Contact;
import com.sk.weichat.bean.Contacts;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.UploadingFile;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.circle.FindItem;
import com.sk.weichat.bean.collection.Collectiion;
import com.sk.weichat.bean.event.EventCreateGroupFriend;
import com.sk.weichat.bean.event.EventQRCodeReady;
import com.sk.weichat.bean.event.EventSendVerifyMsg;
import com.sk.weichat.bean.event.MessageContactEvent;
import com.sk.weichat.bean.event.MessageEventBG;
import com.sk.weichat.bean.event.MessageEventHongdian;
import com.sk.weichat.bean.event.MessageLogin;
import com.sk.weichat.bean.event.MessageSendChat;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.broadcast.MucgroupUpdateUtil;
import com.sk.weichat.broadcast.OtherBroadcast;
import com.sk.weichat.broadcast.TimeChangeReceiver;
import com.sk.weichat.broadcast.UpdateUnReadReceiver;
import com.sk.weichat.broadcast.UserLogInOutReceiver;
import com.sk.weichat.call.AudioOrVideoController;
import com.sk.weichat.call.CallConstants;
import com.sk.weichat.call.Jitsi_connecting_second;
import com.sk.weichat.call.MessageEventCancelOrHangUp;
import com.sk.weichat.call.MessageEventInitiateMeeting;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.ContactDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.MyZanDao;
import com.sk.weichat.db.dao.NewFriendDao;
import com.sk.weichat.db.dao.OnCompleteListener2;
import com.sk.weichat.db.dao.UploadingFileDao;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.db.dao.login.MachineDao;
import com.sk.weichat.downloader.UpdateManger;
import com.sk.weichat.fragment.DiscoverFragment;
import com.sk.weichat.fragment.FriendFragment;
import com.sk.weichat.fragment.MeFragment;
import com.sk.weichat.fragment.MessageFragment;
import com.sk.weichat.fragment.Nav1Fragment;
import com.sk.weichat.fragment.SquareFragment;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.helper.LoginSecureHelper;
import com.sk.weichat.helper.PrivacySettingHelper;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.pay.PaymentReceiptMoneyActivity;
import com.sk.weichat.pay.ReceiptPayMoneyActivity;
import com.sk.weichat.socket.SocketException;
import com.sk.weichat.ui.backup.ReceiveChatHistoryActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.CoreManager;
import com.sk.weichat.ui.lock.DeviceLockActivity;
import com.sk.weichat.ui.lock.DeviceLockHelper;
import com.sk.weichat.ui.login.WebLoginActivity;
import com.sk.weichat.ui.message.MucChatActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.ui.other.QRcodeActivity;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.ContactsUtil;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.util.HttpUtil;
import com.sk.weichat.util.JsonUtils;
import com.sk.weichat.util.PermissionUtil;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.ScreenUtil;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UiUtils;
import com.sk.weichat.util.log.LogUtils;
import com.sk.weichat.view.PermissionExplainDialog;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.VerifyDialog;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.helloDemon.FirebaseMessageService;
import com.sk.weichat.xmpp.helloDemon.HuaweiClient;
import com.sk.weichat.xmpp.helloDemon.MeizuPushMsgReceiver;
import com.sk.weichat.xmpp.helloDemon.OppoPushMessageService;
import com.sk.weichat.xmpp.helloDemon.VivoPushMessageReceiver;
import com.sk.weichat.xmpp.listener.ChatMessageListener;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;

import static com.sk.weichat.bean.message.XmppMessage.TYPE_EXIT_VOICE;
import static com.sk.weichat.bean.message.XmppMessage.TYPE_VIDEO_OUT;

/**
 * ?????????
 */
public class MainActivity extends BaseActivity implements PermissionUtil.OnRequestPermissionsResultCallbacks {
    // ????????????
    public static final String APP_ID = BuildConfig.XIAOMI_APP_ID;
    public static final String APP_KEY = BuildConfig.XIAOMI_APP_KEY;
    // ???????????????initView??????
    // ?????????????????????????????????????????????????????????true
    public static boolean isInitView = false;
    /**
     * ??????????????????
     */
    Handler mHandler = new Handler();
    private UpdateUnReadReceiver mUpdateUnReadReceiver = null;
    private UserLogInOutReceiver mUserLogInOutReceiver = null;
    private TimeChangeReceiver timeChangeReceiver = null;
    private ActivityManager mActivityManager;
    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private int mLastFragmentId;// ????????????
    private RadioGroup mRadioGroup;
    private RadioButton mRbTab1, mRbTab2, mRbTab3, mRbTab4, mRbNav1Tab;
    private TextView mTvMessageNum;// ??????????????????????????????
    private TextView mTvNewFriendNum;// ?????????????????????????????????
    private TextView mTvCircleNum;// ???????????????????????????
    private int numMessage = 0;// ????????????????????????
    private int numCircle = 0; // ???????????????????????????
    private String mUserId;// ??????????????? UserID
    private My_BroadcastReceiver my_broadcastReceiver;
    private int mCurrtTabId;
    private boolean isCreate;
    /**
     * ????????????????????????????????????
     */
    private boolean isConflict;

    public MainActivity() {
        noLoginRequired();
    }

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        ctx.startActivity(intent);
    }

    /**
     * ????????????????????????
     * ??????MainActivity??????Fragment?????????
     */
    public static void requestQrCodeScan(Activity ctx) {
        int size = ScreenUtil.getScreenWidth(MyApplication.getContext()) / 16 * 9;
        // ??????????????????????????????bitmap
        QRcodeActivity.getSelfQrCodeBitmap(size,
                CoreManager.requireSelf(ctx).getUserId(),
                CoreManager.requireSelf(ctx).getNickName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ?????????????????? | ?????????????????? | Activity????????????????????? | ??????????????????
/*
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
*/
        setContentView(R.layout.activity_main);
        // ????????????
        if (PrivacySettingHelper.getPrivacySettings(this).getIsKeepalive() == 1) {
            initKeepLive();
        }
        initLog();

        mUserId = coreManager.getSelf().getUserId();
        initView();// ???????????????
        initBroadcast();// ???????????????
        initDatas();// ?????????????????????

        // ??????????????????Control
        AudioOrVideoController.init(mContext, coreManager);

        AsyncUtils.doAsync(this, mainActivityAsyncContext -> {
            // ??????app????????????????????????????????????????????????????????????????????????
            List<UploadingFile> uploadingFiles = UploadingFileDao.getInstance().getAllUploadingFiles(coreManager.getSelf().getUserId());
            for (int i = uploadingFiles.size() - 1; i >= 0; i--) {
                ChatMessageDao.getInstance().updateMessageState(coreManager.getSelf().getUserId(), uploadingFiles.get(i).getToUserId(),
                        uploadingFiles.get(i).getMsgId(), ChatMessageListener.MESSAGE_SEND_FAILED);
            }
        });

        UpdateManger.checkUpdate(this, coreManager.getConfig().androidAppUrl, coreManager.getConfig().androidVersion);

        EventBus.getDefault().post(new MessageLogin());
        // ????????????
        showDeviceLock();

        initMap();

        // ??????????????????????????????ios?????????
        setSwipeBackEnable(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "onNewIntent1");
        if (isInitView) {
            Log.e(TAG, "onNewIntent2");
            // ????????????????????????????????????????????????
            setStatusBarColor();
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> lf = fm.getFragments();
            for (Fragment f : lf) {
                fm.beginTransaction().remove(f).commitNowAllowingStateLoss();
            }
            initView();
        }
        MainActivity.isInitView = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // ??????????????????????????????????????????????????????
        MsgBroadcast.broadcastMsgUiUpdate(mContext);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!JCVideoPlayer.backPress()) {
                // ??????JCVideoPlayer.backPress()
                // true : ??????????????????????????????
                // false: ??????????????????????????????
                moveTaskToBack(true);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        // XMPP???????????? ????????????disconnect ?????????????????????????????????????????????????????? ??????????????????
        coreManager.disconnect();

        unregisterReceiver(mUpdateUnReadReceiver);
        unregisterReceiver(mUserLogInOutReceiver);
        unregisterReceiver(my_broadcastReceiver);
        unregisterReceiver(timeChangeReceiver);
        EventBus.getDefault().unregister(this);

        Glide.get(this).clearMemory();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(getApplicationContext()).clearDiskCache();
            }
        });
        super.onDestroy();
    }

    private void initKeepLive() {
        //??????????????????
        KeepLive.startWork(getApplication(), KeepLive.RunMode.ENERGY,
                //??????????????????????????????socket???????????????????????????????????????????????????????????????????????????
                new KeepLiveService() {
                    /**
                     * ?????????
                     * ?????????????????????????????????????????????????????????????????????
                     */
                    @Override
                    public void onWorking() {
                        Log.e("xuan", "onWorking: ");
                    }

                    /**
                     * ????????????
                     * ???????????????????????????????????????????????????????????????????????????onWorking?????????????????????????????????broadcast
                     */
                    @Override
                    public void onStop() {
                        Log.e("xuan", "onStop: ");
                    }
                }
        );
    }

    private void initLog() {
        String dir = FileUtil.getSaveDirectory("IMLogs");
        LogUtils.setLogDir(dir);
        LogUtils.setLogLevel(LogUtils.LogLevel.WARN);
    }

    private void initView() {
        getSupportActionBar().hide();
        mRadioGroup = (RadioGroup) findViewById(R.id.main_rg);
        mRbTab1 = (RadioButton) findViewById(R.id.rb_tab_1);
        mRbTab2 = (RadioButton) findViewById(R.id.rb_tab_2);
        mRbTab3 = (RadioButton) findViewById(R.id.rb_tab_3);
        mRbTab4 = (RadioButton) findViewById(R.id.rb_tab_4);
        mRbNav1Tab = (RadioButton) findViewById(R.id.rb_tab_nav1);

        mTvMessageNum = (TextView) findViewById(R.id.main_tab_one_tv);
        mTvNewFriendNum = (TextView) findViewById(R.id.main_tab_two_tv);
        Friend newFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE);
        if (newFriend != null) {
            updateNewFriendMsgNum(newFriend.getUnReadNum());
        }

        mTvCircleNum = (TextView) findViewById(R.id.main_tab_three_tv);

        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            hideInput();
            if (checkedId > 0 && mCurrtTabId != checkedId) {
                mCurrtTabId = checkedId;

                changeFragment(checkedId);

                if (checkedId == R.id.rb_tab_1) {
                    updateNumData();
                }
                JCVideoPlayer.releaseAllVideos();
            }
        });

        isCreate = false;
        //  ????????????bug
        mRbTab1.toggle();
        // initFragment();

        // ????????????
        ColorStateList tabColor = SkinUtils.getSkin(this).getMainTabColorState();
        for (RadioButton radioButton : Arrays.asList(mRbTab1, mRbTab2, mRbTab3, mRbTab4)) {
            // ???????????????????????????????????????
            Drawable drawable = radioButton.getCompoundDrawables()[1];
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(drawable, tabColor);
            // ?????????getDrawable?????????Drawable???????????????setCompoundDrawables??????????????????
            radioButton.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            radioButton.setTextColor(tabColor);
        }
        mRbNav1Tab.setTextColor(tabColor);
        // ?????????????????????????????????
        checkNotifyStatus();

    }

    private void getNavData() {
//        if(true){
//            List<FindItem> findItems = new ArrayList<>();
//            FindItem item = new FindItem();
//            item.setTitle("test");
//            item.setUrl("https://www.baidu.com");
//            findItems.add(item);
//            findItems.add(item);
//            Nav1Fragment.findItems = findItems;
//        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("page", 1 + "");
        params.put("limit", 10 + "");
        HttpUtils.get().url(coreManager.getConfig().FIND_MORE_ITEMS)
                .params(params)
                .build()
                .execute(new ListCallback<FindItem>(FindItem.class) {
                    @Override
                    public void onResponse(ArrayResult<FindItem> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null && result.getData().size() > 0) {
                            if ((coreManager.getConfig().researchSwitch)) {
                                mRbNav1Tab.setVisibility(View.VISIBLE);
                                Nav1Fragment.findItems = result.getData();
                            } else {
                                mRbNav1Tab.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void initBroadcast() {
        EventBus.getDefault().register(this);

        // ??????????????????????????????
        IntentFilter filter = new IntentFilter();
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE);
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE_NEW_FRIEND);
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_RESET);
        mUpdateUnReadReceiver = new UpdateUnReadReceiver(this);
        registerReceiver(mUpdateUnReadReceiver, filter);

        // ??????????????????????????????
        mUserLogInOutReceiver = new UserLogInOutReceiver(this);
        registerReceiver(mUserLogInOutReceiver, LoginHelper.getLogInOutActionFilter());

        // ???????????????????????? ?????????????????????????????????????????????????????????
        filter = new IntentFilter();
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????(?????????????????????????????????????????????????????????)???????????????
        filter.addAction(Constants.UPDATE_ROOM);
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.SYNC_CLEAN_CHAT_HISTORY);
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.SYNC_SELF_DATE);
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.CollectionRefresh);
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.SEND_MULTI_NOTIFY);  // ??????????????????
        my_broadcastReceiver = new My_BroadcastReceiver();
        registerReceiver(my_broadcastReceiver, filter);

        // ???????????????????????????
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        timeChangeReceiver = new TimeChangeReceiver(this);
        registerReceiver(timeChangeReceiver, filter);
    }

    private void initDatas() {
        // ???????????????????????????????????????????????????
        User loginUser = coreManager.getSelf();
        if (!LoginHelper.isUserValidation(loginUser)) {
            LoginHelper.prepareUser(this, coreManager);
        }
        LoginSecureHelper.autoLogin(this, coreManager, t -> {
            if (t instanceof LoginSecureHelper.LoginTokenOvertimeException) {
                MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_OVERDUE;
                loginOut();
            }
        }, () -> {
            // ??????????????????????????????????????????accessToken???????????????
            loginRequired();
            initCore();
            CoreManager.initLocalCollectionEmoji();
            CoreManager.updateMyBalance();
            initOther();// ??????????????????
            checkTime();
            // ?????????????????????
            if ((coreManager.getConfig().isSupportAddress
                    && !coreManager.getConfig().registerUsername)) {
                addressBookOperation();
            }
            login();
            updateSelfData();
        });

        mUserId = loginUser.getUserId();
        FriendDao.getInstance().checkSystemFriend(mUserId); // ?????? ???????????????

        //?????????????????????
        getNavData();

        // ???????????????????????????
        updateNumData();
    }

    private void showDeviceLock() {
        if (DeviceLockHelper.isLocked()) {
            // ?????????????????????
            DeviceLockActivity.start(this);
        } else {
            Log.e("DeviceLock", "???????????????????????????????????????");
        }
    }

    private void initMap() {
        // ?????????????????????????????????
        // ???????????????????????????????????????????????????
        String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
        if (TextUtils.equals(area, "CN")) {
            MapHelper.setMapType(MapHelper.MapType.BAIDU);
        } else {
            MapHelper.setMapType(MapHelper.MapType.GOOGLE);
        }
    }

    /**
     * ??????Fragment
     */
    private void changeFragment(int checkedId) {
        if (mLastFragmentId == checkedId) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(checkedId));
        if (fragment == null) {
            switch (checkedId) {
                case R.id.rb_tab_1:
                    fragment = new MessageFragment();
                    break;
                case R.id.rb_tab_2:
                    fragment = new FriendFragment();
                    break;
                case R.id.rb_tab_3:
//                    if (coreManager.getConfig().newUi) { // ??????????????????ui??????????????????????????????
//                        fragment = new SquareFragment();
//                    } else {
                    fragment = new DiscoverFragment();
//                    }
                    break;
                case R.id.rb_tab_4:
                    fragment = new MeFragment();
                    break;
                case R.id.rb_tab_nav1:
                    fragment = new Nav1Fragment();
                    break;
            }
        }

        // fragment = null;
        assert fragment != null;

        if (!fragment.isAdded()) {// ????????? add
            transaction.add(R.id.main_content, fragment, String.valueOf(checkedId));
        }

        Fragment lastFragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(mLastFragmentId));

        if (lastFragment != null) {
            transaction.hide(lastFragment);
        }
        // ??????????????????last???current???????????????fragment???????????????hide???show,
        transaction.show(fragment);

        // transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);// ????????????
        transaction.commitNowAllowingStateLoss();

        // getSupportFragmentManager().executePendingTransactions();

        mLastFragmentId = checkedId;

        if (checkedId == R.id.rb_tab_nav1) {
//            setStatusBarLight(false);
        } else {
            setStatusBarColor();
        }
    }

    /**
     * OPPO?????????App????????????????????????????????????????????????????????????
     * OPPO?????????App??????????????????StartActivity??????????????????????????????????????? ????????????-????????????-??????????????? ??????App??????????????????
     * <p>
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private void checkNotifyStatus() {
        int launchCount = PreferenceUtils.getInt(this, Constants.APP_LAUNCH_COUNT, 0);// ??????app???????????????
        Log.e("zq", "??????app?????????:" + launchCount);
        if (launchCount == 1) {
            String tip = "";
            if (!AppUtils.isNotificationEnabled(this)) {
                tip = getString(R.string.title_notification) + "\n" + getString(R.string.content_notification);
            }
            if (DeviceInfoUtil.isOppoRom()) {// ??????Rom???OPPO???????????????????????????????????????
                tip += getString(R.string.open_auto_launcher);
            }
            if (!TextUtils.isEmpty(tip)) {
                SelectionFrame dialog = new SelectionFrame(this);
                dialog.setSomething(null, tip, new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        PermissionUtil.startApplicationDetailsSettings(MainActivity.this, 0x001);
                    }
                });
                dialog.show();
            }
        } else if (launchCount == 2) {
            if (DeviceInfoUtil.isMiuiRom() || DeviceInfoUtil.isMeizuRom()) {
                SelectionFrame dialog = new SelectionFrame(this);
                dialog.setSomething(getString(R.string.open_screen_lock_show),
                        getString(R.string.open_screen_lock_show_for_audio), new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {

                            }

                            @Override
                            public void confirmClick() {
                                PermissionUtil.startApplicationDetailsSettings(MainActivity.this, 0x001);
                            }
                        });
                dialog.show();
            }
        }
    }

    private void initOther() {
        Log.d(TAG, "initOther() called");

        // ????????????????????????????????????????????????ID?????????????????????????????????
        // ?????????????????????????????????????????????????????????????????????

        //noinspection ConstantConditions
        AsyncUtils.doAsync(this, t -> {
            Reporter.post("?????????????????????", t);
        }, mainActivityAsyncContext -> {
            if (coreManager.getConfig().enableGoogleFcm && googleAvailable()) {
                if (HttpUtil.testGoogle()) {// ?????????????????????????????? ??????????????????
                    FirebaseMessageService.init(MainActivity.this);
                } else {// ????????????????????????????????????????????????????????????????????????????????????????????????
                    selectPush();
                }
            } else {
                selectPush();
            }
        });
    }

    private boolean googleAvailable() {
        boolean isGoogleAvailability = true;
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            // ????????????????????????????????????
            // ????????????????????????????????????
            // if (googleApiAvailability.isUserResolvableError(resultCode)) {
            //     googleApiAvailability.getErrorDialog(this, resultCode, 2404).show();
            // }
            // ?????????????????????????????????
            isGoogleAvailability = false;
        }
        return isGoogleAvailability;
    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    private void selectPush() {
        // ??????Rom????????????
        if (DeviceInfoUtil.isEmuiRom()) {
            Log.e(TAG, "???????????????: ???????????????");
            // ???????????? ????????????
            HuaweiClient client = new HuaweiClient(this);
            client.clientConnect();
        } else if (DeviceInfoUtil.isMeizuRom()) {
            Log.e(TAG, "???????????????: ???????????????");
            MeizuPushMsgReceiver.init(this);
        } else if (PushManager.isSupportPush(this)) {
            Log.e(TAG, "???????????????: OPPO?????????");
            OppoPushMessageService.init(this);
        } else if (DeviceInfoUtil.isVivoRom()) {
            Log.e(TAG, "???????????????: VIVO?????????");
            VivoPushMessageReceiver.init(this);
        } else if (true || DeviceInfoUtil.isMiuiRom()) {
            Log.e(TAG, "???????????????: ???????????????");
            if (shouldInit()) {
                // ?????????????????????
                MiPushClient.registerPush(this, APP_ID, APP_KEY);
            }
        }
    }

    public void checkTime() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        long requestTime = System.currentTimeMillis();
        HttpUtils.get().url(coreManager.getConfig().GET_CURRENT_TIME)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        // ?????????config????????????????????????????????????????????????????????????
                        // ???ios???????????????????????????????????????
                        long responseTime = System.currentTimeMillis();
                        TimeUtils.responseTime(requestTime, result.getCurrentTime(), result.getCurrentTime(), responseTime);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ??????????????????
                        Log.e("TimeUtils", "??????????????????", e);
                    }
                });
    }

    public void cancelUserCheckIfExist() {
        Log.d(TAG, "cancelUserCheckIfExist() called");
    }

    /* ?????????????????????????????????????????????????????????Fragment???????????????????????????????????????????????????????????? */
    public void removeNeedUserFragment() {
        mRadioGroup.clearCheck();
        mLastFragmentId = -1;
        isCreate = true;
    }

    /**
     * ????????????
     */
    public void login() {
        Log.d(TAG, "login() called");
        User user = coreManager.getSelf();

        Intent startIntent = CoreService.getIntent(MainActivity.this, user.getUserId(), user.getPassword(), user.getNickName());
        ContextCompat.startForegroundService(MainActivity.this, startIntent);

        mUserId = user.getUserId();
        numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mUserId);
        numCircle = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        updateNumData();
        if (isCreate) {
            mRbTab1.toggle();
        }
    }

    public void loginOut() {
        Log.d(TAG, "loginOut() called");
        coreManager.logout();
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        if (MyApplication.getInstance().mUserStatus == LoginHelper.STATUS_USER_TOKEN_OVERDUE) {
            UserCheckedActivity.start(MyApplication.getContext());
        }
        finish();
    }

    public void conflict() {
        Log.d(TAG, "conflict() called");
        isConflict = true;// ????????????

        coreManager.logout();
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
        UserCheckedActivity.start(this);
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        }
        mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        finish();
    }

    public void need_update() {
        Log.d(TAG, "need_update() called");
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        // ???????????????
        UserCheckedActivity.start(this);
    }

    public void login_give_up() {
        Log.d(TAG, "login_give_up() called");
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_NO_UPDATE;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageSendChat message) {
        if (!message.isGroup) {
            coreManager.sendChatMessage(message.toUserId, message.chat);
        } else {
            coreManager.sendMucChatMessage(message.toUserId, message.chat);
        }
    }

    // ?????????????????????????????????
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventHongdian message) {
        if (message.number == -1) {
            // ?????????????????????
            int size = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
            if (size == 0) {
                // ??????????????????????????????
                UiUtils.updateNum(mTvCircleNum, -1);
            }
            return;
        }
        numCircle = message.number;
        UiUtils.updateNum(mTvCircleNum, numCircle);
    }

    // ??????????????????????????????IM,????????????????????????
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageContactEvent mMessageEvent) {
        List<Contact> mNewContactList = ContactDao.getInstance().getContactsByToUserId(coreManager.getSelf().getUserId(),
                mMessageEvent.message);
        if (mNewContactList != null && mNewContactList.size() > 0) {
            updateContactUI(mNewContactList);
        }
    }

    /**
     * ????????????????????????????????????XMPP???????????????
     * copy by AudioOrVideoController
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventCancelOrHangUp event) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        ChatMessage message = new ChatMessage();
        if (event.type == 103) {          // ?????? ????????????
            message.setType(XmppMessage.TYPE_NO_CONNECT_VOICE);
        } else if (event.type == 104) {// ?????? ????????????
            message.setType(XmppMessage.TYPE_END_CONNECT_VOICE);
        } else if (event.type == 113) {// ?????? ????????????
            message.setType(XmppMessage.TYPE_NO_CONNECT_VIDEO);
        } else if (event.type == 114) {// ?????? ????????????
            message.setType(XmppMessage.TYPE_END_CONNECT_VIDEO);
        } else if (event.type == TYPE_VIDEO_OUT) {// ??????????????????
            message.setType(TYPE_VIDEO_OUT);
            message.setGroup(true);
        } else if (event.type == TYPE_EXIT_VOICE) {// ?????????????????????
            message.setType(TYPE_EXIT_VOICE);
            message.setGroup(true);
        }
        message.setMySend(true);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(coreManager.getSelf().getNickName());
        message.setToUserId(event.toUserId);
        message.setContent(event.content);
        message.setTimeLen(event.callTimeLen);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));

        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, event.toUserId, message)) {
            ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, message.getFromUserId(), message, false);
        }

        coreManager.sendChatMessage(event.toUserId, message);
        MsgBroadcast.broadcastMsgUiUpdate(mContext);   // ??????????????????
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventInitiateMeeting message) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginNickName = coreManager.getSelf().getNickName();

        Jitsi_connecting_second.start(this, mLoginUserId, mLoginUserId, message.type);

        for (int i = 0; i < message.list.size(); i++) {
            ChatMessage mMeetingMessage = new ChatMessage();
            int type;
            String str;
            if (message.type == CallConstants.Audio_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VOICE;
                str = getString(R.string.tip_invite_voice_meeting);
            } else if (message.type == CallConstants.Video_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VIDEO;
                str = getString(R.string.tip_invite_video_meeting);
            } else {
                type = XmppMessage.TYPE_IS_MU_CONNECT_TALK;
                str = getString(R.string.tip_invite_talk_meeting);
            }
            mMeetingMessage.setType(type);
            mMeetingMessage.setContent(str);
            mMeetingMessage.setFromUserId(mLoginUserId);
            mMeetingMessage.setFromUserName(mLoginNickName);
            mMeetingMessage.setObjectId(mLoginUserId);
            mMeetingMessage.setTimeSend(TimeUtils.sk_time_current_time());
            mMeetingMessage.setToUserId(message.list.get(i));
            mMeetingMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            coreManager.sendChatMessage(message.list.get(i), mMeetingMessage);
            // ??????????????????????????????

//            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.list.get(i), mMeetingMessage);
//            FriendDao.getInstance().updateFriendContent(mLoginUserId, message.list.get(i), str, type, TimeUtils.sk_time_current_time());

        }
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param eventQRCodeReady
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventQRCodeReady eventQRCodeReady) {
        // todo ????????????requestQrCodeScan????????????ctx????????????getActivity??????(???MainActivity)??????ctx????????????activity??????????????????????????????this?????????????????????????????????
        int size = ScreenUtil.getScreenWidth(MyApplication.getContext()) / 16 * 9;
        Intent intent = new Intent(this, ScannerActivity.class);
        // ?????????????????????
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_WIDTH, size);
        // ?????????????????????
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_HEIGHT, size);
        // ?????????????????????????????????
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_TOP_PADDING, DisplayUtil.dip2px(this, 100));
        // ?????????????????????
        intent.putExtra(Constant.EXTRA_IS_ENABLE_SCAN_FROM_PIC, true);
        if (eventQRCodeReady.getBitmap() != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            eventQRCodeReady.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            intent.putExtra(Constant.EXTRA_SELF_QR_CODE_BITMAP, bytes);
        }
        startActivityForResult(intent, 888);
    }

    /**
     * ??????????????? || ??????????????? ??????????????????????????????????????? ???????????????????????????
     *
     * @param eventSendVerifyMsg
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventSendVerifyMsg eventSendVerifyMsg) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginUserName = coreManager.getSelf().getNickName();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GROUP_VERIFY);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginUserName);
        message.setToUserId(eventSendVerifyMsg.getCreateUserId());
        String s = JsonUtils.initJsonContent(mLoginUserId, mLoginUserName, eventSendVerifyMsg.getGroupJid(), "1", eventSendVerifyMsg.getReason());
        message.setObjectId(s);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        if (coreManager.isLogin()) {
            coreManager.sendChatMessage(eventSendVerifyMsg.getCreateUserId(), message);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventBG mMessageEventBG) {
        if (mMessageEventBG.flag) {// ???????????????
            // ????????????
            showDeviceLock();
            // ?????????????????????
            NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.cancelAll();
            }

            if (isConflict) {// ????????????????????????????????????
                isConflict = false;// Reset Status
                Log.e("zq", "????????????????????????????????????");
                return;
            }

            if (!coreManager.isServiceReady()) {
                // ?????????????????????????????????CoreService????????????????????????????????????ta
                Log.e("zq", "CoreService?????????????????????");
                coreManager.relogin();
            } else {
                if (!coreManager.isLogin()) {// XMPP?????????
                    Log.e("zq", "XMPP????????????????????????");
                    coreManager.autoReconnect(MainActivity.this);
                }
            }
        } else {
            if (mMessageEventBG.isCloseError) {
                // XMPP???????????? || ????????????
                MachineDao.getInstance().resetMachineStatus();
            }
            AsyncUtils.doAsync(this, c -> coreManager.appBackstage(getApplicationContext(), coreManager.isLogin(), mMessageEventBG.isCloseError));
        }
    }

    /*
    ??????????????? || ??????????????? ???????????? ????????????????????????
    */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventCreateGroupFriend eventCreateGroupFriend) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginUserName = coreManager.getSelf().getNickName();
        MucRoom room = eventCreateGroupFriend.getMucRoom();

        MyApplication.getInstance().saveGroupPartStatus(room.getJid(), room.getShowRead(), room.getAllowSendCard(),
                room.getAllowConference(), room.getAllowSpeakCourse(), room.getTalkTime());

        Friend friend = new Friend();
        friend.setOwnerId(mLoginUserId);
        friend.setUserId(room.getJid());
        friend.setNickName(room.getName());
        friend.setDescription(room.getDesc());
        friend.setRoomId(room.getId());
        friend.setRoomCreateUserId(room.getUserId());
        friend.setChatRecordTimeOut(room.getChatRecordTimeOut());// ?????????????????? -1/0 ??????
        friend.setContent(mLoginUserName + " " + getString(R.string.Message_Object_Group_Chat));
        friend.setTimeSend(TimeUtils.sk_time_current_time());
        friend.setRoomFlag(1);
        friend.setStatus(Friend.STATUS_FRIEND);
        FriendDao.getInstance().createOrUpdateFriend(friend);

        // ??????socket?????????????????????
        coreManager.joinMucChat(room.getJid(), 0);
    }

    private boolean shouldInit() {
        ActivityManager activityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        String mainProcessName = getPackageName();
        int myPid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ???????????????
     */
    public void hideInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder token = getWindow().getDecorView().getWindowToken();
        if (imm != null && imm.isActive() && token != null) {
            imm.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * ???????????????????????????
     */
    private void addressBookOperation() {
        boolean isReadContacts = PermissionUtil.checkSelfPermissions(this, new String[]{Manifest.permission.READ_CONTACTS});
        if (isReadContacts) {
            try {
                uploadAddressBook();
            } catch (Exception e) {
                String message = getString(R.string.tip_read_contacts_failed);
                ToastUtil.showToast(this, message);
                Reporter.post(message, e);
                ContactsUtil.cleanLocalCache(this, coreManager.getSelf().getUserId());
            }
        } else {
            String[] permissions = new String[]{Manifest.permission.READ_CONTACTS};
            if (!PermissionUtil.deniedRequestPermissionsAgain(this, permissions)) {
                PermissionExplainDialog tip = new PermissionExplainDialog(this);
                tip.setPermissions(permissions);
                tip.setOnConfirmListener(() -> {
                    PermissionUtil.requestPermissions(this, 0x01, permissions);
                });
                tip.show();
            } else {
                PermissionUtil.requestPermissions(this, 0x01, permissions);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms, boolean isAllGranted) {
        if (isAllGranted) {// ?????????
            try {
                uploadAddressBook();
            } catch (Exception e) {
                String message = getString(R.string.tip_read_contacts_failed);
                ToastUtil.showToast(this, message);
                Reporter.post(message, e);
                ContactsUtil.cleanLocalCache(this, coreManager.getSelf().getUserId());
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms, boolean isAllDenied) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 888:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.getExtras() == null) {
                        return;
                    }
                    String result = data.getExtras().getString(Constant.EXTRA_RESULT_CONTENT);
                    Log.e("zq", "????????????????????????" + result);
                    if (TextUtils.isEmpty(result)) {
                        return;
                    }
                    if (PaymentReceiptMoneyActivity.checkQrCode(result)) {
                        // ?????????19??? && ????????? ???????????????????????? ??????????????????
                        Intent intent = new Intent(mContext, PaymentReceiptMoneyActivity.class);
                        intent.putExtra("PAYMENT_ORDER", result);
                        startActivity(intent);
                    } else if (result.contains("userId")
                            && result.contains("userName")) {
                        // ???????????????????????? ??????????????????
                        Intent intent = new Intent(mContext, ReceiptPayMoneyActivity.class);
                        intent.putExtra("RECEIPT_ORDER", result);
                        startActivity(intent);
                    } else if (ReceiveChatHistoryActivity.checkQrCode(result)) {
                        // ?????????????????????????????????????????????????????????????????????????????????
                        ReceiveChatHistoryActivity.start(this, result);
                    } else if (WebLoginActivity.checkQrCode(result)) {
                        // ????????????????????????????????????????????????????????????
                        WebLoginActivity.start(this, result);
                    } else {
                        if (result.contains("shikuId")) {
                            // ?????????
                            Map<String, String> map = WebViewActivity.URLRequest(result);
                            String action = map.get("action");
                            String userId = map.get("shikuId");
                            if (TextUtils.equals(action, "group")) {
                                getRoomInfo(userId);
                            } else if (TextUtils.equals(action, "user")) {
                                getUserInfo(userId);
                            } else {
                                Reporter.post("????????????????????????<" + result + ">");
                                ToastUtil.showToast(this, R.string.unrecognized);
                            }
                        } else if (!result.contains("shikuId")
                                && HttpUtil.isURL(result)) {
                            // ????????????  ???????????????
                            Intent intent = new Intent(this, WebViewActivity.class);
                            intent.putExtra(WebViewActivity.EXTRA_URL, result);
                            startActivity(intent);
                        } else {
                            Reporter.post("????????????????????????<" + result + ">");
                            ToastUtil.showToast(this, R.string.unrecognized);
                        }
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * ?????????????????????userId
     */
    private void getUserInfo(String account) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken);
        params.put("account", account);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_GET_URL_ACCOUNT)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            User user = result.getData();
                            BasicInfoActivity.start(mContext, user.getUserId(), BasicInfoActivity.FROM_ADD_TYPE_QRCODE);
                        } else {
                            ToastUtil.showErrorData(MyApplication.getInstance());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MyApplication.getInstance());
                    }
                });
    }

    /**
     * ??????????????????
     */
    private void getRoomInfo(String roomId) {
        Friend friend = FriendDao.getInstance().getMucFriendByRoomId(coreManager.getSelf().getUserId(), roomId);
        if (friend != null) {
            if (friend.getGroupStatus() == 0) {
                interMucChat(friend.getUserId(), friend.getNickName());
                return;
            } else {// ????????????????????? || ?????????????????? || ????????????????????????
                FriendDao.getInstance().deleteFriend(coreManager.getSelf().getUserId(), friend.getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), friend.getUserId());
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            final MucRoom mucRoom = result.getData();
                            if (mucRoom.getIsNeedVerify() == 1) {
                                VerifyDialog verifyDialog = new VerifyDialog(MainActivity.this);
                                verifyDialog.setVerifyClickListener(MyApplication.getInstance().getString(R.string.tip_reason_invite_friends), new VerifyDialog.VerifyClickListener() {
                                    @Override
                                    public void cancel() {

                                    }

                                    @Override
                                    public void send(String str) {
                                        EventBus.getDefault().post(new EventSendVerifyMsg(mucRoom.getUserId(), mucRoom.getJid(), str));
                                    }
                                });
                                verifyDialog.show();
                                return;
                            }
                            joinRoom(mucRoom, coreManager.getSelf().getUserId());
                        } else {
                            ToastUtil.showErrorData(MainActivity.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MainActivity.this);
                    }
                });
    }

    /**
     * ????????????
     */
    private void joinRoom(final MucRoom room, final String loginUserId) {
        DialogHelper.showDefaulteMessageProgressDialog(MainActivity.this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", room.getId());
        if (room.getUserId().equals(loginUserId))
            params.put("type", "1");
        else
            params.put("type", "2");

        MyApplication.mRoomKeyLastCreate = room.getJid();

        HttpUtils.get().url(coreManager.getConfig().ROOM_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(MainActivity.this, result)) {
                            EventBus.getDefault().post(new EventCreateGroupFriend(room));
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {// ???500ms?????????????????????????????????????????????????????????????????????
                                    interMucChat(room.getJid(), room.getName());
                                }
                            }, 500);
                        } else {
                            MyApplication.mRoomKeyLastCreate = "compatible";
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MainActivity.this);
                        MyApplication.mRoomKeyLastCreate = "compatible";
                    }
                });
    }

    /**
     * ????????????
     */
    private void interMucChat(String roomJid, String roomName) {
        Intent intent = new Intent(MainActivity.this, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);

        MucgroupUpdateUtil.broadcastUpdateUi(MainActivity.this);
    }

    private void uploadAddressBook() {
        List<Contacts> mNewAdditionContacts = ContactsUtil.getNewAdditionContacts(this, coreManager.getSelf().getUserId());
        /**
         * ????????????
         * [{"name":"15768779999","telephone":"8615768779999"},{"name":"?????????","telephone":"8615720966659"},
         * {"name":"zas","telephone":"8613000000000"},{"name":"????????????","telephone":"864007883333"},]
         * ???????????????
         * [{\"toTelephone\":\"15217009762\",\"toRemarkName\":\"????????????????????????\"},{\"toTelephone\":\"15217009762\",\"toRemarkName\":\"????????????????????????\"}]
         */
        if (mNewAdditionContacts.size() <= 0) {
            return;
        }

        String step1 = JSON.toJSONString(mNewAdditionContacts);
        String step2 = step1.replaceAll("name", "toRemarkName");
        String contactsListStr = step2.replaceAll("telephone", "toTelephone");
        Log.e("contact", "????????????????????????" + contactsListStr);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("uploadJsonStr", contactsListStr);

        HttpUtils.post().url(coreManager.getConfig().ADDRESSBOOK_UPLOAD)
                .params(params)
                .build()
                .execute(new ListCallback<Contact>(Contact.class) {

                    @Override
                    public void onResponse(ArrayResult<Contact> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<Contact> mContactList = result.getData();
                            for (int i = 0; i < mContactList.size(); i++) {
                                Contact contact = mContactList.get(i);
                                if (ContactDao.getInstance().createContact(contact)) {
                                    if (contact.getStatus() == 1) {// ???????????????????????????????????????????????????
                                        NewFriendDao.getInstance().addFriendOperating(contact.getToUserId(), contact.getToUserName(), contact.getToRemarkName());
                                    }
                                }
                            }

                            if (mContactList.size() > 0) {// ????????????????????????  ????????????contacts id
                                updateContactUI(mContactList);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private void updateRoom() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", "0");
        params.put("pageIndex", "0");
        params.put("pageSize", "1000");// ????????????????????????

        HttpUtils.get().url(coreManager.getConfig().ROOM_LIST_HIS)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoom> result) {
                        if (result.getResultCode() == 1) {
                            FriendDao.getInstance().addRooms(mHandler, coreManager.getSelf().getUserId(), result.getData(), new OnCompleteListener2() {
                                @Override
                                public void onLoading(int progressRate, int sum) {

                                }

                                @Override
                                public void onCompleted() {
/*
                                    if (coreManager.isLogin()) {
                                        coreManager.batchMucChat();
                                    }
*/
                                    MsgBroadcast.broadcastMsgUiUpdate(MainActivity.this);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /*
    ?????? ??????
     */
    public void msg_num_update(int operation, int count) {
        numMessage = (operation == MsgBroadcast.NUM_ADD) ? numMessage + count : numMessage - count;
        updateNumData();
    }

    public void msg_num_reset() {
        numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mUserId);
        numCircle = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        updateNumData();
    }

    public void updateNumData() {
        numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mUserId);
        numCircle = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());

        ShortcutBadger.applyCount(this, numMessage);

        UiUtils.updateNum(mTvMessageNum, numMessage);
        UiUtils.updateNum(mTvCircleNum, numCircle);
    }

    /*
    ?????????
     */
    public void updateNewFriendMsgNum(int msgNum) {
        int mNewContactsNumber = PreferenceUtils.getInt(this, Constants.NEW_CONTACTS_NUMBER + coreManager.getSelf().getUserId(),
                0);
        int totalNumber = msgNum + mNewContactsNumber;

        if (totalNumber == 0) {
            mTvNewFriendNum.setText("");
            mTvNewFriendNum.setVisibility(View.INVISIBLE);
        } else {
            mTvNewFriendNum.setText(totalNumber + "");
            mTvNewFriendNum.setVisibility(View.VISIBLE);
        }
    }

    private void updateContactUI(List<Contact> mContactList) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        int mContactsNumber = PreferenceUtils.getInt(MainActivity.this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, 0);
        int mTotalContactsNumber = mContactsNumber + mContactList.size();
        PreferenceUtils.putInt(MainActivity.this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, mTotalContactsNumber);
        Friend newFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE);
        updateNewFriendMsgNum(newFriend.getUnReadNum());

        List<String> mNewContactsIds = new ArrayList<>();
        for (int i = 0; i < mContactList.size(); i++) {
            mNewContactsIds.add(mContactList.get(i).getToUserId());
        }
        String mContactsIds = PreferenceUtils.getString(MainActivity.this, Constants.NEW_CONTACTS_IDS + mLoginUserId);
        List<String> ids = JSON.parseArray(mContactsIds, String.class);
        if (ids != null && ids.size() > 0) {
            mNewContactsIds.addAll(ids);
        }
        PreferenceUtils.putString(MainActivity.this, Constants.NEW_CONTACTS_IDS + mLoginUserId, JSON.toJSONString(mNewContactsIds));
    }

    // ???????????????????????????????????????????????????
    private void emptyServerMessage(String friendId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(0));// 0 ???????????? 1 ????????????
        params.put("toUserId", friendId);

        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private void updateSelfData() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            User user = result.getData();
                            boolean updateSuccess = UserDao.getInstance().updateByUser(user);
                            // ????????????????????????
                            if (updateSuccess) {
                                // ?????????????????????User?????????
                                coreManager.setSelf(user);
                                // ??????MeFragment??????
                                sendBroadcast(new Intent(OtherBroadcast.SYNC_SELF_DATE_NOTIFY));
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    public void notifyCollectionList() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", coreManager.getSelf().getUserId());

        HttpUtils.get().url(coreManager.getConfig().Collection_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Collectiion>(Collectiion.class) {
                    @Override
                    public void onResponse(ArrayResult<Collectiion> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            MyApplication.mCollection = result.getData();
                            Collectiion collection = new Collectiion();
                            collection.setType(7);
                            MyApplication.mCollection.add(0, collection);
                            // ????????????????????????
                            sendBroadcast(new Intent(OtherBroadcast.CollectionRefresh_ChatFace));
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MyApplication.getContext());
                    }
                });
    }

    private class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }

            if (action.equals(Constants.UPDATE_ROOM)) {
                updateRoom();
            } else if (action.equals(SocketException.FINISH_CONNECT_EXCEPTION)) {
                coreManager.autoReconnect(MainActivity.this);
            } else if (action.equals(OtherBroadcast.SYNC_CLEAN_CHAT_HISTORY)) {
                String friendId = intent.getStringExtra(AppConstant.EXTRA_USER_ID);
                emptyServerMessage(friendId);

                FriendDao.getInstance().resetFriendMessage(coreManager.getSelf().getUserId(), friendId);
                ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), friendId);
                sendBroadcast(new Intent(Constants.CHAT_HISTORY_EMPTY));// ??????????????????
                MsgBroadcast.broadcastMsgUiUpdate(mContext);
            } else if (action.equals(OtherBroadcast.SYNC_SELF_DATE)) {
                updateSelfData();
            } else if (action.equals(OtherBroadcast.CollectionRefresh)) {
                notifyCollectionList();
            } else if (action.equals(OtherBroadcast.SEND_MULTI_NOTIFY)) {
                mRbTab4.setChecked(false);
                mRbTab1.setChecked(true);
            }
        }
    }
}