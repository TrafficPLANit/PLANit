package org.goplanit.converter.service;

import org.goplanit.converter.ConverterReader;
import org.goplanit.converter.ConverterWriter;
import org.goplanit.converter.network.NetworkReader;
import org.goplanit.network.ServiceNetwork;

/**
 * Interface to read a PLANit service network
 * 
 * @author markr
 *
 */
public interface ServiceNetworkReader extends ConverterReader<ServiceNetwork> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "SERVICENETWORK";
  }

}
