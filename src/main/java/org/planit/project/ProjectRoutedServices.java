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
   * Collect the first registered entry (if any). Otherwise return null
   * 
   * @return first entry registered if none return null
   */
  public RoutedServices getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectRoutedServices clone() {
    return new ProjectRoutedServices(this);
  }

}
