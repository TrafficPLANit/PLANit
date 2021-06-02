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

  /** Any settings to configure the writer can be configured by collecting these settings
   * 
   * @return the settings to configure the writer
   */
  public abstract ConverterWriterSettings getSettings();  
  
  /**
   * Write a network to the writer's output format.
   * 
   * @param entity1 to write
   * @param entity2 to write
   * @throws PlanItException thrown if error
   */
  void write(T entity1, U entity2) throws PlanItException;

  /**
   * collect the way the ids should be mapped
   * 
   * @return the id mapping choice
   */
  IdMapperType getIdMapperType();

  /**
   * set the way ids should be mapped
   * 
   * @param idMapper to use
   */
  void setIdMapperType(IdMapperType idMapper);

}
