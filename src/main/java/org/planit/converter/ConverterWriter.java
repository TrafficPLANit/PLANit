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
public interface ConverterWriter<T> extends ConverterEntity {

  /**
   * write a network to the writer's output format.
   * 
   * @param network memory model network to write
   * @throws PlanItException thrown if error
   */
  public abstract void write(T network) throws PlanItException;

  /**
   * collect the way the ids should be mapped
   * 
   * @return the idmapping choice
   */
  public abstract IdMapperType getIdMapperType();

  /**
   * set the way ids should be mapped
   * 
   * @param idMapper to use
   */
  public abstract void setIdMapperType(IdMapperType idMapper);

}
