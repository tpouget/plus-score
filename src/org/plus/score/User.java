package org.plus.score;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(detachable="true")
public class User {
	@PrimaryKey
	private String username;
	@Persistent private String oauthToken;
	@Persistent private String refreshToken; 
	@Persistent private Long tokenExpirationTime = 0L;
	@Persistent private Long tokenRetrievalTime = 0L;
	
	@Persistent private String displayName;
	
	@Persistent private String avatarUrl;
	
	@Persistent private int reshares = 0;
	@Persistent private int plusOne = 0;
	@Persistent private int replies = 0; 
	@Persistent private int score = 0;
	
	@Persistent private String id;
	@Persistent private String profileUrl;
	
	public User(String userEmail) {
		this.username = userEmail;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public void setOauthToken(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public int getReshares() {
		return reshares;
	}

	public void setReshares(int reshares) {
		this.reshares = reshares;
	}

	public int getPlusOne() {
		return plusOne;
	}

	public void setPlusOne(int plusOne) {
		this.plusOne = plusOne;
	}

	public int getReplies() {
		return replies;
	}

	public void setReplies(int replies) {
		this.replies = replies;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getId(){
		return id;
	}
	
	public String getProfileUrl(){
		return profileUrl;
	}
	
	public void setProfileUrl(String url){
		profileUrl = url;
	}

	public void resetScore() {
		score=0;
		plusOne=0;
		replies=0;
		reshares=0;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	public String getRefreshToken(){
		return refreshToken;
	}

	public void setTokenExpirationTime(Long expiresIn) {
		this.tokenExpirationTime = expiresIn;
	}

	public void setTokenRetrievalTime(long time) {
		this.tokenRetrievalTime = time;
	}

	public Long getTokenRetrievalTime() {
		return tokenRetrievalTime;
	}

	public void setTokenRetrievalTime(Long tokenRetrievalTime) {
		this.tokenRetrievalTime = tokenRetrievalTime;
	}

	public Long getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	public void addPlusOne(int intValue) {
		plusOne+=intValue;
	}

	public void addReplies(int intValue) {
		replies+=intValue;
	}

	public void addReshares(int intValue) {
		reshares+=intValue;
	}
}
