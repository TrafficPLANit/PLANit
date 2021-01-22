package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

/**
 * Partial implementation of the Zones<T> interface
 * 
 * @author markr
 *
 */
public abstract class ZonesImpl<Z extends Zone> implements Zones<Z> {

  /**
   * Map storing all the zones by their row/column in the OD matrix
   */
  protected final Map<Long, Z> zoneMap = new TreeMap<Long, Z>();

  /** token to use for id generation */
  private IdGroupingToken tokenId;

  /**
   * access to the id generation token
   * 
   * @return id grouping token
   */
  protected IdGroupingToken getGroupingTokenId() {
    return tokenId;
  }

  /**
   * Constructor
   * 
   * @param tokenId
   */
  public ZonesImpl(IdGroupingToken tokenId) {
    this.tokenId = tokenId;
  }

  /**
   * {@index}
   */
  @Override
  public Iterator<Z> iterator() {
    return zoneMap.values().iterator();
  }

  /**
   * {@index}
   */
  @Override
  public Z register(Z zone) {
    return zoneMap.put(zone.getId(), zone);
  }

  /**
   * {@index}
   */
  @Override
  public Z get(long id) {
    return zoneMap.get(id);
  }

  /**
   * {@index}
   */
  @Override
  public int size() {
    return zoneMap.size();
  }

}
