package org.goplanit.zoning.connectoid;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;

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
  private final TransferConnectoid parentConnectoid;

  /**
   * Explicit allowed access points to an infrastructure layer if used, otherwise all are assumed
   * allowed as long as they are mode compatible
   */
  private TreeMap<Long,EdgeSegment> accessEdgeSegments = new TreeMap<>();

  /**
   * constructor
   *
   * @param parentConnectoid parent
   * @param accessZone to use
   * @param type the type
   */
  protected DirectedConnectoidAccessZoneEntryImpl(
      TransferConnectoid parentConnectoid, Zone accessZone, ZoneConnectoidType type) {
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

    if(other.accessEdgeSegments != null) {
      this.accessEdgeSegments = new TreeMap<>(other.accessEdgeSegments);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isValidOrientation(EdgeSegment accessEdgeSegment) {
    if(!hasAccessLinkSegments()){
      return accessEdgeSegment.anyVertexMatches(v -> v.equals(parentConnectoid.getReferenceVertex()));
    }else {
      var refSegment = getFirstAccessLinkSegment();
      if(refSegment.getDownstreamVertex().equals(parentConnectoid.getReferenceVertex())){
        return true;
      }else if(refSegment.getUpstreamVertex().equals(parentConnectoid.getReferenceVertex())){
        return false;
      }else{
        throw new PlanItRunTimeException("Access link segment inconsistent with connectoid access vertex, " +
            "should not happen");
      }
    }
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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()){
      return false;
    }

    DirectedConnectoidAccessZoneEntryImpl that = (DirectedConnectoidAccessZoneEntryImpl) o;
    return super.equals(o) && Objects.equals(parentConnectoid, that.parentConnectoid) &&
        Objects.equals(accessEdgeSegments, that.accessEdgeSegments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parentConnectoid, accessEdgeSegments);
  }
}
