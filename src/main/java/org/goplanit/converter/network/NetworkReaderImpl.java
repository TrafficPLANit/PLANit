package org.goplanit.converter.network;

import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.network.TransportLayerNetwork;

/**
 * A network reader implementation with built-in convenience containers that maps ids used by the external data source to relate entities to each other to the created PLANit
 * entries.
 * 
 * @author markr
 *
 */
public abstract class NetworkReaderImpl extends BaseReaderImpl<TransportLayerNetwork<?, ?>> implements NetworkReader {

  /**
   * Constructor
   */
  protected NetworkReaderImpl() {
    super();
  }
}
