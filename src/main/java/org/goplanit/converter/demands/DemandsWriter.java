package org.goplanit.converter.demands;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.demands.Demands;

/**
 * Interface to write a PLANit demands to disk
 * 
 * @author markr
 *
 */
public interface DemandsWriter extends ConverterWriter<Demands> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "DEMANDS";
  }

}
