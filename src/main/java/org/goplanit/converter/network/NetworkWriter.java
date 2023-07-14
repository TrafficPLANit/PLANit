package org.goplanit.converter.network;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.network.LayeredNetwork;

/**
 * Interface to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public interface NetworkWriter extends ConverterWriter<LayeredNetwork<?,?>> {
  
  /**
   * {@inheritDoc}
   */  
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }  

}
