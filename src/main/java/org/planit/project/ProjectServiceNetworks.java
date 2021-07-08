package org.planit.project;

import java.util.TreeMap;

import org.planit.network.ServiceNetwork;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered service networks on project
 *
 */
public class ProjectServiceNetworks extends LongMapWrapperImpl<ServiceNetwork> {

  /**
   * Constructor
   */
  protected ProjectServiceNetworks() {
    super(new TreeMap<Long, ServiceNetwork>(), ServiceNetwork::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectServiceNetworks(ProjectServiceNetworks other) {
    super(other);
  }

  /**
   * Collect the first registered entry (if any). Otherwise return null
   * 
   * @return first entry registered if none return null
   */
  public ServiceNetwork getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectServiceNetworks clone() {
    return new ProjectServiceNetworks(this);
  }

}
