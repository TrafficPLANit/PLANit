package org.goplanit.converter;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.Pair;

/**
 * Interface for classes able to convert entities of multiple types which are then returned
 * 
 * @param <T> reader type
 * @param <U> reader type
 * 
 * @author markr
 *
 */
public interface MultiConverterReader<T, U> extends ConverterEntity {
  
  /** any settings to configure the reader can be configured by collecting these settings
   * @return the settings to configrue the reader
   */
  public abstract ConverterReaderSettings getSettings();  

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed entities
   * @throws PlanItException thrown if error
   */
  Pair<T, U> read() throws PlanItException;

}
