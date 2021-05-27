package org.planit.logging;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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
   * Create logger using default values in logging properties resource file
   * 
   * @param clazz class for which the logger is being created
   * @return the logger for this class
   * @throws Exception thrown if log file cannot be opened
   */
  public static Logger createLogger(Class<?> clazz) throws Exception {
    Logger logger = createLogger(clazz, null);
    return logger;
  }

  /**
   * Create logger
   * 
   * This method is unlikely to be used directly since the value of logFileSystemProperty should
   * never be changed from its default.
   * 
   * @param clazz class for which the logger is being created
   * @param loggingFileName name of logging properties file
   * @return the logger for this class
   * @throws Exception thrown if log file cannot be opened
   */
  public static Logger createLogger(Class<?> clazz, String loggingFileName) throws Exception {
    Logger logger = Logger.getLogger("");
    if (loggingFileName != null) {
      
      /* TODO:
       * markr: no idea what this is doing, condider refactoring along the lines of the else clause that I rewrote */
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
      
      /* using uris we can deal with jar based resources, or simple files */
      URI resourceUri = ResourceUtils.getResourceUri(DEFAULT_LOGGING_PROPERTIES_FILE_NAME);      
      InputStream inputStream = ResourceUtils.getResourceAsInputStream(resourceUri);
            
      LogManager logManager = LogManager.getLogManager();
      logManager.readConfiguration(inputStream);
      logManager.addLogger(logger);
    }

    return logger;
  }

}
