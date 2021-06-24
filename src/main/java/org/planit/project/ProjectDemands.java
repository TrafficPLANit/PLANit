package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.demands.Demands;

/**
 * class for registered demands on project
 *
 */
public class ProjectDemands {
  
  /**
   * The demands registered on this project
   */
  protected final TreeMap<Long, Demands> demandsMap;  
  
  /**
   * Constructor
   */
  protected ProjectDemands() {
    demandsMap = new TreeMap<Long, Demands>();
  }

  /**
   * Returns a List of demands
   *
   * @return List of demands
   */
  public List<Demands> toList() {
    return new ArrayList<Demands>(demandsMap.values());
  }

  /**
   * Get demands by id
   *
   * @param id the id of the demands
   * @return the retrieved demands
   */
  public Demands get(final long id) {
    return demandsMap.get(id);
  }

  /**
   * Get the number of demands
   *
   * @return the number of demands in the project
   */
  public int size() {
    return demandsMap.size();
  }

  /**
   * Check if demands have already been registered
   *
   * @return true if registered demands exist, false otherwise
   */
  public boolean isEmpty() {
    return !demandsMap.isEmpty();
  }

  /**
   * Collect the first demands that are registered (if any). Otherwise return null
   * 
   * @return first demands that are registered if none return null
   */
  public Demands getFirst() {
    return isEmpty() ? demandsMap.firstEntry().getValue() : null;
  }
  
  /** Register demands
   * 
   * @param demands to register
   */
  public void register(Demands demands) {
    demandsMap.put(demands.getId(), demands);
  }  
}
