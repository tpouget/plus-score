package org.plus.score;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.appengine.auth.oauth2.AbstractAppEngineAuthorizationCodeServlet;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class OAuth2Servlet extends AbstractAppEngineAuthorizationCodeServlet  {
	
  //private static final Logger log = Logger.getLogger(OAuth2Servlet.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	//The authentication is all done! Launch the sync.
	updateConnectedUserScore();
	//Redirect back to the samples index so you can play with them.
	resp.sendRedirect("/");
  }
  
  private void updateConnectedUserScore() throws IOException{
	  	com.google.appengine.api.users.User gaeUser = UserServiceFactory.getUserService().getCurrentUser();
	  	/* A user once had a problem here: his user was null, not sure why. */
	  	if (gaeUser==null){
	  		//TODO something smart
	  	}else{
	  		//Hum
	  	}
	}

  @Override
  protected AuthorizationCodeFlow initializeFlow() throws ServletException,
		IOException {
	  return Utils.newFlow();
  }

  @Override
  protected String getRedirectUri(HttpServletRequest req)
		throws ServletException, IOException {
	  return Utils.getRedirectUri(req);
  }
}
