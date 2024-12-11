package gdaswani.sn.client.oauth.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public final class Configuration {

    private static final String CONFIG_FILE = "./conf/client.prop";

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    private String backupFolder;
    private String loginPageURL;
    private final Properties prop;
    private String snClientId;
    private String snClientSecret;
    private String snOAuthEndPoint;
    private String snPassword;
    private int snRefreshLimitInSeconds;
    private String snUsername;
    private String updateSetListURL;
    private String updateSetURL;

    public Configuration() {

	super();

	prop = new Properties();

	LOGGER.log(Level.CONFIG, "loading config file");

	try (FileInputStream fis = new FileInputStream(new File(CONFIG_FILE))) {

	    prop.load(fis);

	} catch (IOException e) {

	    LOGGER.log(Level.SEVERE, e.getMessage());

	    throw new IllegalStateException("Could not read configuration from file " + CONFIG_FILE);

	}

	initialize();

    }

    public String getBackupFolder() {
	return backupFolder;
    }

    public String getLoginPageURL() {
	return loginPageURL;
    }

    public String getSnClientId() {
	return snClientId;
    }

    public String getSnClientSecret() {
	return snClientSecret;
    }

    public String getSnOAuthEndPoint() {
	return snOAuthEndPoint;
    }

    public String getSnPassword() {
	return snPassword;
    }

    public int getSnRefreshLimitInSeconds() {
	return snRefreshLimitInSeconds;
    }

    public String getSnUsername() {
	return snUsername;
    }

    public String getUpdateSetListURL() {
	return updateSetListURL;
    }

    public String getUpdateSetURL() {
	return updateSetURL;
    }

    private void initialize() {

	snOAuthEndPoint = prop.getProperty("snOAuthEndPoint");
	snClientSecret = prop.getProperty("snClientSecret");
	snClientId = prop.getProperty("snClientId");
	snUsername = prop.getProperty("snUsername");
	snPassword = prop.getProperty("snPassword");
	snRefreshLimitInSeconds = Integer.parseInt(prop.getProperty("snRefreshLimitInSeconds"));

	updateSetListURL = prop.getProperty("updateSetListURL");
	updateSetURL = prop.getProperty("updateSetURL");
	loginPageURL = prop.getProperty("loginPageURL");
	backupFolder = prop.getProperty("backupFolder");

    }

    public String toString() {
	return new ReflectionToStringBuilder(this).appendSuper(super.toString()).toString();
    }

}
