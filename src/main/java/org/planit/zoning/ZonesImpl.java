package org.planit.zoning;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

/**
 * Partial implementation of the Zones &lt;T&gt; interface
 * 
 * @author markr
 *
 * @param <Z> zone type
 */
public abstract class ZonesImpl<Z extends Zone> implements Zones<Z> {

  /**
   * Map storing all the zones by their row/column in the OD matrix
   */
  protected Map<Long, Z> zoneMap = new TreeMap<Long, Z>();
  
  /**
   * recreate the mapping such that all the keys used for each zone reflect their internal id.
   * To be called whenever the ids of zones are changed
   */
  protected void updateIdMapping() {
    Map<Long, Z> updatedMap = new HashMap<Long, Z>(zoneMap.size());
    zoneMap.forEach((oldId, zone) -> updatedMap.put(zone.getId(), zone));
    zoneMap.clear();
    zoneMap = updatedMap;
  }   

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Z> iterator() {
    return zoneMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Z register(Z zone) {
    return zoneMap.put(zone.getId(), zone);
  }
  
  /**
   * {@inheritDoc}
   */  
  @Override
  public Z remove(Z zone) {
    return zoneMap.remove(zone.getId()); 
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public Z get(long id) {
    return zoneMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return zoneMap.size();
  }
  
  @Override
  public Collection<Z> toCollection() {
    return Collections.unmodifiableCollection(zoneMap.values());
  }  

}
