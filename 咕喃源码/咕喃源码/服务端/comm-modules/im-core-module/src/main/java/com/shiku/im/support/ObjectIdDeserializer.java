package com.shiku.im.support;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.bson.types.ObjectId;

import java.lang.reflect.Type;

/**
* @Description: TODO(用一句话描述该文件做什么)
*
* @date 2018年7月11日 
*/
public class ObjectIdDeserializer implements ObjectDeserializer {

	@Override
	public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
		 JSONLexer lexer = parser.getLexer();
	         String value = lexer.stringVal();
	      return (T) new ObjectId(value);
	}

	@Override
	public int getFastMatchToken() {
		// TODO Auto-generated method stub
		return 0;
	}

}

