package org.planit.converter.demands;

import org.planit.converter.ConverterWriter;
import org.planit.demands.Demands;

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
