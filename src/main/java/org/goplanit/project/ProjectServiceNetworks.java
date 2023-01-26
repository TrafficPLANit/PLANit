package org.goplanit.project;

import java.util.TreeMap;

import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered service networks on project
 *
 */
public class ProjectServiceNetworks extends LongMapWrapperImpl<ServiceNetwork> {

  /**
   * Constructor
   */
  protected ProjectServiceNetworks() {
    super(new TreeMap<>(), ServiceNetwork::getId);
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
