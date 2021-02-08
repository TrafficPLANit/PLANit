package org.planit.converter;

import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to write a PLANit entities of given type to disk
 * 
 * @param <T> writer type
 * @param <U> writer type
 * 
 * @author markr
 *
 */
public interface MultiConverterWriter<T, U> extends ConverterEntity {

  /**
   * write a network to the writer's output format.
   * 
   * @param entity1 to write
   * @param entity2 to write
   * @throws PlanItException
   */
  void write(T entity1, U entity2) throws PlanItException;

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
