<%@ page import="java.util.List" %>

<%@ page import="com.google.appengine.api.users.UserService"%> 
<%@ page import="com.google.appengine.api.users.UserServiceFactory"%>

<%@ page import="com.google.api.services.plus.Plus" %>
<%@ page import="com.google.api.client.http.HttpResponseException" %>
<%@ page import="com.google.api.services.plus.model.*" %>

<%@ page import="org.plus.score.DAO"%>
<%@ page import="org.plus.score.User"%>

<%@ page import="java.util.logging.Logger" %>
<% final Logger log = Logger.getLogger("/index.jsp"); %>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Google Plus Score</title>
    <meta name="description" content="">
    <meta name="author" content="Thibault Pouget">
    <link rel="author" href="https://plus.google.com/118230138066120295920">
    <meta name="keywords" content="Google+ score, Hackathon, Google+ API, OAuth2">

    <!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <link rel="stylesheet" href="bootstrap.min.css">
    <style type="text/css">
      body {
        padding-top: 50px;
      }
    </style>
    <script type="text/javascript" src="jquery-1.6.4.min.js"></script>
    <script type="text/javascript" src="/_ah/channel/jsapi"></script>
    <script type="text/javascript" src="https://apis.google.com/js/plusone.js"></script>
    <script type="text/javascript">
      $(document).ready(function(){
        toggleLoader = function(){
          $("#refresh").css("display", 
          $("#refresh").css("display")=="none"?"block":"none");
          $("#loader").css("display", $("#loader").css("display")=="none"?"block":"none");
        };
        onOpened = function(){
          console.log("yeah, channel opened!");
		};
        onMessage = function(msg){
          var data = $.parseJSON(msg.data);
          $("#score").html(data.score);
          $("#replies").html(data.replies);
          $("#reshares").html(data.reshares);
          $("#ones").html(data.ones);
          if(data.over) {
            toggleLoader();
          }
        };
        $("#refreshButton").click(function(){
          $("#score").html(0);
          $("#replies").html(0);
          $("#reshares").html(0);
          $("#ones").html(0);
          toggleLoader();
          $.ajax({ 
            url: "refresh",
            success: function(data){
              channel = new goog.appengine.Channel(data.token);
              var handler = {
                'onopen': onOpened,
                'onmessage': onMessage,
                'onerror': function(e) {
                  console.log("Channel error: "+e.code+", "+e.description);
                  this.close();
                },
                'onclose': function() {
                  console.log("Channel closed");
                  toggleLoader();
                }
              };
              var socket = channel.open(handler);
            }
          });
        });
//         $("#search").keyup(function(){
//           var keyword = $("#search").val();
//           var callback = $entry(
//             function(result) {
//               $(#results).html(result);
//             });
//             $wnd.googleapis.newHttpRequest({
//                              'url': 'https://www.googleapis.com/plus/v1/people?query='+keyword+'&maxResult=5&fields=items(displayName%2Cid%2Cimage)&pp=1&key=AIzaSyCjfzlCouV2gNn_8Z0w4HHgUCD48vZTr5A',
//                              'httpMethod' : 'GET'
//                            })
// 			               .execute(callback);
// 	    	});
 		});
    </script>
  </head>
  <body>
    <div class="container">
      <div class="hero-unit" style="padding: 15px 60px 40px;">
        <h1 style="text-align: center;margin-bottom:18px">Google+ Score</h1>
        <% 
		   UserService userService = UserServiceFactory.getUserService();
           DAO dao = new DAO();
           User user = null;
           if (userService.isUserLoggedIn()){
        	   user = dao.getOrCreateUser();
           }
           if (user != null && user.getDisplayName() != null){
        	   if (user.getUsername().endsWith("@gmail.com")){ %>
   	      		<table class="zebra-striped">
   		          <tr>
   		            <td style="line-height: 10px; width: 96px; padding: 0px;">
   		            	<img src="<%= user.getAvatarUrl()+(user.getAvatarUrl().endsWith("NoPictureDark65.jpg")?"":"?size=128") %>" height="128px"/>
   		            </td>
   		            <td style="padding: 0px; font-size: 24px;vertical-align: middle; text-align: center;"><%= user.getDisplayName() %></td>
   		            <td style="padding: 0px; font-size: 16px;vertical-align: middle; text-align: center;">
   		            	<span id="replies" style="font-weight: bold"><%= user.getReplies() %></span> replies, <br/>
   		            	<span id="ones" style="font-weight: bold"><%= user.getPlusOne() %></span> +1s <br/>
   		            	<span id="reshares" style="font-weight: bold"><%= user.getReshares() %></span> reshares
   		            </td>
   		            <td style="padding: 0px; font-size: 16px;vertical-align: middle; text-align: center;">
   		            	<p>Score<br/>
   		            	<span id="score" style="font-weight: bold;"><%= user.getScore() %></span>
   		            	</p>
   		            	<p id="refresh"><span id="refreshButton" class="btn primary">refresh</span></p>
   		            	<p id="loader" style="display: none;">
   		            		Wait for it...
   						</p>
   		            </td>
   		          </tr>
   		        </table>
          			<% } else { %>
           	<div>
           		You are connected with your <b><%= user.getUsername() %></b> e-mail account.<br/>
           		This application only works with public GMail addresses since Google + isn't available for Google Apps yet.<br/> 
           		Please logout from this account and try and reconnect with your public address.
           	</div>
           	<p><a class="btn primary large" href="<%= userService.createLogoutURL("/") %>">Logout</a></p>
        			<% } 
      		}else{ %>
	        	<p>We're going to read your public feed. Analyze all of it, and made some heavy computing to know if you are a winner or a loser.</p>
	        	<p><a class="btn primary large" href="oauth2">Connect to Google+</a></p>
	     <% } %>
      </div>
<!--       <div class="hero-unit" style="padding:15px 60px;text-align:center"> -->
<!--       	<input type="text" id="search" style="width:50%;"/> -->
<!--       	<div id="results"> -->
<!--       		<div> -->
<!--       			<img alt="profile" src=""> -->
<!--       			<label></label> -->
<!--       		</div> -->
<!--  		</div> -->
<!--       </div> -->
      <div style="text-align: center;"><g:plusone/></div>
      <br/>
      <div class="hero-unit">
      	<h1 style="margin-bottom: 50px;">Top 25 ranking</h1>
        <table class="zebra-striped">
          <% List<User> users = dao.getRankedUsers(); 
          	 if (users!=null){
          	 	for (int rank=0;rank<users.size();rank++){ 
          	 		User u = users.get(rank) ;%>
          		<tr>
	                <td style="width: 96px; padding: 0px; font-size: 56px;vertical-align: middle; text-align: center;"><%= rank+1 %></td>
	                <td style="line-height: 10px; width: 96px; padding: 0px;">
	                	<img src="<%= u.getAvatarUrl()+(u.getAvatarUrl().endsWith("NoPictureDark65.jpg")?"":"?size=128") %>" height="128px"/>
	               	</td>
	                <td style="padding: 0px; font-size: 24px;vertical-align: middle; text-align: center;">
	                	<a href="<%= u.getProfileUrl()!=null?u.getProfileUrl():"#" %>" ><%= u.getDisplayName() %></a>
	                </td>
	                <td style="padding: 0px; font-size: 16px;vertical-align: middle; text-align: center;">
		            	<b><%= u.getReplies() %></b> replies<br/>
		            	<b><%= u.getPlusOne() %></b> +1s<br/>
		            	<b><%= u.getReshares() %></b> reshares
		            </td>
	                <td style="padding: 0px; font-size: 60px;vertical-align: middle; text-align: center;"><b><%= u.getScore() %></b></td>
	              </tr>
          <% }} %>
        </table>
      </div> 
    </div>
  </body>
</html>