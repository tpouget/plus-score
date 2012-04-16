package org.plus.score;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase{
	static{
		ObjectifyService.register(User.class);
	}
	
	public DAO() {
		super();
	}
	public DAO(ObjectifyOpts ogyOpts) {
		super(ogyOpts);
	}

	public User getOrCreateUser(){
		String userEmail = UserServiceFactory.getUserService().getCurrentUser().getEmail();
		return getOrCreateUser(userEmail);
	}
	
	public User getOrCreateUser(String id){
		User user = null;
		try{
			user = ofy().get(User.class, id);
		}catch(NotFoundException e){
			user = new User(id);
			ofy().put(user);
		}
		return user;
	}
	
	public List<User> getRankedUsers(){
		Objectify ofy = ObjectifyService.begin();
		Query<User> q = ofy.query(User.class)
				     	   .order("-score");
		ArrayList<User> rankedUsers = new ArrayList<User>();
		for (User user:q){
			if (user.getId()!=null) rankedUsers.add(user);
			if (rankedUsers.size()>=25) break;
		}
		return rankedUsers;
	}
	public boolean userHasOAuthToken(String userEmail) {
		return getOrCreateUser(userEmail).getOauthToken()!=null;
	}
}
