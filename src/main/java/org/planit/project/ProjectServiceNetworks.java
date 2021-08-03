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
   * {@inheritDoc}
   */
  @Override
  public ProjectServiceNetworks clone() {
    return new ProjectServiceNetworks(this);
  }

}
