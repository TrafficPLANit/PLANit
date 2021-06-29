package org.planit.project;

import java.util.TreeMap;

import org.planit.service.routed.RoutedServices;
import org.planit.utils.wrapper.LongMapWrapper;

/**
 * class for registered routed services on project
 *
 */
public class ProjectRoutedServices extends LongMapWrapper<RoutedServices> {

  /**
   * Constructor
   */
  protected ProjectRoutedServices() {
    super(new TreeMap<Long, RoutedServices>(), RoutedServices::getId);
  }

  /**
   * Collect the first registered entry (if any). Otherwise return null
   * 
   * @return first entry registered if none return null
   */
  public RoutedServices getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

}
