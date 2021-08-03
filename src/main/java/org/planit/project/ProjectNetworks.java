package org.planit.project;

import java.util.TreeMap;

import org.planit.network.TransportLayerNetwork;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Internal class for registered physical networks
 *
 */
public class ProjectNetworks extends LongMapWrapperImpl<TransportLayerNetwork<?, ?>> {

  /**
   * Constructor
   */
  protected ProjectNetworks() {
    super(new TreeMap<Long, TransportLayerNetwork<?, ?>>(), TransportLayerNetwork<?, ?>::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectNetworks(ProjectNetworks other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectNetworks clone() {
    return new ProjectNetworks(this);
  }

}
