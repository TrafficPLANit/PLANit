package org.goplanit.network.layer;

import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * A network layer generator generates a (non-empty) network based on its type and or configuration on-the-fly.Unlike a Network reader it is not meant to read anything from disk,
 * but instead generate it based on some pattern of sorts. concrete implementations are meant to provide a quick and easy way to obtain some sample networks for testing.
 * 
 * @author markr
 *
 */
public interface NetworkLayerGenerator {

  /**
   * Generate the network layer
   * 
   * @return
   */
  public abstract UntypedPhysicalLayer<?, ?, ?> generate();
}
