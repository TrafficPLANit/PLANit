package org.goplanit.converter;

/**
 * Settings to be derived from this dummy interface for all writers compatible with the converter setup. This ensures
 * a generic way of providing settings for such writers
 * 
 * @author markr
 *
 */
public interface ConverterWriterSettings {
  
  /**
   * Reset settings after persisting
   */
  public abstract void reset();

  /**
   * Log settings
   *
   * @param level level to use
   */
  public abstract void logSettings(int level);

  /**
   * Log settings at level 0
   *
   */
  public default void logSettings(){
    logSettings(0);
  }

}
