package org.plus.score;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;
import com.google.api.services.plus.model.ActivityObject;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserServiceFactory;

public class PlusSyncer extends HttpServlet{
	private static final long serialVersionUID = 4867143563426233021L;
	private static Logger logger = Logger.getLogger(PlusSyncer.class.getName());
	public static final String QUEUE = "sync";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String userNickName = UserServiceFactory.getUserService().getCurrentUser().getNickname();
		String channelKey = userNickName+new Date().getTime();
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		String token = channelService.createChannel(channelKey);
		
		QueueFactory.getQueue(QUEUE)
				    .add(TaskOptions.Builder
				            	    .withUrl("/sync")
				            	    .param("userEmail", 
			            	    		   UserServiceFactory.getUserService()
				            	    						 .getCurrentUser()
				            	    						 .getEmail())
				            	    .param("channelKey", channelKey));
		resp.setContentType("application/json");
		resp.getWriter().write("{\"token\":\""+token+"\"}");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String userEmail = req.getParameter("userEmail");
		String channelKey = req.getParameter("channelKey");
		String nextLink = req.getParameter("nextLink");
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		User user = null;
		try{
			user = pm.getObjectById(User.class, userEmail);
			if (nextLink==null) 
				user.resetScore();
			
			GoogleAccessProtectedResource requestInitializer =
		        new GoogleAccessProtectedResource(
		        	user.getOauthToken(),
		            Util.TRANSPORT,
		            Util.JSON_FACTORY,
		            ConfigHelper.CLIENT_ID,
		            ConfigHelper.CLIENT_SECRET,
		            user.getRefreshToken()
		       );

			Long expirationTime = user.getTokenRetrievalTime()+user.getTokenExpirationTime();
			if (expirationTime<new Date().getTime()){
				if (requestInitializer.refreshToken()){
					user.setOauthToken(requestInitializer.getAccessToken());
					user.setRefreshToken(requestInitializer.getRefreshToken());
					user.setTokenRetrievalTime(new Date().getTime());
					//expires should be the same here
				}
			}
			Plus plus = new Plus(Util.TRANSPORT, requestInitializer, Util.JSON_FACTORY);
			Plus.Activities.List listActivities = plus.activities.list("me", "public");
			listActivities.setMaxResults(10L);
			if (nextLink!=null) listActivities.setPageToken(nextLink);
			ActivityFeed activityFeed = listActivities.execute();
			List<Activity> activities = activityFeed.getItems();
			
			
			if (activities!=null){
				if (logger.isLoggable(Level.INFO)){
					logger.info("Number of activities: "+activities.size());
				}
				for (Activity activity:activityFeed.getItems()){
					ActivityObject ao = activity.getPlusObject();
					if (logger.isLoggable(Level.INFO)){
						logger.info("User had: \n"
								   +"Plus Ones:"+user.getPlusOne()+"\n"
								   +"Replies:"+user.getReplies()+"\n"
								   +"Reshares:"+user.getReshares()+"\n"
								   +"Score:"+user.getScore()+"\n");
						logger.info("Activity had: \n"
								   +"Plus Ones:"+ao.getPlusoners().getTotalItems().intValue()+"\n"
								   +"Replies:"+ao.getReplies().getTotalItems().intValue()+"\n"
								   +"Reshares:"+ao.getResharers().getTotalItems().intValue()+"\n");
					}
					user.addPlusOne(ao.getPlusoners().getTotalItems().intValue());
					user.addReplies(ao.getReplies().getTotalItems().intValue());
					user.addReshares(ao.getResharers().getTotalItems().intValue());
					user.setScore(user.getPlusOne()+user.getReplies()+user.getReshares());
					logger.info("User now has: \n"
							   +"Plus Ones:"+user.getPlusOne()+"\n"
							   +"Replies:"+user.getReplies()+"\n"
							   +"Reshares:"+user.getReshares()+"\n"
							   +"Score:"+user.getScore()+"\n");
				}
			}
			
			user = pm.makePersistent(user);
			
			String msg = "{\"score\":"+user.getScore()+", "
			           + "\"replies\":"+user.getReplies()+", "
			           + "\"reshares\":"+user.getReshares()+", "
			           + "\"ones\":"+user.getPlusOne();
			if (activityFeed.getNextPageToken() != null){
				QueueFactory.getQueue(QUEUE)
						    .add(TaskOptions.Builder
						            	    .withUrl("/sync")
						            	    .param("userEmail", userEmail)
						            	    .param("channelKey", channelKey)
						            	    .param("nextLink", activityFeed.getNextPageToken()));
			}else{
				msg+=", \"over\":true";
			}
			msg+="}";
			if (channelKey!=null)
				ChannelServiceFactory.getChannelService()
				 					 .sendMessage(new ChannelMessage(channelKey, msg));
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			resp.sendError(500, e.getMessage());
		}finally{
			pm.close();
		}
	}
}
