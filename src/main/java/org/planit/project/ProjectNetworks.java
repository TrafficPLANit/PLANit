package org.planit.project;

import java.util.TreeMap;

import org.planit.network.TransportLayerNetwork;
import org.planit.utils.wrapper.LongMapWrapper;

/**
 * Internal class for registered physical networks
 *
 */
public class ProjectNetworks extends LongMapWrapper<TransportLayerNetwork<?, ?>> {

  /**
   * Constructor
   */
  protected ProjectNetworks() {
    super(new TreeMap<Long, TransportLayerNetwork<?, ?>>(), TransportLayerNetwork<?, ?>::getId);
  }

  /**
   * Collect the first network that is registered (if any). Otherwise return null
   * 
   * @return first network that is registered if none return null
   */
  public TransportLayerNetwork<?, ?> getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

}
