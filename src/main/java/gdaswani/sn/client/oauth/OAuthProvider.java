package gdaswani.sn.client.oauth;

import gdaswani.sn.client.oauth.model.AccessToken;

public interface OAuthProvider {

    AccessToken getAccessToken();

}
