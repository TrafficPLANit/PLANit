package org.planit.logging;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.planit.utils.resource.ResourceUtils;

/**
 * Utility class to close the current logger
 * 
 * @author gman6028
 *
 */
public class Logging {

  private static final String DEFAULT_LOGGING_PROPERTIES_FILE_NAME = "logging.properties";
  private static final String LEVEL_PROPERTY = "java.util.logging.FileHandler.level";
  private static final Level DEFAULT_LEVEL = Level.INFO;
    
  /**
   * Close the current logger
   *
   * @param logger the logger to be closed
   */
  public static void closeLogger(Logger logger) {
    Handler[] handlers = logger.getHandlers();
    for (Handler handler : handlers) {
      handler.close();
    }
  }

  /**
   * Create logger using configuration. If no configuration exists, it is attempted to be reader from the
   * available resources. If it does not exist, null is returned. Otherwise the logger is returned based
   * on the configuration. Use this to inialise a PLANit application with a particular logging configuration
   * and the ability to verify this has worked using the default configuration properties by checking for null
   * 
   * @param clazz class for which the logger is being created
   * @return the logger for this class
   * @throws Exception thrown if log file cannot be opened
   */
  public static Logger createLogger(Class<?> clazz) throws Exception {
    Optional<Logger> logger = createLogger(clazz, null);
    if(logger.isEmpty()) {
      System.out.println(String.format("Unable to create logger for class %s",clazz.getName()));
      return null;
    }
    return logger.get();
  }

  /**
   * Create logger
   * 
   * This method is unlikely to be used directly since the value of logFileSystemProperty should
   * never be changed from its default.
   * 
   * @param clazz class for which the logger is being created
   * @param loggingFileName name of logging properties file
   * @return the logger for this class, null if not able to configure based on properties file
   * @throws Exception thrown if log file cannot be opened
   */
  public static Optional<Logger> createLogger(Class<?> clazz, String loggingFileName) throws Exception {
    Logger logger = Logger.getLogger("");
    if (loggingFileName != null) {
      
      /* TODO:
       * markr: no idea what this is doing, consider refactoring along the lines of the else clause that I rewrote */
      Handler handler = new FileHandler(loggingFileName);
      Formatter formatter = new SimpleFormatter();
      handler.setFormatter(formatter);
      Properties p = new Properties();
      URL url = ClassLoader.getSystemResource(DEFAULT_LOGGING_PROPERTIES_FILE_NAME);
      if (url != null) {
        p.load(url.openStream());
      }
      Level level = null;
      if (p.containsKey(LEVEL_PROPERTY)) {
        level = Level.parse(p.getProperty(LEVEL_PROPERTY));
      } else {
        level = DEFAULT_LEVEL;
      }
      handler.setLevel(level);
      logger.addHandler(handler);
    } else {
      
      LogManager logManager = LogManager.getLogManager();
      
      /* using uris we can deal with jar based resources, or simple files */
      URI resourceUri = ResourceUtils.getResourceUri(DEFAULT_LOGGING_PROPERTIES_FILE_NAME);
      if(resourceUri==null) {
        return Optional.empty();
      }
      InputStream inputStream = ResourceUtils.getResourceAsInputStream(resourceUri);                  
			
														 
      logManager.readConfiguration(inputStream);
      logManager.addLogger(logger);
    }

    return Optional.of(logger);
  }

}
