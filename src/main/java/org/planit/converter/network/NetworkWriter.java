package org.planit.converter.network;

import org.planit.converter.ConverterWriter;
import org.planit.network.InfrastructureNetwork;

/**
 * Interface to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public interface NetworkWriter extends ConverterWriter<InfrastructureNetwork<?>> {
  
  /**
   * {@inheritDoc}
   */  
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }  

}
