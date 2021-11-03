package org.goplanit.converter.network;

import org.goplanit.converter.ConverterReader;
import org.goplanit.network.TransportLayerNetwork;

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
