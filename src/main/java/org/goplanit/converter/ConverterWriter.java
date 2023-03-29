package org.goplanit.converter;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to write a PLANit entities of given type to disk
 * 
 * @param <T> writer type
 * 
 * @author markr
 *
 */
public interface ConverterWriter<T> extends ConverterEntity {

  /** any settings to configure the writer can be configured by collecting these settings
   * @return the settings to configure the writer
   */
  public abstract ConverterWriterSettings getSettings();  
  
  /**
   * write a network to the writer's output format.
   * 
   * @param entity entity to write
   * @throws PlanItException thrown if error
   */
  public abstract void write(T entity) throws PlanItException;

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
