package org.goplanit.zoning;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Stores access properties for each zone for a given connectoid
 *
 * @author markr
 *
 */
public class DirectedConnectoidAccessZoneEntryImpl extends ConnectoidAccessZoneEntryImpl
    implements DirectedConnectoidAccessZoneEntry {

  private static final Logger LOGGER =
      Logger.getLogger(DirectedConnectoidAccessZoneEntryImpl.class.getCanonicalName());

  /** required to determine consistency of orientation of edge segments w.r.t. access node */
  private final DirectedConnectoid parentConnectoid;

  /**
   * the access point to an infrastructure layer
   */
  private TreeMap<Long,EdgeSegment> accessEdgeSegments = new TreeMap<>();

  /**
   * Check if orientation of edge segment conforms to connectoid access vertex set and existing
   * access segments
   * @param accessEdgeSegment to check
   * @return true when valid, false otherwise
   */
  private boolean isValidOrientation(EdgeSegment accessEdgeSegment) {
    if(!hasAccessLinkSegments()){
      return accessEdgeSegment.anyVertexMatches(v -> v.equals(parentConnectoid.getAccessVertex()));
    }else {
      var refSegment = getFirstAccessLinkSegment();
      if(refSegment.getDownstreamVertex().equals(parentConnectoid.getAccessVertex())){
        return true;
      }else if(refSegment.getUpstreamVertex().equals(parentConnectoid.getAccessVertex())){
        return false;
      }else{
        throw new PlanItRunTimeException("access link segment inconsistent with connectoid access vertex, " +
            "should not happen");
      }
    }
  }

  /**
   * constructor
   *
   * @param parentConnectoid parent
   * @param accessZone to use
   * @param type the type
   */
  protected DirectedConnectoidAccessZoneEntryImpl(
      DirectedConnectoid parentConnectoid, Zone accessZone, ZoneConnectoidType type) {
    super(accessZone, type);
    this.parentConnectoid = parentConnectoid;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  public DirectedConnectoidAccessZoneEntryImpl(DirectedConnectoidAccessZoneEntryImpl other) {
    super(other);
    this.parentConnectoid = other.parentConnectoid;

    this.accessEdgeSegments = new TreeMap<>(other.accessEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addAccessLinkSegment(EdgeSegment accessEdgeSegment) {
    if(!isValidOrientation(accessEdgeSegment)){
      LOGGER.warning(String.format("Unable to add access segment (%s) to directed connectoid (%s), " +
              "it is inconsistent in orientation compared to other access segments",
          accessEdgeSegment.getIdsAsString(), parentConnectoid.getIdsAsString()));
      return false;
    }
    var old = accessEdgeSegments.put(accessEdgeSegment.getId(), accessEdgeSegment);
    return old != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeAccessLinkSegment(EdgeSegment accessEdgeSegment) {
    return accessEdgeSegments.remove(accessEdgeSegment.getId()) !=null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<? extends EdgeSegment> getAccessLinkSegments() {
    return Collections.unmodifiableCollection(accessEdgeSegments.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidAccessZoneEntryImpl shallowClone() {
    return new DirectedConnectoidAccessZoneEntryImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidAccessZoneEntryImpl deepClone() {
    return shallowClone();
  }

}
