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
	 * Logger object
	 */
	private static Logger LOGGER;
	
	/**
	 * 
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
	private static void addHandler(String logfileLocation, Formatter formatter) throws SecurityException, IOException {
		File logFile = new File(logfileLocation);
		if (!logFile.exists()) {
			String[] locations = logfileLocation.split("\\\\");
			String dirName = "";
			for (int i=0; i<locations.length-1; i++) {
				dirName += locations[i];
			}
			File dir = new File(dirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			logFile.createNewFile();
		}
		Handler fileHandler  = new FileHandler(logfileLocation);
		if (formatter != null) {
			fileHandler.setFormatter(formatter);
		}
		LOGGER.addHandler(fileHandler);
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
	public static void setLogging(String logfileLocation, Class clazz, String format) throws SecurityException, IOException {
		setLoggingToConsoleOnly(clazz, format);		
		Formatter formatter = new SimpleFormatter();
		addHandler(logfileLocation, formatter);
	}
	
	/**
	 * Sets up the logger with simple text formatting and default format
	 * 
	 * @param logfileLocation name of the log file to be created
	 * @param clazz class of the application logging is being done for
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	public static void setLogging(String logfileLocation, Class clazz) throws SecurityException, IOException {
		setLogging(logfileLocation, clazz, DEFAULT_LOG_FORMAT);
	}
	
	/**
	 * Sets up the logger with XML formatting
	 * 
	 * @param logfileLocation name of the log file to be created
	 * @param clazz class of the application logging is being done for
	 * @throws SecurityException thrown security exception
	 * @throws IOException thrown io exception
	 */
	public static void setLoggingWithXmlFormatting(String logfileLocation, Class clazz) throws SecurityException, IOException {
		LOGGER = Logger.getLogger(clazz.getName());		
		addHandler(logfileLocation, null);
	}
	
	/**
	 * Sets up logger to output to console only with default format (no log file created)
	 * 
	 * @param clazz class of the application logging is being done for
	 */
	public static void setLoggingToConsoleOnly(Class clazz) {
		setLoggingToConsoleOnly(clazz, DEFAULT_LOG_FORMAT);
	}
	
	/**
	 * Set up the logger to output to the console only (no log file created)
	 * 
	 * @param clazz class of the application logging is being done for
	 * @param format format of the log lines
	 */
	public static void setLoggingToConsoleOnly(Class clazz, String format) {
		System.setProperty("java.util.logging.SimpleFormatter.format", format);
		LOGGER = Logger.getLogger(clazz.getName());
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
	public static void setLogging(String logfileLocation, Class clazz, Formatter formatter) throws SecurityException, IOException {
		LOGGER = Logger.getLogger(clazz.getName());
		addHandler(logfileLocation, formatter);
	}

	/**
	 * Set logging to console
	 * 
	 * This is set to true by default.
	 * 
	 * @param outputToConsole true if logging to console required, false otherwise
	 */
	public static void setOutputToConsole(boolean outputToConsole) {
		LOGGER.setUseParentHandlers(outputToConsole);
	}
	
	public static void info(String msg) {
		LOGGER.info(msg);
	}
	
	public static void fine(String msg) {
		LOGGER.fine(msg);
	}
	
	public static void finer(String msg) {
		LOGGER.finer(msg);
	}
	
	public static void finest(String msg) {
		LOGGER.finest(msg);
	}
	
	public static void warning(String msg) {
		LOGGER.warning(msg);
	}
	
	public static void severe(String msg) {
		LOGGER.severe(msg);
	}
	
	public static void config(String msg) {
		LOGGER.config(msg);
	}
	
	public static void close() {
		Handler[] handlers =  LOGGER.getHandlers();
		for (Handler handler : handlers) {
			handler.close();
		}
	}
	
}
