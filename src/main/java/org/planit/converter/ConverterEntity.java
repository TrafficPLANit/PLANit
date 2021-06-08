package org.planit.converter;

/**
 * Base interface for classes able to read/write something
 * 
 * 
 * @author markr
 *
 */
public interface ConverterEntity {

  /**
   * short description (one word capitals) of this reader for logging purposes
   * 
   * @return description
   */
  public abstract String getTypeDescription();

  /**
   * reset the reader to allow for marking unnecessary resources for garbage collection
   */
  public abstract void reset();

}
