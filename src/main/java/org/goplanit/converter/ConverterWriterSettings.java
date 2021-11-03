package org.goplanit.converter;

/**
 * Settings to be derived from this dummy interface for all writers compatible with the converter setup. this ensures
 * a generic way of providing settings for such writers
 * 
 * @author markr
 *
 */
public interface ConverterWriterSettings {
  
  /**
   * reset settings after persisting
   */
  public abstract void reset();

}
