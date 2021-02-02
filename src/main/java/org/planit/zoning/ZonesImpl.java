package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGroupingToken;
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
   * @param tokenId to use for id generation
   */
  public ZonesImpl(IdGroupingToken tokenId) {
    this.tokenId = tokenId;
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

}
