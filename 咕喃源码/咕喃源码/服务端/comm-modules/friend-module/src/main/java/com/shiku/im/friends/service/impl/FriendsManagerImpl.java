package com.shiku.im.friends.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.shiku.common.model.PageResult;
import com.shiku.common.model.PageVO;
import com.shiku.commons.thread.ThreadUtils;
import com.shiku.im.comm.constants.KConstants;
import com.shiku.im.comm.ex.ServiceException;
import com.shiku.im.comm.model.MessageBean;
import com.shiku.im.comm.utils.DateUtil;
import com.shiku.im.comm.utils.ReqUtil;
import com.shiku.im.comm.utils.StringUtil;
import com.shiku.im.friends.dao.FriendsDao;
import com.shiku.im.friends.entity.Friends;
import com.shiku.im.friends.entity.NewFriends;
import com.shiku.im.friends.service.FriendsManager;
import com.shiku.im.friends.service.FriendsRedisRepository;
import com.shiku.im.message.IMessageRepository;
import com.shiku.im.message.MessageService;
import com.shiku.im.message.MessageType;
import com.shiku.im.support.Callback;
import com.shiku.im.user.dao.OfflineOperationDao;
import com.shiku.im.user.entity.AuthKeys;
import com.shiku.im.user.entity.OfflineOperation;
import com.shiku.im.user.entity.User;
import com.shiku.im.user.event.UserChageNameEvent;
import com.shiku.im.user.service.AuthKeysService;
import com.shiku.im.user.service.RoleCoreService;
import com.shiku.im.user.service.UserCoreService;
import com.shiku.im.utils.SKBeanUtils;
import com.shiku.im.vo.JSONMessage;
import com.shiku.utils.MapUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FriendsManagerImpl implements FriendsManager {

	private static final String groupCode = "110";

	private static Logger Log = LoggerFactory.getLogger(FriendsManager.class);
	@Lazy
	@Autowired
	private FriendsDao friendsDao;
	public FriendsDao getFriendsDao(){
		return friendsDao;
	}
	@Autowired
	private OfflineOperationDao offlineOperationDao;

	@Autowired
	private AuthKeysService authKeysService;

	@Autowired
	private AddressBookManagerImpl addressBookManager;

	@Autowired
	private FriendGroupManagerImpl friendGroupManager;

	@Autowired
	private FriendsRedisRepository firendsRedisRepository;

	@Autowired
	@Lazy
	private MessageService messageService;

	@Autowired(required = false)
	private IMessageRepository messageRepository;

	@Autowired
	private RoleCoreService roleCoreService;

	@Autowired
	private UserCoreService userManager;

	public FriendsRedisRepository getFirendsRedisRepository() {
		return firendsRedisRepository;
	}
	

	private  UserCoreService getUserManager(){
		return userManager;
	};
	
	@Override
	public Friends addBlacklist(Integer userId, Integer toUserId) {
		// ????????????AB??????
		Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
		Friends friendsBA= getFriendsDao().getFriends(toUserId, userId);

		if (null == friendsAB) {
			Friends friends = new Friends(userId, toUserId,getUserManager().getNickName(toUserId), Friends.Status.Stranger, Friends.Blacklist.Yes,0);
			getFriendsDao().saveFriends(friends);
		}else {
			// ????????????
			getFriendsDao().updateFriends(new Friends(userId, toUserId,null, -1, Friends.Blacklist.Yes,(0 == friendsAB.getIsBeenBlack())?0:friendsAB.getIsBeenBlack()));
			if(null==friendsBA){
				Friends friends = new Friends(toUserId, userId,getUserManager().getNickName(userId), Friends.Status.Stranger, Friends.Blacklist.No,1);
				getFriendsDao().saveFriends(friends);
			}else {
				getFriendsDao().updateFriends(new Friends(toUserId, userId,null, null, (0 == friendsBA.getBlacklist()?Friends.Blacklist.No:friendsBA.getBlacklist()),1));
			}

		}
		messageRepository.deleteLastMsg(userId.toString(), toUserId.toString());
		//messageRepository.deleteLastMsg(toUserId.toString(),userId.toString());

		/**
		 * ????????????????????????
		 */
		friendGroupManager.deleteFriendToFriendGroup(userId,toUserId);

		// ??????????????????
		deleteFriendsInfo(userId, toUserId);
		// ??????????????????????????????
		updateOfflineOperation(userId, toUserId);
		return getFriendsDao().getFriends(userId, toUserId);
	}

	/** @Description: ????????????????????????????????? 
	* @param userId
	* @param toUserId
	**/ 
	private void deleteAddressFriendsInfo(Integer userId,Integer toUserId){
		// ???????????????id
		firendsRedisRepository.delAddressBookFriendsUserIds(userId);
		firendsRedisRepository.delAddressBookFriendsUserIds(toUserId);
		deleteFriendsInfo(userId, toUserId);
	}

	/** @Description: ????????????????????????
	* @param userId
	* @param toUserId
	**/ 
	public void deleteFriendsInfo(Integer userId,Integer toUserId){
		// ??????userIdsList
		firendsRedisRepository.deleteFriendsUserIdsList(userId);
		firendsRedisRepository.deleteFriendsUserIdsList(toUserId);
		// ????????????
		firendsRedisRepository.deleteFriends(userId);
		firendsRedisRepository.deleteFriends(toUserId);
	}
	
	// ???????????????????????????????????????????????????
	public Friends consoleAddBlacklist(Integer userId, Integer toUserId,Integer adminUserId) {
		// ????????????AB??????
		Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
		Friends friendsBA= getFriendsDao().getFriends(toUserId, userId);
		if (null == friendsAB) {
			Friends friends = new Friends(userId, toUserId,getUserManager().getNickName(toUserId), Friends.Status.Stranger, Friends.Blacklist.Yes,0);
			getFriendsDao().saveFriends(friends);
		} else {
			// ????????????
			getFriendsDao().updateFriends(new Friends(userId, toUserId,null, -1, Friends.Blacklist.Yes,(0 == friendsAB.getIsBeenBlack())?0:friendsAB.getIsBeenBlack()));
			getFriendsDao().updateFriends(new Friends(toUserId, userId,null, null, (0 == friendsBA.getBlacklist()?Friends.Blacklist.No:friendsBA.getBlacklist()),1));
		}
		messageRepository.deleteLastMsg(userId.toString(), toUserId.toString());
		ThreadUtils.executeInThread(new Callback() {
			
			@Override
			public void execute(Object obj) {

				//xmpp????????????
				MessageBean messageBean=new MessageBean();
				messageBean.setType(MessageType.joinBlacklist);
				messageBean.setFromUserId(adminUserId+"");
				messageBean.setFromUserName("?????????????????????");
				MessageBean beanVo = new MessageBean();
				beanVo.setFromUserId(userId+"");
				beanVo.setFromUserName(getUserManager().getNickName(userId));
				beanVo.setToUserId(toUserId+"");
				beanVo.setToUserName(getUserManager().getNickName(toUserId));
				messageBean.setObjectId(JSONObject.toJSONString(beanVo));
				messageBean.setMessageId(StringUtil.randomUUID());
				try {
					List<Integer> userIdlist = new ArrayList<Integer>();
					userIdlist.add(userId);
					userIdlist.add(toUserId);
					messageService.send(messageBean,userIdlist);
				} catch (Exception e) {
				}
			
			}
		});
		// ??????????????????
		deleteFriendsInfo(userId, toUserId);
		// ??????????????????????????????
		updateOfflineOperation(userId, toUserId);
		return getFriendsDao().getFriends(userId, toUserId);
	}
	
	
	
	
	public Friends updateFriends(Friends friends){
		return getFriendsDao().updateFriends(friends);
	}
	
	public boolean isBlack(Integer toUserId) {
		Friends friends = getFriends(ReqUtil.getUserId(), toUserId);
		if (friends == null)
			return false;
		return friends.getStatus() == -1 ? true : false;
	}
	
	public boolean isBlack(Integer userId,Integer toUserId) {
		Friends friends = getFriends(userId, toUserId);
		if (friends == null)
			return false;
		return friends.getStatus() == -1 ? true : false;
	}

	private void saveFansCount(int userId) {
		/*BasicDBObject q = new BasicDBObject("_id", userId);
		DBCollection dbCollection = SKBeanUtils.getTigaseDatastore().getDB().getCollection("shiku_msgs_count");
		if (0 == dbCollection.count(q)) {
			BasicDBObject jo = new BasicDBObject("_id", userId);
			jo.put("count", 0);// ?????????
			jo.put("fansCount", 1);// ?????????
			dbCollection.insert(jo);
		} else {
			dbCollection.update(q, new BasicDBObject("$inc", new BasicDBObject("fansCount", 1)));
		}*/
	}

	@Override
	public boolean addFriends(Integer userId, Integer toUserId) {

		int toUserType = 0;
		List<Integer> toUserRoles = roleCoreService.getUserRoles(toUserId);
		if (toUserRoles.size() > 0 && null != toUserRoles) {
			if (toUserRoles.contains(2))
				toUserType = 2;
		}
		int userType = 0;
		List<Integer> userRoles = roleCoreService.getUserRoles(userId);
		if (userRoles.size() > 0 && null != userRoles) {
			if (userRoles.contains(2))
				userType = 2;
		}
		Friends friends = getFriends(userId, toUserId);
		if (null == friends) {
			getFriendsDao().saveFriends(new Friends(userId, toUserId, getUserManager().getNickName(toUserId),
					Friends.Status.Friends, 0, 0, toUserRoles, toUserType, 4));
			saveFansCount(toUserId);
		} else {
			saveFansCount(toUserId);
//			
			Map<String,Object> map = new HashMap<>();
			map.put("modifyTime", DateUtil.currentTimeSeconds());
			map.put("status", Friends.Status.Friends);
			map.put("toUserType", toUserType);
			map.put("toFriendsRole", toUserRoles);
			friendsDao.updateFriends(userId,toUserId,map);
		}
		Friends toFriends = getFriends(toUserId, userId);
		if (null == toFriends) {
			getFriendsDao().saveFriends(new Friends(toUserId, userId, getUserManager().getNickName(userId),
					Friends.Status.Friends, 0, 0, userRoles, userType, 4));
			saveFansCount(toUserId);
		} else {
			saveFansCount(toUserId);
//			
			Map<String,Object> map = new HashMap<>();
			map.put("modifyTime", DateUtil.currentTimeSeconds());
			map.put("status", Friends.Status.Friends);
			map.put("toUserType", userType);
			map.put("toFriendsRole", userRoles);
			friendsDao.updateFriends(toUserId,userId,map);
		}
		// ??????????????????????????????
		updateOfflineOperation(userId, toUserId);
		// ??????????????????
		deleteFriendsInfo(userId, toUserId);
		return true;
	}
	
	@Override
	public Friends deleteBlacklist(Integer userId, Integer toUserId) {
		// ????????????AB??????
		Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
		Friends friendsBA = getFriendsDao().getFriends(toUserId, userId);

		if (null == friendsAB) {
			// ?????????
		} else {
			// ??????????????????
			if (Friends.Blacklist.Yes == friendsAB.getBlacklist() && Friends.Status.Stranger == friendsAB.getStatus()) {
				// ????????????
				getFriendsDao().deleteFriends(userId, toUserId);
			} else {
				// ????????????
				getFriendsDao().updateFriends(new Friends(userId, toUserId,null, 2, Friends.Blacklist.No,(0 == friendsAB.getIsBeenBlack()?0:friendsAB.getIsBeenBlack())));
				if(null!=friendsBA){
					getFriendsDao().updateFriends(new Friends(toUserId, userId,null, (2 == friendsBA.getStatus()?2:friendsBA.getStatus()), (0 == friendsBA.getBlacklist()?Friends.Blacklist.No:friendsBA.getBlacklist()),0));
				}

			}
			// ????????????AB??????
			friendsAB = getFriendsDao().getFriends(userId, toUserId);
			// ??????????????????
			deleteFriendsInfo(userId, toUserId);
			// ??????????????????????????????
			updateOfflineOperation(userId, toUserId);
		}
		
		return friendsAB;
	}

	/** @Description:??????????????????????????? 
	* @param userId
	* @param toUserId
	* @return
	**/ 
	public Friends consoleDeleteBlacklist(Integer userId, Integer toUserId, Integer adminUserId) {
		// ????????????AB??????
		Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
		Friends friendsBA = getFriendsDao().getFriends(toUserId, userId);

		if (null == friendsAB) {
			// ?????????
		} else {
			// ??????????????????
			if (Friends.Blacklist.Yes == friendsAB.getBlacklist() && Friends.Status.Stranger == friendsAB.getStatus()) {
				// ????????????
				getFriendsDao().deleteFriends(userId, toUserId);
			} else {
				// ????????????
				getFriendsDao().updateFriends(new Friends(userId, toUserId,null, 2, Friends.Blacklist.No,(0 == friendsAB.getIsBeenBlack()?0:friendsAB.getIsBeenBlack())));
				getFriendsDao().updateFriends(new Friends(toUserId, userId,null, (2 == friendsBA.getStatus()?2:friendsBA.getStatus()), (0 == friendsBA.getBlacklist()?Friends.Blacklist.No:friendsBA.getBlacklist()),0));
			}
			// ????????????AB??????
			friendsAB = getFriendsDao().getFriends(userId, toUserId);
		}
		
		ThreadUtils.executeInThread(new Callback() {
			
			@Override
			public void execute(Object obj) {

				//xmpp????????????
				MessageBean messageBean=new MessageBean();
				messageBean.setType(MessageType.moveBlacklist);
				messageBean.setFromUserId(adminUserId+"");
				messageBean.setFromUserName("?????????????????????");
				MessageBean beanVo = new MessageBean();
				beanVo.setFromUserId(userId+"");
				beanVo.setFromUserName(getUserManager().getNickName(userId));
				beanVo.setToUserId(toUserId+"");
				beanVo.setToUserName(getUserManager().getNickName(toUserId));
				messageBean.setObjectId(JSONObject.toJSONString(beanVo));
				messageBean.setMessageId(StringUtil.randomUUID());
				try {
					List<Integer> userIdlist = new ArrayList<Integer>();
					userIdlist.add(userId);
					userIdlist.add(toUserId);
					messageService.send(messageBean,userIdlist);
				} catch (Exception e) {
				}
			
			
			}
		});
		// ??????????????????
		deleteFriendsInfo(userId, toUserId);
		return friendsAB;
	}
	
	@Override
	public boolean deleteFriends(Integer userId, Integer toUserId) {
		getFriendsDao().deleteFriends(userId, toUserId);
		getFriendsDao().deleteFriends(toUserId, userId);
		messageRepository.deleteLastMsg(userId.toString(), toUserId.toString());
		messageRepository.deleteLastMsg(toUserId.toString(),userId.toString());
		// ???????????????????????????
		messageRepository.delFriendsChatMsg(userId,toUserId);
		messageRepository.delFriendsChatMsg(toUserId,userId);

		// ?????????????????????
		addressBookManager.deleteAddressBook(userId, toUserId);
		addressBookManager.deleteAddressBook(toUserId, userId);


		/**
		 * ????????????????????????
		 */
		friendGroupManager.deleteFriendToFriendGroup(userId,toUserId);
		// ??????????????????
		deleteFriendsInfo(userId, toUserId);
		// ??????????????????????????????
		updateOfflineOperation(userId, toUserId);

		return true;
	}
	
	/** @Description:?????????????????????-xmpp???????????? 
	* @return
	**/
	public boolean consoleDeleteFriends(Integer userId, Integer adminUserId, String... toUserIds) {
		for(String strtoUserId : toUserIds){
			Integer toUserId = Integer.valueOf(strtoUserId);

			getFriendsDao().deleteFriends(userId, toUserId);
			getFriendsDao().deleteFriends(toUserId, userId);

			messageRepository.deleteLastMsg(userId.toString(), toUserId.toString());
			messageRepository.deleteLastMsg(toUserId.toString(),userId.toString());

			ThreadUtils.executeInThread(new Callback() {
				
				@Override
				public void execute(Object obj) {
					//????????????????????????????????????
					MessageBean messageBean=new MessageBean();
					messageBean.setType(MessageType.deleteFriends);
					messageBean.setFromUserId(adminUserId+"");
					messageBean.setFromUserName("?????????????????????");
					MessageBean beanVo = new MessageBean();
					beanVo.setFromUserId(userId+"");
					beanVo.setFromUserName(getUserManager().getNickName(userId));
					beanVo.setToUserId(toUserId+"");
					beanVo.setToUserName(getUserManager().getNickName(toUserId));
					messageBean.setObjectId(JSONObject.toJSONString(beanVo));
					messageBean.setMessageId(StringUtil.randomUUID());
					messageBean.setContent("?????????????????????????????????");
					messageBean.setMessageId(StringUtil.randomUUID());
					try {
						List<Integer> userIdlist = new ArrayList<Integer>();
						userIdlist.add(userId);
						userIdlist.add(toUserId);
						messageService.send(messageBean,userIdlist);
					} catch (Exception e) {
					}
					// ??????????????????
					deleteFriendsInfo(userId, toUserId);
				}
			});
		}		
		return true;
	}
	

	@SuppressWarnings("unused")
	@Override
	public JSONMessage followUser(Integer userId, Integer toUserId, Integer fromAddType) {
		final String serviceCode = "08";
		JSONMessage jMessage = null;
		User toUser = getUserManager().getUser(toUserId);
		int toUserType = 0;
		List<Integer> toUserRoles = roleCoreService.getUserRoles(toUserId);
		if(toUserRoles.size()>0 && null != toUserRoles){
			if(toUserRoles.contains(2))
				toUserType = 2;
			else
				return JSONMessage.failureByErrCode(KConstants.ResultCode.ProhibitAddFriends);
		}
		//???????????????
		if(null==toUser){
			if(10000==toUserId)
				return null;
			else
				return JSONMessage.failureByErrCode(KConstants.ResultCode.UserNotExist);
				
		}
			
		try {
			User user = getUserManager().getUser(userId);
			int userType = 0;
			List<Integer> userRoles = roleCoreService.getUserRoles(userId);
			if(userRoles.size()>0 && null != userRoles){
				if(userRoles.contains(2))
					userType = 2;
			}

			// ????????????AB??????
			Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
			// ????????????BA??????
			Friends friendsBA = getFriendsDao().getFriends(toUserId, userId);
			// ????????????????????????
			User.UserSettings userSettingsB = getUserManager().getSettings(toUserId);

			// ----------------------------
			// 0 0 0 0 ????????? ??????????????????
			// A B 1 0 ????????? ??????????????????
			// A B 1 1 ??????????????? ??????????????????
			// A B 2 0 ?????? ????????????
			// A B 3 0 ?????? ????????????
			// A B 2 1 ???????????? ????????????
			// A B 3 1 ???????????? ????????????
			// ----------------------------
			// ???AB?????????????????????????????????????????????
			if(null != friendsAB&&friendsAB.getIsBeenBlack()==1){
				return jMessage = JSONMessage.failureByErrCode(KConstants.ResultCode.AddFriendsFailure);
			}
			if (null == friendsAB || Friends.Status.Stranger == friendsAB.getStatus()) {
				// ????????????????????????
				if (0 == userSettingsB.getAllowAtt()) {
					jMessage = new JSONMessage(groupCode, serviceCode, "01", "???????????????????????????????????????");
				}
				// ????????????????????????
				else {
					int statusA = 0;

					// ?????????????????????????????????????????????????????????????????????????????????
					if (1 == userSettingsB.getFriendsVerify() && 2 != toUserType) {
						// ----------------------------
						// 0 0 0 0 ????????? ??????????????????
						// B A 1 0 ????????? ??????????????????
						// B A 1 1 ??????????????? ??????????????????
						// B A 2 0 ?????? ?????????
						// B A 3 0 ?????? ?????????
						// B A 2 1 ???????????? ?????????
						// B A 3 1 ???????????? ?????????
						// ----------------------------
						// ???BA????????????????????????????????????????????????
						if (null == friendsBA || Friends.Status.Stranger == friendsBA.getStatus()) {
							statusA = Friends.Status.Attention;
						} else {
							statusA = Friends.Status.Friends;

							getFriendsDao()
									.updateFriends(new Friends(toUserId, user.getUserId(),user.getNickname(), Friends.Status.Friends));
						}
					}
					// ???????????????????????????????????????????????????
					else {
						statusA = Friends.Status.Friends;

						if (null == friendsBA) {
							getFriendsDao().saveFriends(new Friends(toUserId, user.getUserId(),user.getNickname(),
									Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,userType,fromAddType));

							saveFansCount(toUserId);
						} else
							getFriendsDao()
									.updateFriends(new Friends(toUserId, user.getUserId(),user.getNickname(), Friends.Status.Friends,userType,userRoles));//??????usertype
					}

					if (null == friendsAB) {
						getFriendsDao().saveFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0,toUserRoles,toUserType,fromAddType));
						saveFansCount(toUserId);
					} else {
						getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0));
					}

					if (statusA == Friends.Status.Attention) {
						HashMap<String, Object> newMap = MapUtil.newMap("type", 1);
						newMap.put("fromAddType", fromAddType);
						jMessage = JSONMessage.success(KConstants.ResultCode.AttentionSuccess, newMap);
					} else {
						HashMap<String, Object> newMap = MapUtil.newMap("type", 2);
						newMap.put("fromAddType", fromAddType);
						jMessage = JSONMessage.success(KConstants.ResultCode.AttentionSuccessAndFriends,newMap);
					}

				}
			}
			// ???????????????????????????????????????
			else if (Friends.Blacklist.No == friendsAB.getBlacklist()) {
				if (Friends.Status.Attention == friendsAB.getStatus()) {
					// ???????????????????????????
					if(0 == userSettingsB.getFriendsVerify()){
						Integer statusA = Friends.Status.Friends;
						if (null == friendsBA) {
							getFriendsDao().saveFriends(new Friends(toUserId, user.getUserId(),user.getNickname(),Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,userType,fromAddType));
							saveFansCount(toUserId);
						} else{
							getFriendsDao().updateFriends(new Friends(toUserId, user.getUserId(),user.getNickname(), Friends.Status.Friends));
						}
						if (null == friendsAB) {
							getFriendsDao().saveFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0,toUserRoles,toUserType,fromAddType));
							saveFansCount(toUserId);
						} else {
							getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0));
						}
						HashMap<String, Object> newMap = MapUtil.newMap("type", 2);
						newMap.put("fromAddType", fromAddType);
						jMessage = JSONMessage.success(KConstants.ResultCode.AttentionSuccessAndFriends, newMap);
					}else if(1 == userSettingsB.getFriendsVerify()){
						HashMap<String, Object> newMap = MapUtil.newMap("type", 1);
						newMap.put("fromAddType", fromAddType);
						jMessage = JSONMessage.success(KConstants.ResultCode.AttentionSuccess, newMap);
					}
				} else {
					HashMap<String, Object> newMap = MapUtil.newMap("type", 2);
					newMap.put("fromAddType", fromAddType);
					jMessage = JSONMessage.success(KConstants.ResultCode.AttentionSuccessAndFriends,newMap);
				}
			}
			// ?????????????????????????????????????????????????????????
			else {
				getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), Friends.Blacklist.No));

				jMessage = null;
			}
			// ??????????????????
			deleteFriendsInfo(userId, toUserId);
			// ??????????????????????????????
			updateOfflineOperation(userId, toUserId);
		} catch (Exception e) {
			Log.error("????????????", e);
			jMessage = JSONMessage.failureByErrCode(KConstants.ResultCode.AttentionFailure);
		}
		return jMessage;
	}
	
	/** @Description:??????????????????????????????
	* @param userId
	* @param toUserId
	**/ 
	public void updateOfflineOperation(Integer userId,Integer toUserId){
//		Query<OfflineOperation> query = getDatastore().createQuery(OfflineOperation.class).field("userId").equal(userId).field("tag").equal(KConstants.MultipointLogin.TAG_FRIEND).field("friendId").equal(String.valueOf(toUserId));
		OfflineOperation offlineOperation = offlineOperationDao.queryOfflineOperation(userId,KConstants.MultipointLogin.TAG_FRIEND,String.valueOf(toUserId));
		if(null == offlineOperation){
//			getDatastore().save(new OfflineOperation(userId, KConstants.MultipointLogin.TAG_FRIEND, String.valueOf(toUserId), DateUtil.currentTimeSeconds()));
			offlineOperationDao.addOfflineOperation(userId,KConstants.MultipointLogin.TAG_FRIEND,String.valueOf(toUserId),DateUtil.currentTimeSeconds());
		}else{
//			UpdateOperations<OfflineOperation> ops = getDatastore().createUpdateOperations(OfflineOperation.class);
//			ops.set("operationTime", DateUtil.currentTimeSeconds());
//			getDatastore().update(query, ops);
			Map<String,Object> map = new HashMap<>();
			map.put("operationTime", DateUtil.currentTimeSeconds());
			offlineOperationDao.updateOfflineOperation(userId,toUserId.toString(),map);
		}
	}
	
	// ??????????????????
	@Override
	public JSONMessage batchFollowUser(Integer userId, String toUserIds) {
		JSONMessage jMessage = null;
		if(StringUtil.isEmpty(toUserIds))
			return null;
		int[] toUserId = StringUtil.getIntArray(toUserIds, ",");
		for(int i = 0; i < toUserId.length; i++){
			//???????????????
			if(userId==toUserId[i]||10000==toUserId[i])
				continue;
			User toUser = getUserManager().getUser(toUserId[i]);
			if(null==toUser)
				continue;
			int toUserType = 0;
			List<Integer> toUserRoles = roleCoreService.getUserRoles(toUserId[i]);
			if(toUserRoles.size()>0 && null != toUserRoles){
				if(toUserRoles.contains(2))
					toUserType = 2;
			}
			
			try {
				User user = getUserManager().getUser(userId);
				int userType = 0;
				List<Integer> userRoles = roleCoreService.getUserRoles(userId);
				if(userRoles.size()>0 && null != userRoles){
					if(userRoles.contains(2))
						userType = 2;
				}

				// ????????????AB??????
				Friends friendsAB = getFriendsDao().getFriends(userId, toUserId[i]);
				// ????????????BA??????
				Friends friendsBA = getFriendsDao().getFriends(toUserId[i], userId);
				// ????????????????????????
				User.UserSettings userSettingsB = getUserManager().getSettings(toUserId[i]);

				if(null != friendsAB&&friendsAB.getIsBeenBlack()==1){
//					return jMessage = JSONMessage.failure("???????????????");
//					continue;
					throw new ServiceException(KConstants.ResultCode.WasAddBlacklist);
				}
				if (null == friendsAB || Friends.Status.Stranger == friendsAB.getStatus()) {
					// ????????????????????????
					if (0 == userSettingsB.getAllowAtt()) {
//						jMessage = new JSONMessage(groupCode, serviceCode, "01", "???????????????????????????????????????");
						continue;
					}
					// ????????????????????????
					else {
						int statusA = 0;
							statusA = Friends.Status.Friends;

							if (null == friendsBA) {
								getFriendsDao().saveFriends(new Friends(toUserId[i], user.getUserId(),user.getNickname(),
										Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,userType,4));

								saveFansCount(toUserId[i]);
							} else
								getFriendsDao()
										.updateFriends(new Friends(toUserId[i], user.getUserId(),user.getNickname(), Friends.Status.Friends));

						if (null == friendsAB) {
							getFriendsDao().saveFriends(new Friends(userId, toUserId[i],toUser.getNickname(), statusA, Friends.Blacklist.No,0,toUserRoles,toUserType,4));
							saveFansCount(toUserId[i]);
						} else {
							getFriendsDao().updateFriends(new Friends(userId, toUserId[i],toUser.getNickname(), statusA, Friends.Blacklist.No,0));
						}

					}
				}
				// ???????????????????????????????????????
				else if (Friends.Blacklist.No == friendsAB.getBlacklist()) {
					if (Friends.Status.Attention == friendsAB.getStatus()){
						// ?????????????????????????????????
						getFriendsDao().updateFriends(new Friends(userId, toUserId[i],toUser.getNickname(), Friends.Status.Friends,toUserType,toUserRoles));
						// ??????????????????
						getFriendsDao().saveFriends(new Friends(toUserId[i], user.getUserId(),user.getNickname(),
								Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,"",userType));
//						continue;
					}
				}else {
					// ?????????????????????????????????????????????????????????
					getFriendsDao().updateFriends(new Friends(userId, toUserId[i],toUser.getNickname(), Friends.Blacklist.No));
					jMessage = null;
				}
				notify(userId, toUserId[i]);
				jMessage = JSONMessage.success();
				// ??????????????????
				deleteAddressFriendsInfo(userId, toUserId[i]);
				// ??????????????????????????????
				updateOfflineOperation(userId, toUserId[i]);
			} catch (Exception e) {
				Log.error("???????????????????????????", e.getMessage());
				throw  e;
			}
		}
		return jMessage;
	}
	
	
	/** @Description:????????????????????????????????? 
	* @param userId
	* @param addressBook<userid ??????id, toRemark ?????? >
	* @return
	**/ 
	public JSONMessage autofollowUser(Integer userId, Map<String, String> addressBook) {
		final String serviceCode = "08";
		Integer toUserId  = Integer.valueOf(addressBook.get("toUserId"));
		String toRemark = addressBook.get("toRemark");

//		final String serviceCode = "08";
		JSONMessage jMessage = null;
			User toUser = getUserManager().getUser(toUserId);
			int toUserType = 0;
			List<Integer> toUserRoles = roleCoreService.getUserRoles(toUserId);
			if(toUserRoles.size()>0 && null != toUserRoles){
				if(toUserRoles.contains(2))
					toUserType = 2;
			}
			//???????????????
			if(10000==toUser.getUserId())
				return null;
			try {
				User user = getUserManager().getUser(userId);
				int userType = 0;
				List<Integer> userRoles = roleCoreService.getUserRoles(userId);
				if(userRoles.size()>0 && null != userRoles){
					if(userRoles.contains(2))
						userType = 2;
				}

				// ????????????AB??????
				Friends friendsAB = getFriendsDao().getFriends(userId, toUserId);
				// ????????????BA??????
				Friends friendsBA = getFriendsDao().getFriends(toUserId, userId);
				// ????????????????????????
				User.UserSettings userSettingsB = getUserManager().getSettings(toUserId);

				if(null != friendsAB&&friendsAB.getIsBeenBlack()==1){
					return jMessage = JSONMessage.failureByErrCode(KConstants.ResultCode.AddFriendsFailure);
				}
				if (null == friendsAB || Friends.Status.Stranger == friendsAB.getStatus()) {
					// ????????????????????????
					if (0 == userSettingsB.getAllowAtt()) {
						jMessage = JSONMessage.failureByErrCode(KConstants.ResultCode.AttentionFailure);
					}
					// ????????????????????????
					else {
						int statusA = 0;
						// ???????????????????????????????????????????????????
//						else {
							statusA = Friends.Status.Friends;

							if (null == friendsBA) {
								getFriendsDao().saveFriends(new Friends(toUserId, user.getUserId(),user.getNickname(),
										Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,"",userType));

								saveFansCount(toUserId);
							} else
								getFriendsDao()
										.updateFriends(new Friends(toUserId, user.getUserId(),user.getNickname(), Friends.Status.Friends));
//						}

						if (null == friendsAB) {
							getFriendsDao().saveFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0,toUserRoles,toRemark,toUserType));
							saveFansCount(toUserId);
						} else {
							getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), statusA, Friends.Blacklist.No,0));
						}

					}
				}
				// ???????????????????????????????????????
				else if (Friends.Blacklist.No == friendsAB.getBlacklist()) {
					if (Friends.Status.Attention == friendsAB.getStatus()){
						// ?????????????????????????????????
						getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), Friends.Status.Friends,toUserType,toUserRoles));
						// ??????????????????
						getFriendsDao().saveFriends(new Friends(toUserId, user.getUserId(),user.getNickname(),
								Friends.Status.Friends, Friends.Blacklist.No,0,userRoles,"",userType));
					}
				}else {
					// ?????????????????????????????????????????????????????????
					getFriendsDao().updateFriends(new Friends(userId, toUserId,toUser.getNickname(), Friends.Blacklist.No));
					jMessage = null;
				}
				notify(userId, toUserId);
				// ??????????????????
				deleteFriendsInfo(userId, toUserId);
				jMessage = JSONMessage.success();
			} catch (Exception e) {
				Log.error("????????????", e);
				jMessage = JSONMessage.failureByErrCode(KConstants.ResultCode.AttentionFailure);
			}
		return jMessage;
	}
	
	public void notify(Integer userId,Integer toUserId){
		ThreadUtils.executeInThread((Callback) obj -> {
				MessageBean messageBean=new MessageBean();
				messageBean.setType(MessageType.batchAddFriend);
				messageBean.setFromUserId(String.valueOf(userId));
				messageBean.setFromUserName(getUserManager().getNickName(userId));
				messageBean.setToUserId(String.valueOf(toUserId));
				messageBean.setToUserName(getUserManager().getNickName(toUserId));
				messageBean.setContent(toUserId);
				messageBean.setMsgType(0);// ????????????
				messageBean.setMessageId(StringUtil.randomUUID());
				try {
					messageService.send(messageBean);
				} catch (Exception e) {
					e.printStackTrace();
				}
		});
	}
	
	public Friends getFriends(int userId, int toUserId) {
		return getFriendsDao().getFriends(userId, toUserId);
	}
	
	public void getFriends(int userId, String... toUserIds) {
		for (String strToUserId : toUserIds) {
			Integer toUserId = Integer.valueOf(strToUserId);
			Friends friends = getFriendsDao().getFriends(userId, toUserId);
			if(null == friends)
				throw new ServiceException(KConstants.ResultCode.NotYourFriends);
		
		}
//		return getFriendsRepository().getFriends(userId, toUserId);
//		return getFriendsRepository().getFriends(userId, toUserId);
	}
	
	public List<Friends> getFansList(Integer userId) {
		
		List<Friends> result =getFriendsDao().queryAllFriends(userId);
		result.forEach(friends->{
		   User	user = getUserManager().getUser(friends.getToUserId());
			
			friends.setToNickname(user.getNickname());
		});
		


		return result;
	}

	

	@Override
	public Friends getFriends(Friends p) {
		return getFriendsDao().getFriends(p.getUserId(), p.getToUserId());
	}

	@Override
	public List<Integer> queryFriendUserIdList(int userId) {
		List<Integer> result;

		try {
			result = firendsRedisRepository.getFriendsUserIdsList(userId);
			if (null != result && result.size() > 0) {

				return result;
			} else {
				result = friendsDao.queryFriendUserIdList(userId);

				firendsRedisRepository.saveFriendsUserIdsList(userId, result);
			}
			return result;
		} catch (Exception e) {
			Log.error(e.getMessage(),e);
			throw e;
		}


	}

	@Override
	public List<Friends> queryBlacklist(Integer userId,int pageIndex,int pageSize) {
		return getFriendsDao().queryBlacklist(userId,pageIndex,pageSize);
	}
	
	public PageVO queryBlacklistWeb(Integer userId, int pageIndex, int pageSize) {
		List<Friends> data = getFriendsDao().queryBlacklistWeb(userId,pageIndex,pageSize);
		return new PageVO(data, Long.valueOf(data.size()), pageIndex, pageSize);
	}


	/**
	 * ???????????????????????? ?????????
	 * @return
	 */
	@Override
	public boolean getFriendIsNoPushMsg(int userId, int toUserId) {
		Document query=new Document("userId", userId).append("toUserId", toUserId);
		query.put("offlineNoPushMsg", 1);
		Object field = getFriendsDao().queryOneField("offlineNoPushMsg",query);
		return null!=field;
	}
	@Override
	public List<Friends> queryFollow(Integer userId,int status) {
		List<Friends> userfriends = firendsRedisRepository.getFriendsList(userId);
		if(null != userfriends && userfriends.size() > 0){
			return userfriends;
		}else{
			if(0 == status)
				status = 2;  //??????
			List<Friends> result = getFriendsDao().queryFriendsList(userId,status,0,0);
			Iterator<Friends> iter = result.iterator();
			while (iter.hasNext()) { 
				Friends friends=iter.next();
				User user = getUserManager().getUser(friends.getToUserId());
				if(null==user){
					iter.remove();
					deleteFansAndFriends(friends.getToUserId());
					continue;
				}
				AuthKeys authks = authKeysService.getAuthKeys(user.getUserId());
				if(authks!=null) {
					friends.setDhMsgPublicKey(authks.getMsgDHKeyPair()!=null ? authks.getMsgDHKeyPair().getPublicKey() : "");
					friends.setRsaMsgPublicKey(authks.getMsgRsaKeyPair()!=null ? authks.getMsgRsaKeyPair().getPublicKey() : "");
				}

				friends.setToNickname(user.getNickname());
			}
			firendsRedisRepository.saveFriendsList(userId,result);
			return result;
		}
	}
	
	
	public PageResult<Friends> consoleQueryFollow(Integer userId,Integer toUserId,int status,int page,int limit) {
		PageResult<Friends> result = new PageResult<Friends>();
		result = getFriendsDao().consoleQueryFollow(userId,toUserId,status,page,limit);
		Iterator<Friends> iter = result.getData().iterator(); 
		while (iter.hasNext()) { 
			Friends friends=iter.next();
			User user = getUserManager().getUser(friends.getToUserId());
			friends.setNickname(getUserManager().getNickName(userId));
			if(null==user){
				iter.remove();
				deleteFansAndFriends(friends.getToUserId());
				continue;
			}
			friends.setToNickname(user.getNickname());
		}
		return result;
	}

	
	
	

	@Override
	public List<Integer> queryFollowId(Integer userId) {
		return getFriendsDao().queryFollowId(userId);
	}

	@Override
	public List<Friends> queryFriends(Integer userId) {
		List<Friends> result = getFriendsDao().queryFriends(userId);

		for (Friends friends : result) {
			User toUser = getUserManager().getUser(friends.getToUserId());
			if(null==toUser){
				deleteFansAndFriends(friends.getToUserId());
				continue;
			}
			friends.setToNickname(toUser.getNickname());
			//friends.setCompanyId(toUser.getCompanyId());
		}

		return result;
	}
	
	
	@Override   //???????????????userId ??????????????????userId
	public List<Integer> friendsAndAttentionUserId(Integer userId,String type) {
		List<Friends> result;
		if("friendList".equals(type) || "blackList".equals(type)){  //???????????????userId ??????????????????userId
			 result = getFriendsDao().friendsOrBlackList(userId, type);
		}else{
			throw new ServiceException(KConstants.ResultCode.ParamsAuthFail);
		}
		List<Integer> userIds = new ArrayList<Integer>();
		for (Friends friend : result) {
			userIds.add(friend.getToUserId());
		}
		return userIds;
	}

	@Override
	public PageVO queryFriends(Integer userId,int status,String keyword, int pageIndex, int pageSize) {
		PageResult<Friends> pageData = friendsDao.queryFollowByKeyWord(userId,status,keyword,pageIndex,pageSize);
		long total = pageData.getCount();
		for (Friends friends : pageData.getData()) {
			User toUser = getUserManager().getUser(friends.getToUserId());
			if(null==toUser){
				deleteFansAndFriends(friends.getToUserId());
				continue;
			}
			if(toUser.getUserId()==10000){
				continue;
			}
			friends.setToNickname(toUser.getNickname());
			AuthKeys authks = authKeysService.getAuthKeys(toUser.getUserId());
			if(authks!=null) {
				friends.setDhMsgPublicKey(authks.getMsgDHKeyPair()!=null ? authks.getMsgDHKeyPair().getPublicKey() : "");
				friends.setRsaMsgPublicKey(authks.getMsgRsaKeyPair()!=null ? authks.getMsgRsaKeyPair().getPublicKey() : "");
			}
		};
		return new PageVO(pageData.getData(), total);
	}
	public List<Friends> queryFriendsList(Integer userId,int status, int pageIndex, int pageSize) {
		List<Friends> pageData = friendsDao.queryFriendsList(userId,status,pageIndex,pageSize);
		for (Friends friends : pageData) {
			User toUser = getUserManager().getUser(friends.getToUserId());
			if(null==toUser){
				deleteFansAndFriends(friends.getToUserId());
				continue;
			}
			friends.setToNickname(toUser.getNickname());
		}
		return pageData;
	}

	
	
	/**
	 * ????????????
	 */
	@Override
	public boolean unfollowUser(Integer userId, Integer toUserId) {
		// ??????????????????
		getFriendsDao().deleteFriends(userId, toUserId);
		// ??????????????????????????????
		updateOfflineOperation(userId, toUserId);
		return true;
	}

	@Override
	public Friends updateRemark(int userId, int toUserId, String remarkName,String describe) {
		return getFriendsDao().updateFriendRemarkName(userId, toUserId, remarkName,describe);
	}

	

	@Override
	public void deleteFansAndFriends(int userId) {
		/*List<Integer> list = queryFollowId(userId);
		list.forEach(toUserId->{
			getFriendsDao().deleteFriends(toUserId,userId);

			firendsRedisRepository.deleteFriends(toUserId);
			firendsRedisRepository.deleteFriendsUserIdsList(toUserId);
		});*/
		getFriendsDao().deleteFriends(userId);



	}

	/* (non-Javadoc)
	 * @see cn.xyz.mianshi.service.FriendsManager#newFriendList(int,int,int)
	 */
	@Override
	public List<NewFriends> newFriendList(int userId, int pageIndex, int pageSize) {
		
		List<NewFriends> pageData = friendsDao.getNewFriendsList(userId,pageIndex,pageSize);
		Friends friends=null;
		for (NewFriends newFriends : pageData) {
			friends=getFriends(newFriends.getUserId(), newFriends.getToUserId());
			newFriends.setToNickname(getUserManager().getNickName(newFriends.getToUserId()));

			if(null!=friends)
				newFriends.setStatus(friends.getStatus());
		}
		return pageData;
		
	}
	
	@SuppressWarnings("deprecation")
	public PageVO newFriendListWeb(int userId,int pageIndex,int pageSize) {
		
		List<NewFriends> pageData = friendsDao.getNewFriendsList(userId,pageIndex,pageSize);
		Friends friends = null;
		for (NewFriends newFriends : pageData) {
			friends=getFriends(newFriends.getUserId(), newFriends.getToUserId());
			newFriends.setToNickname(getUserManager().getNickName(newFriends.getToUserId()));
			if(null!=friends)
				newFriends.setStatus(friends.getStatus());
		}
		return new PageVO(pageData, (long)pageData.size(), pageIndex, pageSize);
	}


	public NewFriends newFriendLast(int userId,int toUserId) {
		NewFriends newFriend = friendsDao.getNewFriendLast(userId,toUserId);
		newFriend.setToNickname(getUserManager().getNickName(newFriend.getToUserId()));
		return newFriend;
	}
	
	/* ?????????????????????????????????????????????????????????
	 * type = 0  ??????????????? ,type = 1  ???????????? ,type = 2  ????????????
	 */
	@Override
	public Friends updateOfflineNoPushMsg(int userId, int toUserId, int offlineNoPushMsg ,int type) {
		Map<String,Object> map = new HashMap<>();
		switch (type) {
		case 0:
			map.put("offlineNoPushMsg", offlineNoPushMsg);
			break;
		case 1:
			map.put("isOpenSnapchat", offlineNoPushMsg);
			break;
		case 2:
			map.put("openTopChatTime", (offlineNoPushMsg == 0 ? 0 : DateUtil.currentTimeSeconds()));
			break;
		default:
			break;
		}
		// ??????????????????????????????xmpp??????
		if(getUserManager().isOpenMultipleDevices(userId)){
			getUserManager().multipointLoginUpdateUserInfo(userId, getUserManager().getNickName(userId), toUserId,getUserManager().getNickName(toUserId),1);
		}
		firendsRedisRepository.deleteFriends(userId);
		return friendsDao.updateFriendsReturn(userId,toUserId,map);
	}
	
	
	/**
	 * ??????????????????      ??????????????????????????????????????????????????????????????????????????????
	 * @param startDate
	 * @param endDate
	 *
	 */
	public List<Object> getAddFriendsCount(String startDate, String endDate, short timeUnit){
		
		List<Object> countData = new ArrayList<>();
		
		long startTime = 0; //?????????????????????
		
		long endTime = 0; //?????????????????????,?????????????????????
		
		/**
		 * ??????????????????????????????????????????????????????????????????????????? ; ????????????????????????????????????????????????????????????????????????;
		 * ??????????????????????????????????????????????????????????????????0???
		 */
		long defStartTime = timeUnit==4? DateUtil.getTodayMorning().getTime()/1000 
				: timeUnit==3 ? DateUtil.getLastMonth().getTime()/1000 : DateUtil.getLastYear().getTime()/1000;
		
		startTime = StringUtil.isEmpty(startDate) ? defStartTime :DateUtil.toDate(startDate).getTime()/1000;
		endTime = StringUtil.isEmpty(endDate) ? DateUtil.currentTimeSeconds() : DateUtil.toDate(endDate).getTime()/1000;
				
//		BasicDBObject queryTime = new BasicDBObject("$ne",null);
//
//		if(startTime!=0 && endTime!=0){
//			queryTime.append("$gt", startTime);
//			queryTime.append("$lt", endTime);
//		}
//
//		BasicDBObject query = new BasicDBObject("createTime",queryTime);
//
//		//????????????????????????
//		DBCollection collection = SKBeanUtils.getDatastore().getCollection(getEntityClass());
		
		String mapStr = "function Map() { "   
	            + "var date = new Date(this.createTime*1000);" 
	            +  "var year = date.getFullYear();"
				+  "var month = (\"0\" + (date.getMonth()+1)).slice(-2);"  //month ???0?????????????????????1
				+  "var day = (\"0\" + date.getDate()).slice(-2);"
				+  "var hour = (\"0\" + date.getHours()).slice(-2);"
				+  "var minute = (\"0\" + date.getMinutes()).slice(-2);"
				+  "var dateStr = date.getFullYear()"+"+'-'+"+"(parseInt(date.getMonth())+1)"+"+'-'+"+"date.getDate();";
				
				if(timeUnit==1){ // counType=1: ??????????????????
					mapStr += "var key= year + '-'+ month;";
				}else if(timeUnit==2){ // counType=2:???????????????
					mapStr += "var key= year + '-'+ month + '-' + day;";
				}else if(timeUnit==3){ //counType=3 :???????????????
					mapStr += "var key= year + '-'+ month + '-' + day + '  ' + hour +' : 00';";
				}else if(timeUnit==4){ //counType=4 :??????????????????
					mapStr += "var key= year + '-'+ month + '-' + day + '  ' + hour + ':'+ minute;";
				}
	           
				mapStr += "emit(key,1);}";
		
		 String reduce = "function Reduce(key, values) {" +
			                "return Array.sum(values);" +
	                    "}";
//		 MapReduceCommand.OutputType type =  MapReduceCommand.OutputType.INLINE;//
//		 MapReduceCommand command = new MapReduceCommand(collection, mapStr, reduce,null, type,query);
		 
	
//		 MapReduceOutput mapReduceOutput = collection.mapReduce(command);
//		 Iterable<DBObject> results = mapReduceOutput.results();
//		 Map<String,Double> map = new HashMap<String,Double>();
//		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
//			DBObject obj = (DBObject) iterator.next();
//
//			map.put((String)obj.get("_id"),(Double)obj.get("value"));
//			countData.add(JSON.toJSON(map));
//			map.clear();
//
//		}
		countData = friendsDao.getAddFriendsCount(startTime,endTime,mapStr,reduce);
		return countData;
	}
	
	// ???????????????????????????
	public PageResult<Document> chardRecord(Integer sender, Integer receiver, Integer page, Integer limit){
		return messageRepository.queryFirendMsgRecord(sender,receiver,page,limit);
	}
	
	/**
	 * @Description:????????????????????????????????????
	**/ 
	public void delFriendsChatRecord(String... messageIds){
		 messageRepository.delFriendsChatRecord(messageIds);
	}


	
	/** @Description:??????????????????????????????????????? 
	* @param userId
	* @param toUserId
	* @param type  -1 ?????????????????? 1???????????????  2 ??????????????????   3 ?????????????????????
	* @return
	**/ 
	public boolean isAddressBookOrFriends(Integer userId,Integer toUserId,int type){
		boolean flag = false;
		switch (type) {
		case -1:
			break;
		case 1:
			flag = !flag;
			break;
		case 2:
			List<Integer> friendsUserIdsList= queryFriendUserIdList(userId);
			if (null != friendsUserIdsList && friendsUserIdsList.size() > 0) {
				flag = friendsUserIdsList.contains(toUserId);
			}

			break;
		case 3:
			List<Integer> addressBookUserIdsList;
			List<Integer> allAddressBookUserIdsList = firendsRedisRepository.getAddressBookFriendsUserIds(userId);
			if(null != allAddressBookUserIdsList && allAddressBookUserIdsList.size() > 0)
				addressBookUserIdsList = allAddressBookUserIdsList;
			else{
				List<Integer> AddressBookUserIdsDB = addressBookManager.getAddressBookUserIds(userId);
				addressBookUserIdsList = AddressBookUserIdsDB;
				firendsRedisRepository.saveAddressBookFriendsUserIds(userId, addressBookUserIdsList);
			}
			flag = addressBookUserIdsList.contains(toUserId);
			break;
		default:
			break;
		}
		return flag;
	}

	/**
	 * ??????????????????????????????????????????
	 * @param userId
	 * @param toUserId
	 * @param type
	 */
	public void  modifyEncryptType(int userId,int toUserId,byte type) {
		friendsDao.updateFriendsEncryptType(userId, toUserId, type);
		firendsRedisRepository.deleteFriends(userId);
		if(getUserManager().isOpenMultipleDevices(userId)){
			getUserManager().multipointLoginUpdateUserInfo(userId, getUserManager().getNickName(userId), toUserId,getUserManager().getNickName(toUserId),1);
		}
	}

	public void sendUpdatePublicKeyMsgToFriends(String dhPublicKey,String rsaPublicKey, int userId){
		List<Integer> friendIds = queryFriendUserIdList(userId);
		// ???????????????????????????
		friendIds.forEach(toUserId -> {
			deleteRedisUserFriends(toUserId);
		});
		MessageBean mb = new MessageBean();
		mb.setContent(dhPublicKey+","+rsaPublicKey);
		mb.setFromUserId(userId + "");
		mb.setFromUserName(userManager.getNickName(userId));
		mb.setMessageId(UUID.randomUUID().toString());
		mb.setMsgType(0);// ????????????
		mb.setType(MessageType.updateFriendsEncryptKey);
		messageService.send(mb,friendIds);

	}

	public void deleteRedisUserFriends(int userId){
		firendsRedisRepository.deleteFriends(userId);
	}

	@EventListener
	public void handlerUserChageNameEvent(UserChageNameEvent event){
		//log.info(" friends handlerUserChageNameEvent {}",event.getUserId());
		//??????????????????

		friendsDao.updateFriendsAttribute(0, event.getUserId(), "toNickname",event.getNickName());

	}

}
