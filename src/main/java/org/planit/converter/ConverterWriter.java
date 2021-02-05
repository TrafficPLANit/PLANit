package org.planit.converter;

import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to write a PLANit entities of given type to disk
 * 
 * @param <T> writer type
 * 
 * @author markr
 *
 */
public interface ConverterWriter<T> {

  /**
   * write a network to the writer's output format.
   * 
   * @param network memory model network to write
   * @throws PlanItException thrown if error
   */
  void write(T network) throws PlanItException;
  
  /** short description (one word capitals) of this reader for logging purposes
   * 
   * @return desciption
   */
  String getTypeDescription();  

  /**
   * collect the way the ids should be mapped
   * 
   * @return the idmapping choice
   */
  IdMapperType getIdMapperType();

  /**
   * set the way ids should be mapped
   * 
   * @param idMapper to use
   */
  void setIdMapperType(IdMapperType idMapper);

}
