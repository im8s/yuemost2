package com.wxpay.utils;




import java.security.MessageDigest;

public class MD5Util {

	private static String byteArrayToHexString(byte b[]) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			resultSb.append(byteToHexString(b[i]));

		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n += 256;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}
	 public static String getMD5(String content) {
	        MessageDigest messageDigest = null;
	        try {
	            messageDigest = MessageDigest.getInstance("MD5");
	            messageDigest.reset();
	            messageDigest.update(content.getBytes("UTF-8"));
	        } catch (Exception e) {
	        	e.printStackTrace();
	        } 

	        byte[] byteArray = messageDigest.digest();

	        StringBuffer md5StrBuff = new StringBuffer();

	        for (int i = 0; i < byteArray.length; i++) {
	            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
	                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
	            else
	                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
	        }

	        return md5StrBuff.toString();
	    }
	public static String MD5Encode(String origin, String charsetname) {
		String resultString = null;
		try {
			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");
			if (charsetname == null || "".equals(charsetname))
				resultString = byteArrayToHexString(md.digest(resultString
						.getBytes()));
			else
				resultString = byteArrayToHexString(md.digest(resultString
						.getBytes(charsetname)));
		} catch (Exception exception) {
		}
		return resultString;
	}

	private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

}
