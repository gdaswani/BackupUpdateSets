package gdaswani.sn.client.oauth.model;

import java.time.LocalDateTime;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessToken {

    @JsonProperty("access_token")
    private String accessTokenValue;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonIgnore
    private LocalDateTime generatedOn;

    @JsonProperty("token_type")
    private String tokenType;

    public AccessToken() {

	generatedOn = LocalDateTime.now();

    }

    protected AccessToken(final String accessTokenValue, final String tokenType, final int expiresIn, final LocalDateTime generatedOn) {

	super();

	this.accessTokenValue = accessTokenValue;
	this.tokenType = tokenType;
	this.expiresIn = expiresIn;
	this.generatedOn = generatedOn;

    }

    public String generateAuthorizationValue() {

	if (tokenType == null || accessTokenValue == null) {
	    throw new IllegalStateException("invalid access token");
	}

	return tokenType + " " + accessTokenValue;
    }

    public String getAccessTokenValue() {
	return accessTokenValue;
    }

    public int getExpiresIn() {
	return expiresIn;
    }

    public LocalDateTime getGeneratedOn() {
	return generatedOn;
    }

    public String getTokenType() {
	return tokenType;
    }

    public boolean isExpired() {

	return generatedOn.plusSeconds(Math.round(expiresIn * .9)).isBefore(LocalDateTime.now());

    }

    public void setAccessTokenValue(final String accessTokenValue) {
	this.accessTokenValue = accessTokenValue;
    }

    public void setExpiresIn(final int expiresIn) {
	this.expiresIn = expiresIn;
    }

    public void setGeneratedOn(final LocalDateTime generatedOn) {
	this.generatedOn = generatedOn;
    }

    public void setTokenType(final String tokenType) {
	this.tokenType = tokenType;
    }

    public String toString() {
	return new ReflectionToStringBuilder(this).appendSuper(super.toString()).toString();
    }
}
