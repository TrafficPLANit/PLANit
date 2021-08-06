package org.planit.converter.service;

import org.planit.converter.network.NetworkReader;

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
