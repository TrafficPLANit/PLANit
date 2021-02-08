package org.planit.converter;

import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to convert entities of some type which are then returned
 * 
 * @param <T> reader type
 * 
 * @author markr
 *
 */
public interface ConverterReader<T> extends ConverterEntity {

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed network
   * @throws PlanItException thrown if error
   */
  public abstract T read() throws PlanItException;

}
