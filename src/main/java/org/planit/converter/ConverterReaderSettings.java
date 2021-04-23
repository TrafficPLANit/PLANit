package org.planit.converter;

/**
 * Settings to be derived from this dummy interface for all readers compatible with the converter setup. this ensures
 * a generic way of providing settings for such readers
 * 
 * @author markr
 *
 */
public interface ConverterReaderSettings {
  
  /**
   * be able to reset all settings if needed
   */
  public abstract void reset();

}
