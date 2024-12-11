package gdaswani.sn.client.tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import gdaswani.sn.client.model.UpdateSetInfo;
import gdaswani.sn.client.model.UpdateSetListResult;
import gdaswani.sn.client.oauth.model.AccessToken;
import gdaswani.sn.client.oauth.model.Configuration;
import gdaswani.sn.client.oauth.sn.SNProvider;

public final class PerformExport implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PerformExport.class.getName());

    private final Configuration config;

    private final SNProvider snProvider;

    public PerformExport() {

	super();

	this.config = new Configuration();
	this.snProvider = new SNProvider(config);

    }

    private void exportAndSaveUpdateSets(List<UpdateSetInfo> infos) throws IOException, InterruptedException {

	ChromeOptions cOptions = new ChromeOptions();

	cOptions.addArguments("--headless=new");

	HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
	chromePrefs.put("profile.default_content_settings.popups", 0);
	chromePrefs.put("download.default_directory", config.getBackupFolder());
	chromePrefs.put("safebrowsing.enabled", false);

	cOptions.setExperimentalOption("prefs", chromePrefs);

	WebDriver webDriver = new ChromeDriver(cOptions);

	LOGGER.log(Level.INFO, "Set WebDriver options");

	try {

	    // this one uses a browser, not REST - sadly, ServiceNow does not export the
	    // platforms management features as APIs

	    webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
	    webDriver.manage().window().maximize();

	    webDriver.get(config.getLoginPageURL());

	    LOGGER.log(Level.FINEST, "webDriver = " + webDriver);

	    // this is the LOGIN FORM

	    WebElement user = webDriver.findElement(By.name("user_name"));
	    user.sendKeys(config.getSnUsername());

	    WebElement password = webDriver.findElement(By.name("user_password"));
	    password.sendKeys(config.getSnPassword());

	    LOGGER.log(Level.INFO, "logging in");

	    webDriver.findElement(By.cssSelector("button.btn-primary")).click();

	    for (UpdateSetInfo info : infos) {

		LOGGER.log(Level.INFO, "exporting, updateSetInfo = " + info);

		// head over to a LOCAL UPDATE SET RECORD

		String updateSetURL = config.getUpdateSetURL().replaceAll("%SYS_ID%", info.getSysId());

		LOGGER.log(Level.FINEST, "opening updateSetURL = " + updateSetURL);

		webDriver.get(updateSetURL);

		// wait for GSFT_MAIN IFRAME

		LOGGER.log(Level.FINEST, "waiting for gsft_main");

		WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("gsft_main"));

		// click on the "Export to XML" UI Action

		LOGGER.log(Level.FINEST, "waiting for 'Export to XML' link");

		wait.until(
			ExpectedConditions.elementToBeClickable(By.cssSelector("a#fb1a56050a0a3c1e01f8b4066aff9aa7")));

		webDriver.findElement(By.cssSelector("a#fb1a56050a0a3c1e01f8b4066aff9aa7")).click();

		String downloadsFolder = config.getBackupFolder();

		// check for download completion

		LOGGER.log(Level.FINEST, "waiting for downloaded file, folder = " + downloadsFolder);

		boolean fileExists = false;
		int retryCount = 0;
		Path matchingFile = null;

		while (!fileExists && retryCount < 10) {

		    Thread.sleep(2500);

		    Object[] matchingDownloads = Files.list(Path.of(downloadsFolder)).filter(p -> {

			String fileName = p.getFileName().toString();

			if (fileName.startsWith("sys_remote_update_set_") && fileName.endsWith(".xml")) {
			    return true;
			} else {
			    return false;
			}

		    }).toArray();

		    fileExists = matchingDownloads.length > 0;

		    if (fileExists) {
			matchingFile = (Path) matchingDownloads[0];
		    } else {
			retryCount++;
		    }
		}

		if (retryCount >= 10) {
		    throw new FileNotFoundException("Failed to download");
		}

		LOGGER.log(Level.FINEST, "fileToRename = " + matchingFile);

		String filteredFNFull = downloadsFolder + info.getName().replaceAll("[\\\\/:*?\"<>|]", "") + ".xml";

		Path newName = Path.of(filteredFNFull);

		Files.move(matchingFile, newName, StandardCopyOption.REPLACE_EXISTING);

		LOGGER.log(Level.FINEST, "fileFinalName = " + newName);

	    }

	    LOGGER.log(Level.INFO, "task completed");

	} finally {
	    webDriver.quit();
	}

    }

    public void run() {

	ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

	AccessToken snAccessToken = snProvider.getAccessToken();

	HttpRequest request = null;

	HttpClient client = HttpClient.newHttpClient();

	try {

	    // this calls the TABLE API to extract a list of recent update sets

	    URI actualEndPoint = null;

	    actualEndPoint = URI.create(config.getUpdateSetListURL());

	    request = HttpRequest.newBuilder().uri(actualEndPoint).header("Content-Type", "application/json")
		    .header("Accept", "application/json")
		    .header("Authorization", snAccessToken.generateAuthorizationValue()).GET().build();

	    LOGGER.log(Level.FINEST, "actualEndPoint = " + actualEndPoint);

	    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

	    int statusCode = response.statusCode();

	    LOGGER.log(Level.FINEST, "statusCode = " + Integer.toString(statusCode));

	    LOGGER.log(Level.FINEST, "body = " + response.body());

	    UpdateSetListResult container = mapper.readValue(response.body(), UpdateSetListResult.class);

	    LOGGER.log(Level.INFO, "retrieved UPDATE SET LIST via table API");

	    if (container.getResult().size() > 0) {
		exportAndSaveUpdateSets(container.getResult());
	    }

	} catch (Throwable t) {
	    LOGGER.log(Level.SEVERE, t.getMessage(), t);
	    throw new IllegalStateException(t);
	}

    }

}
