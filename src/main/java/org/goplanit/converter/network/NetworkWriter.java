package org.goplanit.converter.network;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.network.TransportLayerNetwork;

/**
 * Interface to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public interface NetworkWriter extends ConverterWriter<TransportLayerNetwork<?,?>> {
  
  /**
   * {@inheritDoc}
   */  
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }  

}
