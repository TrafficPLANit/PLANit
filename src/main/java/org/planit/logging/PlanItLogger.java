package org.planit.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class manages the logging for PlanIt projects
 *
 * @author gman6028
 *
 */
public class PlanItLogger {


  /**
   * file handler to use for all loggers
   */
  private static Handler fileHandler;

	/**
	 * Default logging format
	 */
	private static final String DEFAULT_LOG_FORMAT = "%1$tc%n%4$s: %5$s%n%n";

	/**
	 * Adds a file handler to the logging
	 *
	 * Create the log file if it does not already exist
	 *
	 * @param logfileLocation name of the log file to be created
	 * @param formatter Formatter object
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	private static void setHandler(final String logfileLocation, final Formatter formatter) throws SecurityException, IOException {
		final File logFile = new File(logfileLocation);
		if (!logFile.exists()) {
			final String[] locations = logfileLocation.split("\\\\");
			String dirName = "";
			for (int i=0; i<locations.length-1; i++) {
				dirName += locations[i];
			}
			final File dir = new File(dirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			logFile.createNewFile();
		}
		fileHandler  = new FileHandler(logfileLocation);
		if (formatter != null) {
			fileHandler.setFormatter(formatter);
		}
	}
	
	/**
	 * Sets up the logger with simple text formatting and developer-specified format
	 *
	 * @param logfileLocation name of the log file to be created
	 * @param clazz class of the application logging is being done for
	 * @param format, the format for the logging
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	public static void activateFileLogging(final String logfileLocation, final String format) throws SecurityException, IOException {
		activateLoggingToConsole(format);
		setHandler(logfileLocation, new SimpleFormatter());
	}

	/**
	 * Sets up the logger with simple text formatting and default format
	 *
	 * @param logfileLocation name of the log file to be created
	 * @param clazz class of the application logging is being done for
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	public static void activateFileLogging(final String logfileLocation) throws SecurityException, IOException {
		activateFileLogging(logfileLocation, DEFAULT_LOG_FORMAT);
	}

	/**
	 * Set up the logger to output to the console only (no log file created)
	 *
	 * @param format format of the log lines
	 */
	public static void activateLoggingToConsole(final String format) {
		System.setProperty("java.util.logging.SimpleFormatter.format", format);
	}
	
  /**
   * Set up the logger to output to the console only (no log file created)
   *
   */
  public static void activateLoggingToConsole() {
    System.setProperty("java.util.logging.SimpleFormatter.format", DEFAULT_LOG_FORMAT);
  }	

	/**
	 * Sets up the logger with formatting specified by developer
	 *
	 * @param logfileLocation name of the log file to be created
	 * @param clazz class of the application logging is being done for
	 * @param formatter Formatter object
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	public static void activateFileLogging(final String logfileLocation, final Formatter formatter) throws SecurityException, IOException {
		setHandler(logfileLocation, formatter);
	}
	
	/**
	 * create a logger for a given class. If any handlers have been configured on the PLANitLogger, they are attached as well
	 * @param theClass
	 * @return theLogger
	 */
	public static Logger createLogger(Class<?> theClass) {
	  Logger theLogger = Logger.getLogger(theClass.getCanonicalName());
	  if(fileHandler != null) {
	    theLogger.addHandler(fileHandler);
	  }
	  return theLogger;
	}
	
	
	/**
	 * Close the file handler (if any)
	 */
	public static void close() {
	  if(fileHandler != null){
	    fileHandler.close();
	  }
	}

}
