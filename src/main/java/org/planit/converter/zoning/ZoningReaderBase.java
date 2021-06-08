package org.planit.converter.zoning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Zone;

/**
 * A zoning reader implementation with built-in convenience containers that maps ids used by the external data source to relate entities to each other
 * to the created PLANit entries. 
 * 
 * @author markr
 *
 */
public abstract class ZoningReaderBase implements ZoningReader {

  /**
   * Map which stores zones by source Id
   */
  protected Map<String, Zone> sourceIdZoneMap;

  /**
   * Map to stores connectoids by source Id
   */
  protected Map<String, Connectoid> sourceIdConnectoidMap; 
  
  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   * 
   * @param <T>      type of object being stored
   * @param sourceId sourceId of object being stored
   * @param obj      object being stored
   * @param map      Map to store the object
   * @return true if this entry is duplicate use of an xml id, false otherwise
   */
  protected static <T> boolean addObjectToSourceIdMap(final String sourceId, final T obj, final Map<String, T> map) {
    boolean containsDuplicates = map.containsKey(sourceId);
    map.put(sourceId, obj);
    return containsDuplicates;
  } 
  
  /**
   * Constructor
   */
  protected ZoningReaderBase(){
    this.sourceIdZoneMap = new HashMap<String, Zone>();
    this.sourceIdConnectoidMap = new HashMap<String, Connectoid>();
  }
  
  /**
   * Stores a zone by its sourceId
   * 
   * @param sourceId sourceId of zone
   * @param zone     zone to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addZoneToSourceIdMap(String sourceId, Zone zone) {
    return addObjectToSourceIdMap(sourceId, zone, sourceIdZoneMap);
  }

  /**
   * Stores a zone by its sourceId
   * 
   * @param sourceId   sourceId of zone
   * @param connectoid connectoid to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addConnectoidToSourceIdMap(String sourceId, Connectoid connectoid) {
    return addObjectToSourceIdMap(sourceId, connectoid, sourceIdConnectoidMap);
  }  
  
  /**
   * Returns the zone for a specified sourceId
   * 
   * @param sourceId the sourceId
   * @return the zone corresponding to this sourceId
   */
  public Zone getZoneBySourceId(String sourceId) {
    return sourceIdZoneMap.get(sourceId);
  }
  
  /**
   * Return all the registered zones
   * 
   * @return collection of registered zones
   */   
  public Map<String, Zone> getAllZonesBySourceId() {
    return Collections.unmodifiableMap(sourceIdZoneMap);
  }    

  /**
   * Returns the connectiod for a specified sourceId
   * 
   * @param sourceId the sourceId
   * @return the connectoid corresponding to this sourceId
   */
  public Zone getConnectoidBySourceId(String sourceId) {
    return sourceIdZoneMap.get(sourceId);
  }  
  
 
}
