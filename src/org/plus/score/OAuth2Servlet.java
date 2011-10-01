package org.plus.score;

/*
 * Copyright (c) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
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

/**
 * A servlet which handles the OAuth 2.0 flow. Once authenticated, the OAuth token is stored in session.accessToken.
 *
 * @author Jenny Murphy
 */
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
    	// Now that we have the OAuth 2.0 code, we must exchange it for a token to make API requests.

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
    	return;
	} else {
		log.info("Exchanging OAuth code for access token using server side call");

		AccessTokenResponse accessTokenResponse = new GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant(
              new NetHttpTransport(),
              new GsonFactory(),
              ConfigHelper.CLIENT_ID,
              ConfigHelper.CLIENT_SECRET,
              code,
              ConfigHelper.REDIRECT_URI
      ).execute();

      log.info("Storing authentication token into the session");

      //The authentication is all done! Launch the sync.
      updateConnectedUserScore(accessTokenResponse);
      //Redirect back to the samples index so you can play with them.
      resp.sendRedirect("/");
    }
  }
  
  
  private void updateConnectedUserScore(AccessTokenResponse accessTokenResponse) throws IOException{
		String userEmail = UserServiceFactory.getUserService().getCurrentUser().getEmail();
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
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
		User user = null;
		try{
			user = pm.getObjectById(User.class, userEmail);	
		
			Person mePerson = plus.people.get("me").execute();
			user.setDisplayName(mePerson.getDisplayName());
			user.setId(mePerson.getId());
			user.setAvatarUrl(mePerson.getImage().getUrl());
			user.setProfileUrl(mePerson.getUrl());
			user.setOauthToken(accessTokenResponse.accessToken);
			user.setRefreshToken(accessTokenResponse.refreshToken);
			user.setTokenExpirationTime(accessTokenResponse.expiresIn);
			user.setTokenRetrievalTime(new Date().getTime());
			pm.makePersistent(user);
			
			QueueFactory.getQueue(PlusSyncer.QUEUE)
					    .add(TaskOptions.Builder
					            	    .withUrl("/sync")
					            	    .param("userEmail", userEmail));
		} finally {
			pm.close();
		}
	}
}
