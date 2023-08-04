package org.goplanit.converter;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to convert entities of some type which are then returned
 * 
 * @param <T> reader type
 * 
 * @author markr
 *
 */
public interface ConverterReader<T> extends ConverterEntity {
  
  /** any settings to configure the reader can be configured by collecting these settings
   * @return the settings to configrue the reader
   */
  public abstract ConverterReaderSettings getSettings();  

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed network
   */
  public abstract T read();
   
}
