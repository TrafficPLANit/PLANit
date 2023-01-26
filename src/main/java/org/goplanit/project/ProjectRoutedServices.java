package org.goplanit.project;

import java.util.TreeMap;

import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered routed services on project
 *
 */
public class ProjectRoutedServices extends LongMapWrapperImpl<RoutedServices> {

  /**
   * Constructor
   */
  protected ProjectRoutedServices() {
    super(new TreeMap<>(), RoutedServices::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectRoutedServices(ProjectRoutedServices other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectRoutedServices clone() {
    return new ProjectRoutedServices(this);
  }

}
