package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.wrapper.MapWrapper;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

/**
 * Partial implementation of the Zones &lt;T&gt; interface
 * 
 * @author markr
 *
 * @param <Z> zone type
 */
public abstract class ZonesImpl<Z extends Zone> extends MapWrapper<Long, Z> implements Zones<Z> {

  /**
   * recreate the mapping such that all the keys used for each zone reflect their internal id. To be called whenever the ids of zones are changed
   */
  protected void updateIdMapping() {
    Map<Long, Z> updatedMap = new HashMap<Long, Z>(size());
    getMap().forEach((oldId, zone) -> updatedMap.put(zone.getId(), zone));
    setMap(updatedMap);
  }

  /**
   * Constructor
   */
  public ZonesImpl() {
    super(new TreeMap<Long, Z>(), Zone::getId);
  }

}
