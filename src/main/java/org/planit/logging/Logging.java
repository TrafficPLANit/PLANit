package org.planit.logging;

import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;

/**
 * Utility class to close the current logger
 * 
 * @author gman6028
 *
 */
public class Logging {
  
  public static final String LOG_FILE_SYSTEM_PROPERTY = "java.util.logging.config.file";
  public static final String DEFAULT_LOGGING_PROPERTIES_FILE_NAME = "logging.properties";
  
  
/**
 * Close the current logger
 *
 * @param logger the logger to be closed
 */
  public static void closeLogger(Logger logger) {
      Handler [] handlers = logger.getHandlers();
      for (Handler handler : handlers) {
        handler.close();
      }
    }

  /**
   * Create logger using default values for logging properties file location and log file system property
   * 
   * @param clazz  class for which the logger is being created
   * @return the logger for this class
   * @throws PlanItException thrown if the logging.properties file cannot be found
   */
  public static Logger createLogger(Class<?> clazz) throws PlanItException {
    return createLogger(clazz, DEFAULT_LOGGING_PROPERTIES_FILE_NAME, LOG_FILE_SYSTEM_PROPERTY);
  }
  
  /**
   * Create logger using default value for log file system property
   * 
   * @param clazz  class for which the logger is being created
   * @param loggingPropertiesFileName name of logging properties file
   * @return the logger for this class
   * @throws PlanItException thrown if the logging.properties file cannot be found
   */
  public static Logger createLogger(Class<?> clazz, String loggingPropertiesFileName) throws PlanItException {
    return createLogger(clazz, loggingPropertiesFileName, LOG_FILE_SYSTEM_PROPERTY);
  }
  
  /**
   * Create logger 
   * 
   * This method is unlikely to be used directly since the value of logFileSystemProperty should never be changed from its default.
   * 
   * @param clazz  class for which the logger is being created
   * @param loggingPropertiesFileName name of logging properties file
   * @param logFileSystemProperty the system property to be used for logging
   * @return the logger for this class
   * @throws PlanItException thrown if the logging.properties file cannot be found
   */
  public static Logger createLogger(Class<?> clazz, String loggingPropertiesFileName, String logFileSystemProperty) throws PlanItException {
    ClassLoader classLoader = clazz.getClassLoader();
    URL url = classLoader.getResource(loggingPropertiesFileName);
    if (url == null) {
      throw new PlanItException("No logging properties file could be found at location " + loggingPropertiesFileName);
    }
    String path = url.getFile();
    System.setProperty(logFileSystemProperty, path);
    return Logger.getLogger(clazz.getName());
  }
  
}