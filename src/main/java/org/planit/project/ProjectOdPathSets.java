package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.path.OdPathSets;

/**
 * class for registered od path sets
 *
 */
public class ProjectOdPathSets {
  
  /**
   * The od path sets registered on this project
   */
  protected final TreeMap<Long, OdPathSets> odPathSetsMap;  
  
  /**
   * Constructor
   */
  protected ProjectOdPathSets() {
    this.odPathSetsMap = new TreeMap<Long, OdPathSets>();
  }

  /**
   * Returns a List of od path sets
   *
   * @return List of od path sets
   */
  public List<OdPathSets> toList() {
    return new ArrayList<OdPathSets>(odPathSetsMap.values());
  }

  /**
   * Get od path sets by id
   *
   * @param id the id of the link
   * @return the retrieved link
   */
  public OdPathSets get(final long id) {
    return odPathSetsMap.get(id);
  }

  /**
   * Get the number of od path sets
   *
   * @return the number of od path sets in the project
   */
  public int size() {
    return odPathSetsMap.size();
  }

  /**
   * Check if od path sets have already been registered
   *
   * @return true if registered od rotue sets exist, false otherwise
   */
  public boolean isEmpty() {
    return !odPathSetsMap.isEmpty();
  }

  /**
   * Collect the first od path set that is registered (if any). Otherwise return null
   * 
   * @return first od path set that is registered if none return null
   */
  public OdPathSets getFirst() {
    return isEmpty() ? odPathSetsMap.firstEntry().getValue() : null;
  }

  /**
   * Register an OD path sets
   * 
   * @param odPathSets to register
   */
  public void register(OdPathSets odPathSets) {
    odPathSetsMap.put(odPathSets.getId(), odPathSets);
  }
}
