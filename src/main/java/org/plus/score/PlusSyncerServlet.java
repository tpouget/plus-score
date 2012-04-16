package org.plus.score;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.HttpResponseException;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.Activity.PlusObject;
import com.google.api.services.plus.model.ActivityFeed;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class PlusSyncerServlet extends HttpServlet{
	private static final long serialVersionUID = 4867143563426233021L;
	private static Logger logger = Logger.getLogger(PlusSyncerServlet.class.getName());
	public static final String QUEUE = "sync";
	
	static final String APP_NAME = "Plus Score";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		TaskOptions options = TaskOptions.Builder.withUrl("/sync");
		String userEmail = req.getParameter("userEmail");
		String userId = req.getParameter("userId");
		String token = null;
		if (userEmail==null) {
			com.google.appengine.api.users.User currentUser = UserServiceFactory.getUserService().getCurrentUser();
			String userNickName = currentUser.getNickname();
			String channelKey = userNickName+new Date().getTime();
			ChannelService channelService = ChannelServiceFactory.getChannelService();
			token = channelService.createChannel(channelKey);
			options = options.param("channelKey", channelKey)
							 .countdownMillis(3000);
			userEmail = currentUser.getEmail();
			userId = currentUser.getUserId();
		}
		options = options.param("userEmail", userEmail);
		options = options.param("userId", userId);
		QueueFactory.getQueue(QUEUE).add(options);
		
		if (token==null){
			resp.setContentType("text/plain");
			resp.getWriter().write("You just started a new synchronization for user "+userEmail);
		}else{
			resp.setContentType("application/json");
			resp.getWriter().write("{\"token\":\""+token+"\"}");
		}
	}
	
	

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String userEmail = req.getParameter("userEmail");
		String userId = req.getParameter("userId");
		String channelKey = req.getParameter("channelKey");
		String nextLink = req.getParameter("nextLink");
		
		Objectify ofy = ObjectifyService.begin();
		
		User user = null;
		try{			
			user = ofy.get(User.class, userEmail);
			if (nextLink==null) user.resetScore();
			
			Plus plus = Utils.loadPlusClient(userId);
			Plus.Activities.List listActivities = plus.activities().list("me", "public");
			listActivities.setMaxResults(100L);
			listActivities.setFields("items(object(plusoners,replies,resharers)),nextPageToken");
			if (nextLink!=null) listActivities.setPageToken(nextLink);
			ActivityFeed activityFeed = listActivities.execute();
			List<Activity> activities = activityFeed.getItems();
			
			if (activities!=null){
				for (Activity activity:activityFeed.getItems()){
					PlusObject ao = activity.getObject();
					user.addPlusOne(ao.getPlusoners().getTotalItems().intValue());
					user.addReplies(ao.getReplies().getTotalItems().intValue());
					user.addReshares(ao.getResharers().getTotalItems().intValue());
					user.setScore(user.getPlusOne()+user.getReplies()+user.getReshares());
				}
			}
			
			ofy.put(user);
			
			if (channelKey!=null){
				String msg = "{\"score\":"+user.getScore()+", "
				           + "\"replies\":"+user.getReplies()+", "
				           + "\"reshares\":"+user.getReshares()+", "
				           + "\"ones\":"+user.getPlusOne();
				if (activityFeed.getNextPageToken() == null)
					msg+=", \"over\":true";
				msg+="}";
				ChannelServiceFactory.getChannelService()
				 					 .sendMessage(new ChannelMessage(channelKey, msg));
			}

			if (activityFeed.getNextPageToken() != null){
				TaskOptions options = TaskOptions.Builder.withUrl("/sync");
				if(userEmail!=null) 
					options = options.param("userEmail", userEmail);
				else 
					logger.warning("userEmail was null");
				if(userId!=null) 
					options = options.param("userId", userId);
				else 
					logger.warning("userId was null");
				if(channelKey!=null) 
					options = options.param("channelKey", channelKey);
				else 
					logger.info("channelKey was null");
				if(activityFeed.getNextPageToken()!=null) 
					options = options.param("nextLink", activityFeed.getNextPageToken());
				else 
					logger.warning("nextLink was null");
				QueueFactory.getQueue(QUEUE).add(options);
			}
		}catch(HttpResponseException e){
			logger.log(Level.SEVERE, 
					   e.getStatusCode()+":"+
					   e.getMessage(), e);
			resp.sendError(500, e.getMessage());
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			resp.sendError(500, e.getMessage());
		}
	}
}
