package org.planit.converter.network;

import org.planit.converter.ConverterReader;
import org.planit.network.TransportLayerNetwork;

/**
 * Interface to read a PLANit network
 * 
 * @author markr
 *
 */
public interface NetworkReader extends ConverterReader<TransportLayerNetwork<?,?>> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }

}
