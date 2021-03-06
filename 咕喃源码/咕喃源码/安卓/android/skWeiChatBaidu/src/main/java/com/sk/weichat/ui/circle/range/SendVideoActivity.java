package com.sk.weichat.ui.circle.range;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.UploadFileResult;
import com.sk.weichat.bean.VideoFile;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.helper.UploadService;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.util.SendTextFilter;
import com.sk.weichat.ui.map.MapPickerActivity;
import com.sk.weichat.ui.me.LocalVideoActivity;
import com.sk.weichat.ui.tool.ButtonColorChange;
import com.sk.weichat.util.BitmapUtil;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.RecorderUtils;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.UploadCacheUtils;
import com.sk.weichat.util.VideoCompressUtil;
import com.sk.weichat.view.SelectionFrame;
import com.sk.weichat.view.TipDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Jni.VideoUitls;
import VideoHandle.OnEditorListener;
import okhttp3.Call;

/**
 * ????????????
 */
public class SendVideoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // ??????
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // ????????????
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // ????????????
    private static final int REQUEST_CODE_SELECT_COVER = 6;  // ????????????
    private static boolean isBoolBan = false;
    private View tvSelectCover;
    private EditText mTextEdit;
    // ????????????
    private TextView mTVLocation;
    // ????????????
    private TextView mTVSee;
    // ????????????
    private TextView mTVAt;
    // Video Item
    private FrameLayout mFloatLayout;
    private ImageView mImageView;
    private ImageView mIconImageView;
    private TextView mVideoTextTv;
    // data
    private int mSelectedId;
    private String mVideoFilePath;
    private String mThumbPath;
    private Bitmap mThumbBmp;
    private long mTimeLen;
    private SelectionFrame mSelectionFrame;
    // ???????????? || ???????????? ?????? ?????????????????????????????????
    private String str1;
    private String str2;
    private String str3;
    // ???????????????
    private int visible = 1;
    // ???????????? || ????????????
    private String lookPeople;
    // ????????????
    private String atlookPeople;
    // ??????????????????
    private double latitude;
    private double longitude;
    private String address;
    private String mVideoData;
    private String mImageData;
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_video);
        initActionBar();
        initView();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageView.setImageBitmap(null);
        mThumbBmp = null;
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isExitNoPublish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.send_video));
    }

    private void initView() {
        tvSelectCover = findViewById(R.id.tvSelectCover);
        checkBox = findViewById(R.id.cb_ban);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isBoolBan = isChecked;
                checkBox.setChecked(isBoolBan);
                if (isBoolBan) {
                    ButtonColorChange.checkChange(SendVideoActivity.this, checkBox);
                } else {
                    checkBox.setButtonDrawable(getResources().getDrawable(R.mipmap.prohibit_icon));
                }
            }
        });
        RelativeLayout rl_ban = findViewById(R.id.rl_ban);
        rl_ban.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBoolBan = !isBoolBan;
                Log.e("zx", "onClick: rl_ban  " + isBoolBan);
                checkBox.setChecked(isBoolBan);
                if (isBoolBan) {
                    ButtonColorChange.checkChange(SendVideoActivity.this, checkBox);
                } else {
                    checkBox.setButtonDrawable(getResources().getDrawable(R.mipmap.prohibit_icon));
                }
            }
        });
        TextView tv_title_right = (TextView) findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.circle_release));
        tv_title_right.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tv_title_right, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mVideoFilePath) || mTimeLen <= 0) {
                    Toast.makeText(SendVideoActivity.this, getString(R.string.add_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                compress(new File(mVideoFilePath));
            }
        });
        mTextEdit = (EditText) findViewById(R.id.text_edit);
        // ??????EditText???ScrollView???????????????
        mTextEdit.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        // ?????????EditText?????????????????????600????????????????????????
        mTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mTextEdit.getText().toString().trim().length() >= 10000) {
                    Toast.makeText(mContext, getString(R.string.tip_edit_max_input_length, 10000), Toast.LENGTH_SHORT).show();
                }
            }
        });
        mTextEdit.setHint(getString(R.string.add_msg_mind));
        // ????????????
        mTVLocation = (TextView) findViewById(R.id.tv_location);
        // ????????????
        mTVSee = (TextView) findViewById(R.id.tv_see);
        // ????????????
        mTVAt = (TextView) findViewById(R.id.tv_at);

        mFloatLayout = findViewById(R.id.float_layout);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mIconImageView = (ImageView) findViewById(R.id.icon_image_view);
        mIconImageView.setBackgroundResource(R.drawable.send_video);
        mVideoTextTv = (TextView) findViewById(R.id.text_tv);
        mVideoTextTv.setText(R.string.circle_add_video);
    }

    private void sendVideo(File file) {
        new UploadTask().execute(file.getPath());
    }

    private void compress(File file) {
        String path = file.getPath();
        DialogHelper.showMessageProgressDialog(this, MyApplication.getContext().getString(R.string.compressed));
        final String out = RecorderUtils.getVideoFileByTime();
        String[] cmds = RecorderUtils.ffmpegComprerssCmd(path, out);
        long duration = VideoUitls.getDuration(path);

        VideoCompressUtil.exec(cmds, duration, new OnEditorListener() {
            public void onSuccess() {
                DialogHelper.dismissProgressDialog();
                File outFile = new File(out);
                runOnUiThread(() -> {
                    if (outFile.exists()) {
                        sendVideo(outFile);
                    } else {
                        sendVideo(file);
                    }
                });
            }

            public void onFailure() {
                DialogHelper.dismissProgressDialog();
                runOnUiThread(() -> {
                    sendVideo(file);
                });
            }

            public void onProgress(float progress) {

            }
        });
    }

    // ????????????????????????
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    private void initEvent() {
        tvSelectCover.setOnClickListener(v -> {
            SelectCoverActivity.start(this, REQUEST_CODE_SELECT_COVER, mVideoFilePath);
        });
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);

        mFloatLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SendVideoActivity.this, LocalVideoActivity.class);
                intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
                // ????????????????????????????????????
                intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, false);
                if (mSelectedId != 0) {
                    intent.putExtra(AppConstant.EXTRA_SELECT_ID, mSelectedId);
                }
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_location:
                // ????????????
                Intent intent1 = new Intent(this, MapPickerActivity.class);
                startActivityForResult(intent1, REQUEST_CODE_SELECT_LOCATE);
                break;
            case R.id.rl_see:
                // ????????????
                Intent intent2 = new Intent(this, SeeCircleActivity.class);
                intent2.putExtra("THIS_CIRCLE_TYPE", visible - 1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER1", str1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER2", str2);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER3", str3);
                startActivityForResult(intent2, REQUEST_CODE_SELECT_TYPE);
                break;
            case R.id.rl_at:
                // ????????????
                if (visible == 2) {
                    ToastUtil.showToast(SendVideoActivity.this, R.string.tip_private_cannot_use_this);
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    intent3.putExtra("REMIND_SELECT_PERSON", atlookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        isExitNoPublish();
    }

    private void isExitNoPublish() {
        if (!TextUtils.isEmpty(mVideoFilePath)) {
            mSelectionFrame = new SelectionFrame(SendVideoActivity.this);
            mSelectionFrame.setSomething(getString(R.string.app_name), getString(R.string.tip_has_video_no_public), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    finish();
                }
            });
            mSelectionFrame.show();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // ?????????????????????
            String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
            List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
            if (fileList == null || fileList.size() == 0) {
                // ???????????????????????????????????????
                Reporter.unreachable();
                return;
            }
            VideoFile videoFile = fileList.get(0);

            String filePath = videoFile.getFilePath();
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(getString(R.string.add_msg_add_video));
                mIconImageView.setBackgroundResource(R.drawable.send_video);
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(getString(R.string.add_msg_add_video));
                mIconImageView.setBackgroundResource(R.drawable.send_video);
                return;
            }
            // ?????????????????????????????????
            mVideoTextTv.setText("");
            mIconImageView.setBackground(null);

            tvSelectCover.setVisibility(View.VISIBLE);
            mVideoFilePath = filePath;
            mThumbBmp = AvatarHelper.getInstance().displayVideoThumb(filePath, mImageView);
            mTimeLen = videoFile.getFileLength();
            mSelectedId = videoFile.get_id();
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
            // ??????????????????
            latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                Log.e("zq", "??????:" + latitude + "   ?????????" + longitude + "   ?????????" + address);
                mTVLocation.setText(address);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.loc_startlocnotice));
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TYPE) {
            // ??????????????????
            int mOldVisible = visible;
            visible = data.getIntExtra("THIS_CIRCLE_TYPE", 1);
            // ?????????????????????????????????????????????????????????????????????
            if (mOldVisible != visible
                    || visible == 3 || visible == 4) {
                // ???????????????????????? 3/4 ???????????????????????????????????????????????????????????????
                atlookPeople = "";
                mTVAt.setText("");
            }
            if (visible == 1) {
                mTVSee.setText(R.string.publics);
            } else if (visible == 2) {
                mTVSee.setText(R.string.privates);
                if (!TextUtils.isEmpty(atlookPeople)) {
                    final TipDialog tipDialog = new TipDialog(this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_notify), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                            tipDialog.dismiss();
                        }
                    });
                    tipDialog.show();
                }
            } else if (visible == 3) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String looKenName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(looKenName);
            } else if (visible == 4) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String lookName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(getString(R.string.not_allow, lookName));
            }
            str1 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER1");
            str2 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER2");
            str3 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER3");
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_REMIND) {
            // ??????????????????
            atlookPeople = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON");
            String atLookPeopleName = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON_NAME");
            mTVAt.setText(atLookPeopleName);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_COVER) {
            mThumbPath = SelectCoverActivity.parseResult(data);
            AvatarHelper.getInstance().displayUrl(mThumbPath, mImageView);
        }
    }

    public void sendAudio() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // ???????????????1=???????????????2=???????????????3=???????????????4=???????????????
        params.put("type", "4");
        // ???????????????1??????????????????2??????????????????3??????????????????
        params.put("flag", "3");

        // ?????????????????????1=?????????2=?????????3=???????????????????????????4=????????????
        params.put("visible", String.valueOf(visible));
        if (visible == 3) {
            // ????????????
            params.put("userLook", lookPeople);
        } else if (visible == 4) {
            // ????????????
            params.put("userNotLook", lookPeople);
        }
        // ????????????
        if (!TextUtils.isEmpty(atlookPeople)) {
            params.put("userRemindLook", atlookPeople);
        }

        // ????????????
        params.put("text", SendTextFilter.filter(mTextEdit.getText().toString()));
        params.put("videos", mVideoData);
        if (!TextUtils.isEmpty(mImageData) && !mImageData.equals("{}") && !mImageData.equals("[{}]")) {
            params.put("images", mImageData);
        }

        /**
         * ????????????
         */
        if (!TextUtils.isEmpty(address)) {
            // ??????
            params.put("latitude", String.valueOf(latitude));
            // ??????
            params.put("longitude", String.valueOf(longitude));
            // ??????
            params.put("location", address);
        }

        params.put("isAllowComment", isBoolBan ? String.valueOf(1) : String.valueOf(0));

        // ?????????????????????????????????????????????????????????????????????????????????
        Area area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));// ??????Id
        } else {
            params.put("cityId", "0");
        }

        /**
         * ????????????
         */
        // ????????????
        params.put("model", DeviceInfoUtil.getModel());
        // ???????????????????????????
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        if (!TextUtils.isEmpty(DeviceInfoUtil.getDeviceId(mContext))) {
            // ???????????????
            params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));
        }

        DialogHelper.showDefaulteMessageProgressDialog(SendVideoActivity.this);

        HttpUtils.post().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(mContext, result)) {
                            Intent intent = new Intent();
                            intent.putExtra(AppConstant.EXTRA_MSG_ID, result.getData());
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(SendVideoActivity.this);
                    }
                });
    }

    private class UploadTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(SendVideoActivity.this);
        }

        /**
         * ?????????????????? <br/>
         * return 1 Token???????????????????????? <br/>
         * return 2 ?????????????????????????????? <br/>
         * return 3 ????????????<br/>
         * return 4 ????????????<br/>
         */
        @Override
        protected Integer doInBackground(String... params) {
            String mVideoFilePath = params[0];
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(mVideoFilePath)) {
                return 2;
            }

            // ????????????????????????sd???
            String imageSavePsth;
            if (TextUtils.isEmpty(mThumbPath)) {
                imageSavePsth = CameraUtil.getOutputMediaFileUri(SendVideoActivity.this, CameraUtil.MEDIA_TYPE_IMAGE).getPath();
                if (!BitmapUtil.saveBitmapToSDCard(mThumbBmp, imageSavePsth)) {// ?????????????????????
                    return 3;
                }
            } else {
                imageSavePsth = mThumbPath;
            }

            Map<String, String> mapParams = new HashMap<String, String>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// ???????????????

            List<String> dataList = new ArrayList<String>();
            dataList.add(mVideoFilePath);
            if (!TextUtils.isEmpty(imageSavePsth)) {
                dataList.add(imageSavePsth);
            }
            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, mapParams, dataList);
            if (TextUtils.isEmpty(result)) {
                return 3;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(SendVideoActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {// ???????????????????????????
                    return 3;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getVideos() != null && data.getVideos().size() > 0) {
                        while (data.getVideos().size() > 1) {// ???????????????????????????????????????????????????????????????????????????
                            data.getVideos().remove(data.getVideos().size() - 1);
                        }
                        data.getVideos().get(0).setSize(new File(mVideoFilePath).length());
                        data.getVideos().get(0).setLength(mTimeLen);
                        // ??????????????????????????????????????????
                        UploadCacheUtils.save(SendVideoActivity.this, data.getVideos().get(0).getOriginalUrl(), mVideoFilePath);
                        mVideoData = JSON.toJSONString(data.getVideos(), UploadFileResult.sAudioVideosFilter);
                    } else {
                        return 3;
                    }
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    }
                    return 4;
                } else {// ??????????????????????????????
                    return 3;
                }
            } else {
                return 3;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                DialogHelper.dismissProgressDialog();
                startActivity(new Intent(SendVideoActivity.this, LoginHistoryActivity.class));
            } else if (result == 2) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendVideoActivity.this, getString(R.string.alert_not_have_file));
            } else if (result == 3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendVideoActivity.this, R.string.upload_failed);
            } else {
                sendAudio();
            }
        }
    }
}
