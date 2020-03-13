package org.planit.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Utility class to close the current logger
 * 
 * @author gman6028
 *
 */
public class Logging {
  
  public static final String LOG_FILE_SYSTEM_PROPERTY = "java.util.logging.config.file";
  public static final String DEFAULT_LOGGING_PROPERTIES_FILE_NAME = "logging.properties";
  private static String logFileLocation;
  
  
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
   * @throws IOException 
   */
  public static Logger createLogger(Class<?> clazz) {
    return createLogger(clazz, DEFAULT_LOGGING_PROPERTIES_FILE_NAME, LOG_FILE_SYSTEM_PROPERTY);
  }
  
  /**
   * Create logger using default value for log file system property
   * 
   * @param clazz  class for which the logger is being created
   * @param loggingPropertiesFileName name of logging properties file
   * @return the logger for this class
   * @throws IOException 
   */
  public static Logger createLogger(Class<?> clazz, String loggingPropertiesFileName) {
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
   * @throws IOException 
   */
  public static Logger createLogger(Class<?> clazz, String loggingPropertiesFileName, String logFileSystemProperty) {
    String path = clazz.getClassLoader().getResource(loggingPropertiesFileName).getFile();
    System.setProperty(logFileSystemProperty, path);
    return Logger.getLogger(clazz.getName());
  }
  
}