package gdaswani.sn.client.oauth.sn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import gdaswani.sn.client.oauth.OAuthProvider;
import gdaswani.sn.client.oauth.model.AccessToken;
import gdaswani.sn.client.oauth.model.Configuration;

public final class SNProvider implements OAuthProvider {

    private static final Logger LOGGER = Logger.getLogger(SNProvider.class.getName());

    private static final String PROP_ACCESS_TOKEN = "accessToken";
    private static final String PROP_EXPIRES_IN = "expiresIn";
    private static final String PROP_GENERATED_ON = "generatedOn";
    private static final String PROP_REFRESH_TOKEN = "refreshToken";
    private static final String PROP_REFRESH_TOKEN_GENERATED_ON = "refreshTokenGeneratedOn";
    private static final String PROP_SCOPE = "scope";
    private static final String PROP_TOKEN_TYPE = "tokenType";
    private static final String TRANSIENT_FILE = "./data/snOAuth.prop";
    private AccessToken accessToken = null;
    private final Configuration config;
    private LocalDateTime refreshTokenGeneratedOn;
    private String refreshTokenValue;

    public SNProvider(final Configuration config) {

	super();

	this.config = config;

	Properties prop = new Properties();

	File dataFile = new File(TRANSIENT_FILE);

	if (!dataFile.exists()) {

	    LOGGER.log(Level.FINE, "creating a new data file");

	    prop.setProperty(PROP_ACCESS_TOKEN, "");
	    prop.setProperty(PROP_TOKEN_TYPE, "");
	    prop.setProperty(PROP_EXPIRES_IN, "");
	    prop.setProperty(PROP_GENERATED_ON, "");
	    prop.setProperty(PROP_REFRESH_TOKEN, "");
	    prop.setProperty(PROP_REFRESH_TOKEN, "");
	    prop.setProperty(PROP_SCOPE, "");
	    prop.setProperty(PROP_REFRESH_TOKEN_GENERATED_ON, "");

	    try (FileOutputStream fos = new FileOutputStream(dataFile)) {
		prop.store(fos, "SN OAUTH DATA FILE");
	    } catch (IOException ioe) {
		throw new IllegalStateException(ioe);
	    }

	} else {

	    LOGGER.log(Level.CONFIG, "loading from data file");

	    try (FileInputStream fis = new FileInputStream(new File(TRANSIENT_FILE))) {

		prop.load(fis);

		String at = prop.getProperty(PROP_ACCESS_TOKEN).trim();
		at = (at != null && at.trim().length() > 0) ? at.trim() : null;

		String type = prop.getProperty(PROP_TOKEN_TYPE);
		type = (type != null && type.trim().length() > 0) ? type.trim() : null;

		String expiresIn = prop.getProperty(PROP_EXPIRES_IN);
		expiresIn = (expiresIn != null && expiresIn.trim().length() > 0) ? expiresIn.trim() : null;

		String generatedOn = prop.getProperty(PROP_GENERATED_ON);
		generatedOn = (generatedOn != null && generatedOn.trim().length() > 0) ? generatedOn.trim() : null;

		String refreshToken = prop.getProperty(PROP_REFRESH_TOKEN);
		refreshToken = (refreshToken != null && refreshToken.trim().length() > 0) ? refreshToken.trim() : null;

		String scope = prop.getProperty(PROP_SCOPE);
		scope = (scope != null && scope.trim().length() > 0) ? scope.trim() : null;

		String refreshTokenGeneratedOn = prop.getProperty(PROP_REFRESH_TOKEN_GENERATED_ON);
		refreshTokenGeneratedOn = (refreshTokenGeneratedOn != null
			&& refreshTokenGeneratedOn.trim().length() > 0) ? refreshTokenGeneratedOn.trim() : null;

		if (at != null && type != null && expiresIn != null && generatedOn != null && refreshToken != null
			&& scope != null && refreshTokenGeneratedOn != null) {

		    accessToken = new SNAccessToken(at, type, Integer.parseInt(expiresIn),
			    LocalDateTime.parse(generatedOn), refreshToken, scope);

		    this.refreshTokenValue = refreshToken;
		    this.refreshTokenGeneratedOn = LocalDateTime.parse(refreshTokenGeneratedOn);

		}

	    } catch (IOException e) {

		LOGGER.log(Level.SEVERE, e.getMessage(), e);

		throw new IllegalStateException("Could not read data from file " + TRANSIENT_FILE);

	    }

	}

    }

    @Override
    public synchronized AccessToken getAccessToken() {

	LOGGER.log(Level.FINE, "getting SN access token");

	if (accessToken == null || accessToken.isExpired()) {

	    if (!isRefreshTokenExpired()) {
		refreshAccessToken();
	    } else {
		retrieveAccessToken();
	    }

	}

	return accessToken;

    }

    private boolean isRefreshTokenExpired() {

	LOGGER.log(Level.FINE, "refreshTokenExpired?");

	boolean isExpired = true;

	if (refreshTokenValue != null && (refreshTokenGeneratedOn != null && refreshTokenGeneratedOn
		.plusSeconds(Math.round(config.getSnRefreshLimitInSeconds() * .9)).isAfter(LocalDateTime.now()))) {
	    isExpired = false;
	}

	return isExpired;

    }

    private void refreshAccessToken() {

	LOGGER.log(Level.FINE, "retrieving a new access token with a refresh token");

	Map<String, String> parameters = new HashMap<>();
	parameters.put("grant_type", "refresh_token");
	parameters.put("client_id", config.getSnClientId());
	parameters.put("client_secret", config.getSnClientSecret());
	parameters.put("refresh_token", refreshTokenValue);

	String form = parameters.entrySet().stream()
		.map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
		.collect(Collectors.joining("&"));

	HttpRequest request = HttpRequest.newBuilder().uri(URI.create(config.getSnOAuthEndPoint()))
		.header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(form))
		.build();

	HttpClient client = HttpClient.newHttpClient();

	try {

	    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

	    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
		throw new IllegalStateException("Failure retrieving access token");
	    }

	    ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
	    saveAccessToken(mapper.readValue(response.body(), SNAccessToken.class));

	} catch (Throwable t) {
	    LOGGER.log(Level.SEVERE, t.getMessage(), t);
	    throw new IllegalStateException(t);
	}

    }

    private void retrieveAccessToken() {

	LOGGER.log(Level.FINE, "retrieving a new access token");

	Map<String, String> parameters = new HashMap<>();
	parameters.put("username", config.getSnUsername());
	parameters.put("password", config.getSnPassword());
	parameters.put("grant_type", "password");
	parameters.put("client_id", config.getSnClientId());
	parameters.put("client_secret", config.getSnClientSecret());

	String form = parameters.entrySet().stream()
		.map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
		.collect(Collectors.joining("&"));

	HttpRequest request = HttpRequest.newBuilder().uri(URI.create(config.getSnOAuthEndPoint()))
		.header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(form))
		.build();

	HttpClient client = HttpClient.newHttpClient();

	try {

	    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

	    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
		throw new IllegalStateException("Failure retrieving access token");
	    }

	    ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
	    saveAccessToken(mapper.readValue(response.body(), SNAccessToken.class));

	} catch (Throwable t) {
	    LOGGER.log(Level.SEVERE, t.getMessage(), t);
	    throw new IllegalStateException(t);
	}

    }

    private void saveAccessToken(final SNAccessToken accessToken) {

	LOGGER.log(Level.FINE, "saving token to data file");

	try (FileOutputStream fos = new FileOutputStream(new File(TRANSIENT_FILE))) {

	    Properties prop = new Properties();

	    prop.setProperty(PROP_ACCESS_TOKEN, accessToken.getAccessTokenValue());
	    prop.setProperty(PROP_TOKEN_TYPE, accessToken.getTokenType());
	    prop.setProperty(PROP_EXPIRES_IN, Integer.toString(accessToken.getExpiresIn()));
	    prop.setProperty(PROP_GENERATED_ON, accessToken.getGeneratedOn().toString());

	    if (!accessToken.getRefreshTokenValue().equals(refreshTokenValue)) {
		this.refreshTokenValue = accessToken.getRefreshTokenValue();
		this.refreshTokenGeneratedOn = accessToken.getGeneratedOn();
	    }

	    prop.setProperty(PROP_REFRESH_TOKEN, refreshTokenValue);
	    prop.setProperty(PROP_SCOPE, accessToken.getScope());
	    prop.setProperty(PROP_REFRESH_TOKEN_GENERATED_ON, refreshTokenGeneratedOn.toString());

	    prop.store(fos, "SN OAUTH DATA FILE");

	} catch (IOException e) {

	    LOGGER.log(Level.SEVERE, e.getMessage(), e);

	    throw new IllegalStateException("Could not save data to file " + TRANSIENT_FILE);

	}

	this.accessToken = accessToken;

    }

}
