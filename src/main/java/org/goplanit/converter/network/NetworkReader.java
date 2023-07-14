package org.goplanit.converter.network;

import org.goplanit.converter.ConverterReader;
import org.goplanit.network.LayeredNetwork;

/**
 * Interface to read a PLANit network
 * 
 * @author markr
 *
 */
public interface NetworkReader extends ConverterReader<LayeredNetwork<?,?>> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "NETWORK";
  }

}
