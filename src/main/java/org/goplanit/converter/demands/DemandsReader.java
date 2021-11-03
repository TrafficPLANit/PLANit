package org.goplanit.converter.demands;

import org.goplanit.converter.ConverterReader;
import org.goplanit.demands.Demands;

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
