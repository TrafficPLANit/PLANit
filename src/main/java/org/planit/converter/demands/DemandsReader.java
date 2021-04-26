package org.planit.converter.demands;

import org.planit.converter.ConverterReader;
import org.planit.demands.Demands;

/**
 * Interface to read a PLANit demands
 * 
 * @author markr
 *
 */
public interface DemandsReader extends ConverterReader<Demands> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "DEMANDS";
  }

}
