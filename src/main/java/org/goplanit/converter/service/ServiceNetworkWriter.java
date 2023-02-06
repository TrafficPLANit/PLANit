package org.goplanit.converter.service;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.ServiceNetwork;

/**
 * Interface to write a PLANit service network to disk
 * 
 * @author markr
 *
 */
public interface ServiceNetworkWriter extends ConverterWriter<ServiceNetwork> {
  
  /**
   * {@inheritDoc}
   */  
  @Override
  default String getTypeDescription() {
    return "SERVICE NETWORK";
  }  

}
