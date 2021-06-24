package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.zoning.Zoning;

/**
 * Class for registered zonings on project
 *
 */
public class ProjectZonings {
  
  /**
   * The zonings registered on this project
   */
  protected final TreeMap<Long, Zoning> zoningsMap;  
  
  /**
   * Constructor
   */
  protected ProjectZonings() {
    zoningsMap = new TreeMap<Long,Zoning>();
  }

  /**
   * Returns a List of zoning
   *
   * @return List of zoning
   */
  public List<Zoning> toList() {
    return new ArrayList<Zoning>(zoningsMap.values());
  }

  /**
   * Get zoning by id
   *
   * @param id the id of the zoning
   * @return the retrieved zoning
   */
  public Zoning get(final long id) {
    return zoningsMap.get(id);
  }

  /**
   * Get the number of zonings
   *
   * @return the number of zonings in the project
   */
  public int size() {
    return zoningsMap.size();
  }

  /**
   * Check if zonings have already been registered
   *
   * @return true if registered zonings exist, false otherwise
   */
  public boolean isEmpty() {
    return !zoningsMap.isEmpty();
  }

  /**
   * Collect the first zonings that are registered (if any). Otherwise return null
   * 
   * @return first zonings that are registered if none return null
   */
  public Zoning getFirst() {
    return isEmpty() ? zoningsMap.firstEntry().getValue() : null;
  }

  /** Register a zoning
   * 
   * @param zoning to register
   */
  public void register(Zoning zoning) {
    zoningsMap.put(zoning.getId(),  zoning);    
  }
}
