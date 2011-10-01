package org.plus.score;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.appengine.api.users.UserServiceFactory;

public class Users {
	public static User checkUserExists(){
		String userEmail = UserServiceFactory.getUserService().getCurrentUser().getEmail();
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		User user = null;
		try {
			user = pm.getObjectById(User.class, userEmail);
		} catch (JDOObjectNotFoundException e) {
			user = new User(userEmail);
			pm.makePersistent(user);
		} finally {
			pm.close();
		}
		
		return user;
	}
	
	@SuppressWarnings("unchecked")
	public static List<User> getRankedUsers(){
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Query q = pm.newQuery(User.class);
		q.setOrdering("score desc");
		q.setRange(0, 25);
		List<User> users = null;
		try{
			users = (List<User>) q.execute();
			return new ArrayList<User>(pm.detachCopyAll(users));
		}finally{
			pm.close();
		}
	}
}
