package org.goplanit.converter.service;

import org.goplanit.converter.network.NetworkReader;

/**
 * Interface to read a PLANit service network
 * 
 * @author markr
 *
 */
public interface ServiceNetworkReader extends NetworkReader {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "SERVICENETWORK";
  }

}
