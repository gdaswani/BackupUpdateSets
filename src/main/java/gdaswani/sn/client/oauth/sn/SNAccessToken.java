package gdaswani.sn.client.oauth.sn;

import java.time.LocalDateTime;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

import gdaswani.sn.client.oauth.model.AccessToken;

final class SNAccessToken extends AccessToken {

	@JsonProperty("refresh_token")
	private String refreshTokenValue;

	@JsonProperty("scope")
	private String scope;

	public SNAccessToken() {
		super();
	}

	SNAccessToken(final String accessTokenValue, final String tokenType, final int expiresIn,
			final LocalDateTime generatedOn, final String refreshTokenValue, final String scope) {

		super(accessTokenValue, tokenType, expiresIn, generatedOn);

		this.refreshTokenValue = refreshTokenValue;
		this.scope = scope;

	}

	public String getRefreshTokenValue() {
		return refreshTokenValue;
	}

	public String getScope() {
		return scope;
	}

	public void setRefreshTokenValue(final String refreshTokenValue) {
		this.refreshTokenValue = refreshTokenValue;
	}

	public void setScope(final String scope) {
		this.scope = scope;
	}

	public String toString() {
		return new ReflectionToStringBuilder(this).appendSuper(super.toString()).toString();
	}

}
