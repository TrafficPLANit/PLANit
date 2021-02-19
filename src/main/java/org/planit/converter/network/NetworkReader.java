package org.planit.converter.network;

import org.planit.converter.ConverterReader;
import org.planit.network.InfrastructureNetwork;

/**
 * Interface to read a PLANit network
 * 
 * @author markr
 *
 */
public interface NetworkReader extends ConverterReader<InfrastructureNetwork<?>> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }

}
