package org.planit.project;

import java.util.TreeMap;

import org.planit.service.routed.RoutedServices;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered routed services on project
 *
 */
public class ProjectRoutedServices extends LongMapWrapperImpl<RoutedServices> {

  /**
   * Constructor
   */
  protected ProjectRoutedServices() {
    super(new TreeMap<Long, RoutedServices>(), RoutedServices::getId);
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
