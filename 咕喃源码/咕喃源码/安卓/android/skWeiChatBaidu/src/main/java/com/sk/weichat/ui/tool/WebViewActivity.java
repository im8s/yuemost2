package com.sk.weichat.ui.tool;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.OrderInfo;
import com.sk.weichat.bean.WebCallback;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.ShareSdkHelper;
import com.sk.weichat.ui.account.AuthorDialog;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.JsonUtils;
import com.sk.weichat.util.PermissionUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ComplaintDialog;
import com.sk.weichat.view.ExternalOpenDialog;
import com.sk.weichat.view.MatchKeyWordEditDialog;
import com.sk.weichat.view.ModifyFontSizeDialog;
import com.sk.weichat.view.PayDialog;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.WebMoreDialog;
import com.sk.weichat.view.window.WindowShowService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.HttpUrl;

/**
 * web
 */
public class WebViewActivity extends BaseActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_DOWNLOAD_URL = "download_url";
    private static final String TAG = "WebViewActivity";
    public static String FLOATING_WINDOW_URL;
    public static boolean IS_FLOATING;
    boolean isReported;
    private TextView mTitleTv;
    private ImageView mTitleLeftIv;
    private ImageView mTitleRightIv;
    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;
    private String mUrl; // ??????URL
    private String mDownloadUrl;// ShareSdk ???????????????????????????????????????(????????????????????????????????????????????????)
    private JsSdkInterface jsSdkInterface;
    // js sdk????????????????????????
    private String shareBeanContent;
    // ????????? ???????????????
    private String mShareParams;

    public static void start(Context ctx, String url) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String url, String shareParams) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra("shareParams", shareParams);
        ctx.startActivity(intent);
    }

    /**
     * ?????????url?????????????????????
     * ??? "index.jsp?Action=del&id=123"????????????Action:del,id:123??????map???
     *
     * @param URL url??????
     * @return url??????????????????
     */
    public static Map<String, String> URLRequest(String URL) {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit = null;

        String strUrlParam = TruncateUrlPage(URL);
        if (strUrlParam == null) {
            return mapRequest;
        }
        //????????????????????? www.2cto.com
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");

            //???????????????
            if (arrSplitEqual.length > 1) {
                //????????????
                try {
                    arrSplitEqual[1] = URLDecoder.decode(arrSplitEqual[1], "UTF-8");
                } catch (Exception ignored) {
                }
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            } else {
                if (arrSplitEqual[0] != "") {
                    //?????????????????????????????????
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        Log.d(TAG, "URLRequest: " + mapRequest.get("webAppName"));
        return mapRequest;
    }

    /**
     * ??????url???????????????????????????????????????
     *
     * @param strURL url??????
     * @return url??????????????????
     */
    private static String TruncateUrlPage(String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        strURL = strURL.trim();

        arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }

        return strAllParam;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        if (getIntent() != null) {
            mUrl = getIntent().getStringExtra(EXTRA_URL);
            mDownloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);
            mShareParams = getIntent().getStringExtra("shareParams");
            initCheck();
            initActionBar();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (IS_FLOATING) {
            getCurrentUrl();
            startService(new Intent(WebViewActivity.this, WindowShowService.class));
        }
    }

    @Override
    protected void onDestroy() {
        if (jsSdkInterface != null) {
            jsSdkInterface.release();
        }
        super.onDestroy();
    }

    private void initCheck() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("webUrl", mUrl);

        HttpUtils.get().url(coreManager.getConfig().URL_CHECK)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() != 1) {// ??????????????????????????????????????????????????????????????????
                            isReported = true;
                        }
                        init();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        init();
                    }
                });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
    }

    private void init() {
        initView();
        initClient();
        initEvent();

        if (isReported) {// ??????????????????
            mWebView.loadUrl("file:////android_asset/prohibit.html");
            mTitleRightIv.setVisibility(View.GONE);
        } else {
            int openStatus = openApp(mUrl);
            if (openStatus == 1) {// ??????????????????????????????????????????????????????return
                finish();
            } else if (openStatus == 2) {// ????????????????????????????????????????????????????????????????????????????????????
                load(mWebView, mDownloadUrl);
            } else if (openStatus == 5) {// ????????????????????????????????????????????????

            } else {// 0 | 3 | 4
                load(mWebView, mUrl);
            }
        }

        stopService(new Intent(WebViewActivity.this, WindowShowService.class));
    }

    private void initView() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mWebView = (WebView) findViewById(R.id.mWebView);
        /* ????????????Js */
        mWebView.getSettings().setJavaScriptEnabled(true);
        /* ?????????true??????????????????js?????????????????? */
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        /* ?????????????????? */
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDomStorageEnabled(true);

        /* ???????????????webview??????????????? */
        mWebView.getSettings().setUseWideViewPort(true);
        /* ?????????????????????????????? */
        mWebView.getSettings().setLoadWithOverviewMode(true);
        /* ??????????????????webview?????????????????????,???????????????false,????????? */
        mWebView.getSettings().setBuiltInZoomControls(false);
        /* ?????????????????????????????? */
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        /* HTML5?????????????????????,?????????true,?????????????????? */
        mWebView.getSettings().setGeolocationEnabled(true);
        /* ???????????????????????? */
        mWebView.getSettings().setAllowFileAccess(true);

        // ??????UserAgent??????
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " app-shikuimapp");
    }

    private void initClient() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setAlpha(1.0f);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.e(TAG, "shouldOverrideUrlLoading: " + url);
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                int openStatus = openApp(url);
                if (openStatus == 1) {// ??????????????????????????????????????????????????????return
                    return true;
                } else if (openStatus == 2) {// ????????????????????????????????????????????????????????????????????????????????????
                    load(view, mDownloadUrl);
                } else if (openStatus == 5) {// ??????????????????????????? ????????????????????????????????????????????????

                } else { // 0 | 3 | 4
                    load(view, url);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        // ????????????????????????
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // ????????????????????????
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // ??????????????????????????????????????????
                    startDismissAnimation(mProgressBar.getProgress());
                } else {
                    // ??????????????????????????????????????????
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mTitleTv.setText(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                // ????????????????????????????????????
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {
                // ???????????????????????????????????????????????????
                ToastUtil.showToast(WebViewActivity.this, R.string.download_error);
            }
        });

        jsSdkInterface = new JsSdkInterface(this, new MyJsSdkListener());
        jsSdkInterface.setShareParams(mShareParams);
        mWebView.addJavascriptInterface(jsSdkInterface, "AndroidWebView");
    }

    private void initEvent() {
        mTitleRightIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WebMoreDialog mWebMoreDialog = new WebMoreDialog(WebViewActivity.this, getCurrentUrl(), new WebMoreDialog.BrowserActionClickListener() {
                    @Override
                    public void floatingWindow() {
                        showFloating();
                    }

                    @Override
                    public void sendToFriend() {
                        forwardToFriend();
                    }

                    @Override
                    public void shareToLifeCircle() {
                        shareMoment();
                    }

                    @Override
                    public void collection() {
                        onCollection(getCurrentUrl());
                    }

                    @Override
                    public void searchContent() {
                        search();
                    }

                    @Override
                    public void copyLink() {
                        ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setText(getCurrentUrl());
                        Toast.makeText(WebViewActivity.this, getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void openOutSide() {
                        ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(mContext, getCurrentUrl());
                        externalOpenDialog.show();
                    }

                    @Override
                    public void modifyFontSize() {
                        setWebFontSiz();
                    }

                    @Override
                    public void refresh() {
                        mWebView.reload();
                    }

                    @Override
                    public void complaint() {
                        report();
                    }

                    @Override
                    public void shareWechat() {
                        String title = mTitleTv.getText().toString().trim();
                        String url = getCurrentUrl();
                        ShareSdkHelper.shareWechat(
                                WebViewActivity.this, title, url, url
                        );
                    }

                    @Override
                    public void shareWechatMoments() {
                        String title = mTitleTv.getText().toString().trim();
                        String url = getCurrentUrl();
                        ShareSdkHelper.shareWechatMoments(
                                WebViewActivity.this, title, url, url
                        );
                    }
                });
                mWebMoreDialog.show();
            }
        });
    }

    /**
     * ??????url???????????????app
     */
    private int openApp(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        try {
            // ????????????
            //  http://192.168.0.141:8080/websiteAuthorh/appAuth.html?appId=sk7c4fd05f92c7460a&callbackUrl=http://192.168.0.141:8080/websiteAuthorh/test.html
            if (url.contains("websiteAuthorh/index.html")) {
                String webAppName = URLRequest(url).get("webAppName");
                String webAppsmallImg = URLRequest(url).get("webAppsmallImg");
                String appId = URLRequest(url).get("appId");
                String redirectURL = URLRequest(url).get("callbackUrl");

                Log.e(TAG, "openApp: " + webAppName + "," + webAppsmallImg + "," + url);
                AuthorDialog dialog = new AuthorDialog(mContext);
                dialog.setDialogData(webAppName, webAppsmallImg);
                dialog.setmConfirmOnClickListener(new AuthorDialog.ConfirmOnClickListener() {
                    @Override
                    public void confirm() {
                        HttpUtils.get().url(coreManager.getConfig().AUTHOR_CHECK)
                                .params("appId", appId)
                                .params("state", coreManager.getSelfStatus().accessToken)
                                .params("callbackUrl", redirectURL)
                                .build().execute(new BaseCallback<WebCallback>(WebCallback.class) {

                            @Override
                            public void onResponse(ObjectResult<WebCallback> result) {
                                if (Result.checkSuccess(mContext, result) && result.getData() != null) {
                                    String html = HttpUrl.parse(result.getData().getCallbackUrl()).newBuilder()
                                            .addQueryParameter("code", result.getData().getCode())
                                            .build()
                                            .toString();
                                    load(mWebView, html);
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {

                            }
                        });
                    }

                    @Override
                    public void AuthorCancel() {

                    }
                });

                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                return 5;
            }

            if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("ftp")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                // host ??? scheme ????????????null
                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(scheme)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (AppUtils.isSupportIntent(this, intent)) {
                        startActivity(intent);
                        return 1;
                    } else {
                        return 2;
                    }
                }
            }
        } catch (Exception e) {
            return 3;
        }
        return 4;
    }

    private void load(WebView view, String url) {
        view.loadUrl(url);
    }

    /****************************************************
     * Start
     ***************************************************/
    private String getCurrentUrl() {
        if (mWebView == null) {
            // ????????????????????????????????????http????????????????????????????????????????????????????????????????????????
            return "";
        }
        Log.e(TAG, mWebView.getUrl());
        String currentUrl = mWebView.getUrl();
        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = mUrl;
        }

        FLOATING_WINDOW_URL = currentUrl;

        if (currentUrl.contains("https://view.officeapps.live.com/op/view.aspx?src=")) {
            currentUrl = currentUrl.replace("https://view.officeapps.live.com/op/view.aspx?src=", "");
        }

        return currentUrl;
    }

    /**
     * ??????
     */
    private void showFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || AppUtils.checkAlertWindowsPermission(this)) {
            IS_FLOATING = !IS_FLOATING;
            if (IS_FLOATING) {
                startService(new Intent(WebViewActivity.this, WindowShowService.class));
                finish();
            } else {
                stopService(new Intent(WebViewActivity.this, WindowShowService.class));
            }
        } else {
            SelectionFrame dialog = new SelectionFrame(this);
            dialog.setSomething(null, getString(R.string.av_no_float),
                    new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {

                        }

                        @Override
                        public void confirmClick() {
                            PermissionUtil.startApplicationDetailsSettings(WebViewActivity.this, 0x01);
                        }
                    });
            dialog.show();
        }
    }

    /**
     * ???????????????
     */
    private void initChatByUrl(String url) {
        String title = mTitleTv.getText().toString().trim();
        String content = JsonUtils.initJsonContent(title, getCurrentUrl(), url);
        initChatByContent(content, XmppMessage.TYPE_LINK);
    }

    private void initChatByContent(String content, int type) {
        String mLoginUserId = coreManager.getSelf().getUserId();

        ChatMessage message = new ChatMessage();
        message.setType(type);
        if (type == XmppMessage.TYPE_LINK) {
            message.setContent(content);
        } else if (type == XmppMessage.TYPE_SHARE_LINK) {
            message.setObjectId(content);
        } else {
            throw new IllegalStateException("????????????: " + type);
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, AppConstant.NORMAL_INSTANT_ID, message)) {
            Intent intent = new Intent(WebViewActivity.this, InstantMessageActivity.class);
            intent.putExtra("fromUserId", AppConstant.NORMAL_INSTANT_ID);
            intent.putExtra("messageId", message.getPacketId());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(mContext, R.string.tip_message_wrap_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void forwardToFriend() {
        if (shareBeanContent != null) {
            initChatByContent(shareBeanContent, XmppMessage.TYPE_SHARE_LINK);
        } else {
            selectShareImage();
        }
    }

    private void selectShareImage() {
        String str = mWebView.getUrl();
        if (TextUtils.isEmpty(str)) {
            str = getCurrentUrl();
        }
        HtmlFactory.instance().queryImage(str, new HtmlFactory.DataListener<String>() {// ??????????????????????????????

            @Override
            public void onResponse(List<String> data, String title) {
                if (data != null && data.size() > 0) {
                    String url = "";
                    for (int i = 0; i < data.size(); i++) {
                        if (!TextUtils.isEmpty(data.get(i))) {
                            url = data.get(i);
                            break;
                        }
                    }
                    if (!TextUtils.isEmpty(url)) { // ??????????????????
                        if (url.contains("http") && url.contains("com")) {
                            // ??????????????????????????????
                        } else if (url.contains("com")) {
                            // ex???m.baidu.com/se/static/img/iphone/logo_web.png
                            url = "https:" + url;
                        } else {
                            // ex???img/logo.png
                            if (!TextUtils.isEmpty(mWebView.getOriginalUrl())) {
                                String prefix = mWebView.getOriginalUrl().substring(0, mWebView.getOriginalUrl().lastIndexOf("/"));
                                url = prefix + "/" + url;
                            }
                        }
                    }
                    initChatByUrl(url);
                } else {
                    initChatByUrl("");
                }
            }

            @Override
            public void onError(String error) {
                initChatByUrl("");
            }
        });
    }

    /**
     * ??????????????????
     */
    private void shareMoment() {
        Intent intent = new Intent(WebViewActivity.this, SendShuoshuoActivity.class);
        intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT, getCurrentUrl());
        startActivity(intent);
    }

    /**
     * ??????
     * ?????? ?????? ???????????? ??????
     */
    private String collectionParam(String content) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_TEXT;
        json.put("type", String.valueOf(type));
        String msg = "";
        String collectContent = "";
        msg = content;
        collectContent = content;
        json.put("msg", msg);
        json.put("collectContent", collectContent);
        json.put("collectType", -1);// ????????????????????????
        array.add(json);
        return JSON.toJSONString(array);
    }

    private void onCollection(final String content) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emoji", collectionParam(content));

        HttpUtils.get().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * ??????????????????
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(this, mWebView);
        Window window = matchKeyWordEditDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// ???????????????
            matchKeyWordEditDialog.show();
        }
    }

    /**
     * ????????????
     */
    private void setWebFontSiz() {
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(this, mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * ??????
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(this, report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(WebViewActivity.this);

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(WebViewActivity.this, R.string.report_success);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        });
        complaintDialog.show();
    }

    /****************************************************
     * End
     ***************************************************/

    /**
     * progressBar????????????
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /**
     * progressBar????????????
     */
    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1500);  // ????????????
        anim.setInterpolator(new DecelerateInterpolator());
        // ??????, ???????????????????????????
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();      // 0.0f ~ 1.0f
                int offset = 100 - progress;
                mProgressBar.setProgress((int) (progress + offset * fraction));
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // ????????????
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.GONE);
                isAnimStart = false;
            }
        });
        anim.start();
    }

    private class MyJsSdkListener implements JsSdkInterface.Listener {

        @Override
        public void onFinishPlay(String path) {
            mWebView.evaluateJavascript("playFinish()", value -> {
            });
        }

        @Override
        public void onUpdateShareData(String shareBeanContent) {
            WebViewActivity.this.shareBeanContent = shareBeanContent;
        }

        @Override
        public void onChooseSKPayInApp(String appId, String prepayId, String sign) {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("appId", appId);
            params.put("prepayId", prepayId);
            params.put("sign", sign);

            // ??????????????????
            HttpUtils.get().url(coreManager.getConfig().PAY_GET_ORDER_INFO)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                        @Override
                        public void onResponse(ObjectResult<OrderInfo> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1 && result.getData() != null) {
                                PayDialog payDialog = new PayDialog(mContext, appId, prepayId, sign, result.getData(), new PayDialog.PayResultListener() {
                                    @Override
                                    public void payResult(String result) {
                                        mWebView.loadUrl("javascript:sk.paySuccess(" + result + ")");
                                    }
                                });
                                payDialog.show();
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        }

    }
}
