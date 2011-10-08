package org.plus.score;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.auth.oauth2.draft10.AuthorizationRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserServiceFactory;
import com.similarity.queen.DAOT;
import com.similarity.queen.DAOT.Transactable;

@SuppressWarnings("serial")
public class OAuth2Servlet extends HttpServlet {
  private static final Logger log = Logger.getLogger(OAuth2Servlet.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Check for an error returned by OAuth
    String error = req.getParameter("error");
    if (error != null) {
    	resp.setContentType("text/plain");
    	resp.getWriter().println("There was a problem during authentication: " + error);
    	log.severe("There was a problem during authentication: " + error);
    	return;
    }

    // When we're redirected back from the OAuth 2.0 grant page, a code will be supplied in a GET parameter named 'code'
    String code = req.getParameter("code");
    if (code == null || code.isEmpty()) {
    	if (UserServiceFactory.getUserService().isUserLoggedIn()){
    		String userEmail = UserServiceFactory.getUserService().getCurrentUser().getEmail();
    		if (!new DAO().userHasOAuthToken(userEmail)){
    			// Build the authorization URL
            	AuthorizationRequestUrl authorizeUrl = new GoogleAuthorizationRequestUrl(
                      ConfigHelper.CLIENT_ID,
                      ConfigHelper.REDIRECT_URI,
                      ConfigHelper.SCOPES
        		);
            	authorizeUrl.redirectUri = ConfigHelper.REDIRECT_URI;
            	authorizeUrl.scope = ConfigHelper.SCOPES;
            	String authorizationUrl = authorizeUrl.build();

            	log.info("Redirecting browser for OAuth 2.0 authorization to " + authorizationUrl);
            	resp.sendRedirect(authorizationUrl);
    		}else{
    			resp.sendRedirect("/");
    		}
    	}else{
    		resp.sendRedirect(UserServiceFactory.getUserService().createLoginURL("/oauth2"));
    	}
	} else {
    	// Now that we have the OAuth 2.0 code, we must exchange it for a token to make API requests.
		log.info("Exchanging OAuth code for access token using server side call");

		AccessTokenResponse accessTokenResponse = new GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant(
              new NetHttpTransport(),
              new GsonFactory(),
              ConfigHelper.CLIENT_ID,
              ConfigHelper.CLIENT_SECRET,
              code,
              ConfigHelper.REDIRECT_URI
		).execute();

		//The authentication is all done! Launch the sync.
		updateConnectedUserScore(accessTokenResponse);
		//Redirect back to the samples index so you can play with them.
		resp.sendRedirect("/");
    }
  }
  
  private void updateConnectedUserScore(final AccessTokenResponse accessTokenResponse) throws IOException{
	  	com.google.appengine.api.users.User gaeUser = UserServiceFactory.getUserService().getCurrentUser();
	  	/* A user once had a problem here: his user was null, not sure why. */
	  	if (gaeUser==null){
	  		//TODO something smart
	  	}else{
	  		final String userEmail = gaeUser.getEmail();
			
			GoogleAccessProtectedResource requestInitializer =
			        new GoogleAccessProtectedResource(
			        	accessTokenResponse.accessToken,
			            Util.TRANSPORT,
			            Util.JSON_FACTORY,
			            ConfigHelper.CLIENT_ID,
			            ConfigHelper.CLIENT_SECRET,
			            accessTokenResponse.refreshToken
		            );
			Plus plus = new Plus(Util.TRANSPORT, requestInitializer, Util.JSON_FACTORY);
			final Person mePerson = plus.people.get("me").execute();
			
			DAOT.repeatInTransaction(new Transactable() {
				@Override
				public void run(DAOT daot) {
					User user = daot.getOrCreateUser(userEmail);
					user.setDisplayName(mePerson.getDisplayName());
					user.setId(mePerson.getId());
					user.setAvatarUrl(mePerson.getImage().getUrl());
					user.setProfileUrl(mePerson.getUrl());
					user.setOauthToken(accessTokenResponse.accessToken);
					user.setRefreshToken(accessTokenResponse.refreshToken);
					user.setTokenExpirationTime(accessTokenResponse.expiresIn);
					user.setTokenRetrievalTime(new Date().getTime());
					daot.ofy().put(user);
				}
			});
			
			QueueFactory.getQueue(PlusSyncerServlet.QUEUE)
					    .add(TaskOptions.Builder
					            	    .withUrl("/sync")
					            	    .param("userEmail", userEmail));
	  	}
	}
}
