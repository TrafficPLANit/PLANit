package org.planit.converter;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.Pair;

/**
 * Interface for classes able to convert entities of multiple types which are then returned
 * 
 * @param <T> reader type
 * @param <U> reader type
 * 
 * @author markr
 *
 */
public interface MultiConverterReader<T,U>  {

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed entities
   * @throws PlanItException thrown if error
   */
  Pair<T,U> read() throws PlanItException;
  
  /** short description (one word capitals) of this reader for logging purposes
   * 
   * @return desciption
   */
  String getTypeDescription();
  

}
