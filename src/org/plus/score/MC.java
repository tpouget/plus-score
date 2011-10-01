package org.plus.score;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class MC {	
	private static MemcacheService cache;
	
	private MC(){}
	
	public static MemcacheService get(){
		if (cache==null){
			cache = MemcacheServiceFactory.getMemcacheService();
		}
		return cache;
	}
}
