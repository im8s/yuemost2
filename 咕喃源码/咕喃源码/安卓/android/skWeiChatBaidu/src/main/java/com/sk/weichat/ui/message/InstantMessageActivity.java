package com.sk.weichat.ui.message;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.Reporter;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.RoomMember;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.bean.message.MucRoomMember;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.RoomMemberDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.TrillStatisticsHelper;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.sortlist.SortHelper;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.ButtonColorChange;
import com.sk.weichat.util.AsyncUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.HeadView;
import com.sk.weichat.view.HorizontalListView;
import com.sk.weichat.view.MessageAvatar;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * ?????? ???????????????
 */
public class InstantMessageActivity extends BaseActivity implements OnClickListener {
    private TextView mCreateChat;
    private ListView mLvRecentlyMessage;
    private MessageRecentlyAdapter messageRecentlyAdapter;
    private List<Friend> friends;
    private List<BaseSortModel<Friend>> mSortFriends;

    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private List<String> mSelectPositions;
    private Button mOkBtn;
    private InstantMessageConfirmNew menuWindow;

    private boolean isMoreSelected; // ?????????????????????
    private boolean isSingleOrMerge; // ????????????????????????
    // ???ChatActivity || MucChatActivity????????????toUserId???messageId???????????????????????? ???????????????
    private String toUserId;
    private String messageId;

    private String mLoginUserId;
    private RoomMember mRoomMember;
    // ?????????????????????????????????
    private int sendSize;
    private int currentSendSerialNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messageinstant);
        isMoreSelected = getIntent().getBooleanExtra(Constants.IS_MORE_SELECTED_INSTANT, false);
        isSingleOrMerge = getIntent().getBooleanExtra(Constants.IS_SINGLE_OR_MERGE, false);
        // ???ChatContentView????????????????????????????????????
        toUserId = getIntent().getStringExtra("fromUserId");
        messageId = getIntent().getStringExtra("messageId");

        mSortFriends = new ArrayList<>();
        mSelectPositions = new ArrayList<>();

        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        loadData();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.most_recent_contact));
    }

    private void loadData() {
        friends = FriendDao.getInstance().getNearlyFriendMsg(coreManager.getSelf().getUserId());
        for (int i = 0; i < friends.size(); i++) {
            if (friends.get(i).getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                    || friends.get(i).getUserId().equals(Friend.ID_SK_PAY)) {
                friends.remove(i);
            }
        }
        loadDataFriend(friends);
    }

    private void initView() {
        mCreateChat = findViewById(R.id.tv_create_newmessage);
        mCreateChat.setOnClickListener(this);

        mLvRecentlyMessage = findViewById(R.id.lv_recently_message);
        messageRecentlyAdapter = new MessageRecentlyAdapter();
        mLvRecentlyMessage.setAdapter(messageRecentlyAdapter);
        mLvRecentlyMessage.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                Friend friend;
                friend = mSortFriends.get(position).bean;
                for (int i = 0; i < mSortFriends.size(); i++) {
                    if (mSortFriends.get(i).getBean().getUserId().equals(friend.getUserId())) {
                        if (friend.getStatus() != 100) {
                            friend.setStatus(100);
                            mSortFriends.get(i).getBean().setStatus(100);
                            addSelect(friend.getUserId());
                        } else {
                            friend.setStatus(101);
                            mSortFriends.get(i).getBean().setStatus(101);
                            removeSelect(friend.getUserId());
                        }
                    }
                    messageRecentlyAdapter.setData(mSortFriends);
                }
            }
        });

        mHorAdapter = new HorListViewAdapter();
        mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
        mHorizontalListView.setAdapter(mHorAdapter);
        mHorAdapter.notifyDataSetChanged();
        mOkBtn = (Button) findViewById(R.id.ok_btn);
        ButtonColorChange.colorChange(mContext, mOkBtn);
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));

        mHorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                for (int i = 0; i < mSortFriends.size(); i++) {
                    if (mSortFriends.get(i).getBean().getUserId().equals(mSelectPositions.get(position))) {
                        mSortFriends.get(i).getBean().setStatus(101);
                        messageRecentlyAdapter.setData(mSortFriends);
                    }
                }
                mSelectPositions.remove(position);
                mHorAdapter.notifyDataSetInvalidated();
                mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
            }
        });

        mOkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopuWindow(v, mSelectPositions);
            }
        });
    }

    private void addSelect(String userId) {
        mSelectPositions.add(userId);
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void removeSelect(String userId) {
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i).equals(userId)) {
                mSelectPositions.remove(i);
            }
        }
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void showPopuWindow(View view, List<String> stringsId) {
        if (menuWindow != null) {
            menuWindow.dismiss();
        }
        List<Friend> friendList = new ArrayList<>();
        for (int i = 0; i < stringsId.size(); i++) {
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), stringsId.get(i));
            friendList.add(friend);
        }
        menuWindow = new InstantMessageConfirmNew(InstantMessageActivity.this, new ClickListener(friendList), friendList);
        menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.tv_create_newmessage:
                Intent intent = new Intent(this, SelectNewContactsActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, isMoreSelected);
                intent.putExtra(Constants.IS_SINGLE_OR_MERGE, isSingleOrMerge);
                intent.putExtra("fromUserId", toUserId);
                intent.putExtra("messageId", messageId);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }

    private void forwardingStep(Friend friend) {
        if (isMoreSelected) {// ???????????? ??????????????????(???????????????????????? || ???????????????????????????????????????????????????????????????)
            EventBus.getDefault().post(new EventMoreSelected(friend.getUserId(), isSingleOrMerge, friend.getRoomFlag() != 0));
        } else {// ????????????
            if (friend.getRoomFlag() == 0) {// ??????
                ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, messageId);
                TrillStatisticsHelper.share(this, coreManager, chatMessage);
                chatMessage.setFromUserId(mLoginUserId);
                chatMessage.setFromUserName(coreManager.getSelf().getNickName());
                chatMessage.setToUserId(friend.getUserId());
                chatMessage.setUpload(true);
                chatMessage.setMySend(true);
                chatMessage.setSendRead(false);
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage);
                coreManager.sendChatMessage(friend.getUserId(), chatMessage);
            } else {  // ??????
                instantChatMessage(friend, toUserId, messageId);
            }
        }
        finishThis();
    }

    /**
     * ???????????????????????????????????????????????????forwardingStep???????????????
     */
    private void faultTolerance() {
        currentSendSerialNumber++;
        finishThis();
    }

    /**
     * ???????????????????????????????????????
     */
    public void finishThis() {
        if (currentSendSerialNumber == sendSize) {
            if (isMoreSelected) {// ?????????????????????event?????????event???????????????????????????????????????
                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, true));
            } else {
                MsgBroadcast.broadcastMsgUiUpdate(mContext);
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }

    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.autoReconnect(this);
        }
        // Todo ???????????????????????????return???????????????...??????????????????(?????????)
        return false;
    }

    private void send(String UserId, ChatMessage message) {
        // ???????????????????????????????????????xmpp???????????????
        // ??????????????????????????????
        if (isAuthenticated()) {
            return;
        }
        coreManager.sendMucChatMessage(UserId, message);
    }

    public boolean isOk() {// ??????????????????????????????
        boolean isOk = true;
        if (mRoomMember != null) {
            if (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2) {
                isOk = true;
            } else {
                isOk = false;
            }
        }
        return isOk;
    }

    private void instantChatMessage(Friend mFriend, String toUserId, String messageId) {
        if (!TextUtils.isEmpty(messageId)) {
            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, messageId);
            boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mFriend.getUserId(), true);
            if (mFriend.getGroupStatus() == 0) {// ????????????
                List<RoomMember> roomMemberList = RoomMemberDao.getInstance().getRoomMember(mFriend.getRoomId());
                if (roomMemberList.size() > 0) {
                    mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
                }
            }

            if (chatMessage.getType() == ChatMessage.TYPE_FILE && !isAllowSendFile && !isOk()) {
                Toast.makeText(this, getString(R.string.tip_cannot_upload), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mRoomMember != null && MucRoomMember.disallowPublicAction(mRoomMember.getRole())) {
                ToastUtil.showToast(mContext, getString(R.string.tip_action_disallow_place_holder, getString(MucRoomMember.getRoleName(mRoomMember.getRole()))));
                return;
            }
            TrillStatisticsHelper.share(this, coreManager, chatMessage);
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(coreManager.getSelf().getNickName());
            chatMessage.setToUserId(mFriend.getUserId());
            chatMessage.setUpload(true);
            chatMessage.setMySend(true);
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
            send(mFriend.getUserId(), chatMessage);
        }
    }

    private void loadDataFriend(List<Friend> friends) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("?????????????????????", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSortFriends = sortedList;
                messageRecentlyAdapter.setData(sortedList);
            });
        });
    }

    /**
     * ?????????????????????????????????(?????????????????????????????????)???????????????
     */
    private void isSupportSend(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", friend.getRoomId());

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// ???????????????room/get??????????????????????????????????????????????????????????????????
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     if (mucRoom.getMember() == null) {// ??????????????????
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// ????????????????????????
                                         DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_kick));
                                         faultTolerance();
                                     } else {// ????????????
                                         if (mucRoom.getS() == -1) {// ?????????????????????
                                             FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 3);// ????????????????????????
                                             DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_group_disable_by_service));
                                             faultTolerance();
                                             return;
                                         }
                                         int role = mucRoom.getMember().getRole();
                                         // ??????????????????
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getTalkTime());

                                         // ?????????????????????
                                         MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(),
                                                 mucRoom.getAllowSendCard(), mucRoom.getAllowConference(),
                                                 mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime());

                                         // ??????????????????
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, role);

                                         if (role == 4) {
                                             DialogHelper.tip(mContext, getString(R.string.hint_invisible));
                                             faultTolerance();
                                             return;
                                         }
                                         if (role == 1 || role == 2) {// ?????????????????? ??????????????????
                                             currentSendSerialNumber++;
                                             forwardingStep(friend);
                                         } else {
                                             if (mucRoom.getTalkTime() > 0) {// ????????????
                                                 DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_now_ban_all));
                                                 faultTolerance();
                                             } else if (mucRoom.getMember().getTalkTime() > System.currentTimeMillis() / 1000) {// ??????
                                                 DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_ban));
                                                 faultTolerance();
                                             } else {
                                                 currentSendSerialNumber++;
                                                 forwardingStep(friend);
                                             }
                                         }
                                     }
                                 } else {// ???????????????
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, friend.getUserId(), 2);// ????????????????????????
                                     DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_disbanded));
                                     faultTolerance();
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                                 faultTolerance();
                             }
                         }
                );
    }

    /**
     * ???????????????
     */
    class ClickListener implements OnClickListener {
        private List<Friend> friends;

        public ClickListener(List<Friend> friends) {
            sendSize = friends.size();
            this.friends = friends;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_send:// ??????
                    for (int i = 0; i < friends.size(); i++) {
                        if (friends.get(i).getRoomFlag() != 0) {// ?????????????????????????????????????????????
                            if (friends.get(i).getIsLostChatKeyGroup() == 1) {
                                ToastUtil.showToast(mContext, getString(R.string.is_lost_key_cannot_support_send_msg, friends.get(i).getNickName()));
                                faultTolerance();
                            } else {
                                isSupportSend(friends.get(i));
                            }
                        } else {
                            currentSendSerialNumber++;
                            forwardingStep(friends.get(i));
                        }
                    }
                    break;
                case R.id.btn_cancle:
                    break;
                default:
                    break;
            }
        }
    }

    class MessageRecentlyAdapter extends BaseAdapter implements SectionIndexer {
        List<BaseSortModel<Friend>> mSortFriends;

        public MessageRecentlyAdapter() {
            mSortFriends = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Friend>> sortFriends) {
            mSortFriends = sortFriends;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mSortFriends != null) {
                return mSortFriends.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mSortFriends != null) {
                return mSortFriends.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (mSortFriends != null) {
                return position;
            }

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(InstantMessageActivity.this, R.layout.item_recently_contacts, null);
                holder = new ViewHolder();
                holder.mIvHead = (MessageAvatar) convertView.findViewById(R.id.iv_recently_contacts_head);
                holder.mTvName = (TextView) convertView.findViewById(R.id.tv_recently_contacts_name);
                holder.checkBox = convertView.findViewById(R.id.cb_instant);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.checkBox.setVisibility(View.VISIBLE);

            Friend friend = mSortFriends.get(position).getBean();
            ColorStateList tabColor = SkinUtils.getSkin(InstantMessageActivity.this).getTabColorState();
            if (friend != null) {
                holder.checkBox.setChecked(false);
                if (friend.getStatus() == 100) {
                    holder.checkBox.setChecked(true);
                    Drawable drawable = getResources().getDrawable(R.drawable.sel_check_wx2);
                    drawable = DrawableCompat.wrap(drawable);
                    DrawableCompat.setTintList(drawable, tabColor);
                    holder.checkBox.setButtonDrawable(drawable);
                } else {
                    holder.checkBox.setChecked(false);
                    holder.checkBox.setButtonDrawable(getResources().getDrawable(R.drawable.sel_nor_wx2));
                }
            }
            holder.mIvHead.fillData(friend);
            holder.mTvName.setText(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName());
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortFriends.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortFriends.get(position).getFirstLetter().charAt(0);
        }
    }

    class ViewHolder {
        MessageAvatar mIvHead;
        TextView mTvName;
        CheckBox checkBox;
    }

    private class HorListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSelectPositions.size();
        }

        @Override
        public Object getItem(int position) {
            return mSelectPositions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new HeadView(mContext);
                int size = DisplayUtil.dip2px(mContext, 37);
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
                convertView.setLayoutParams(param);
            }
            HeadView imageView = (HeadView) convertView;
            String selectPosition = mSelectPositions.get(position);
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), selectPosition);
            AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), friend, imageView);
            return convertView;
        }
    }
}
