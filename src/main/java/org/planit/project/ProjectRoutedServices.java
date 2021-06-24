package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.service.routed.RoutedServices;

/**
 * class for registered routed services on project
 *
 */
class ProjectRoutedServices {
  
  /**
   * The routed services registered on this project
   */
  protected final TreeMap<Long, RoutedServices> routedServicesMap;  
  
  /**
   * Constructor
   */
  protected ProjectRoutedServices() {
    routedServicesMap = new TreeMap<Long, RoutedServices>();
  }

  /**
   * Returns a List of routed services
   *
   * @return List of demands
   */
  public List<RoutedServices> toList() {
    return new ArrayList<RoutedServices>(routedServicesMap.values());
  }

  /**
   * Get by id
   *
   * @param id the id of the routed services
   * @return the retrieved routed services
   */
  public RoutedServices get(final long id) {
    return routedServicesMap.get(id);
  }

  /**
   * Get the number of registered entries
   *
   * @return the number of entries
   */
  public int size() {
    return routedServicesMap.size();
  }

  /**
   * Check if any have been registered
   *
   * @return true if registered entries exist, false otherwise
   */
  public boolean isEmpty() {
    return !routedServicesMap.isEmpty();
  }

  /**
   * Collect the first registered entry (if any). Otherwise return null
   * 
   * @return first entry registered if none return null
   */
  public RoutedServices getFirst() {
    return isEmpty() ? routedServicesMap.firstEntry().getValue() : null;
  }
  
  /** Register
   * 
   * @param entry to register
   */
  public void register(RoutedServices routedServices) {
    routedServicesMap.put(routedServices.getId(), routedServices);
  }  
}
