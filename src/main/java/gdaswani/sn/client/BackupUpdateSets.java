package gdaswani.sn.client;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gdaswani.sn.client.oauth.model.ExitCode;
import gdaswani.sn.client.tasks.PerformExport;

public class BackupUpdateSets {

	public static void main(final String[] args) {

		int status = ExitCode.EXIT_CODE_FAILURE;

		Logger logger = null;

		try {

			LogManager.getLogManager().readConfiguration(new FileInputStream("conf/logging.properties"));

			logger = Logger.getLogger(BackupUpdateSets.class.getName());

			logger.log(Level.INFO, "starting up");

			new PerformExport().run();

			status = ExitCode.EXIT_CODE_SUCCESS;

		} catch (Throwable e) {

			e.printStackTrace();

			if (logger != null) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
			
		} finally {
			if (logger != null) {
				logger.log(Level.INFO, "ending, statusCode = " + status);
			}
		}

		System.exit(status);

	}

}
