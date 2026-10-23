package org.goplanit.network.virtual.physical;

import org.goplanit.network.layer.physical.LinkImpl;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Connectoid link implementation that represent links that exist between centroids and connectoids
 * (their node reference), so not physical entities but rather virtual links
 * 
 * @author markr
 *
 */
public class ConnectoidLinkImpl extends LinkImpl<DirectedVertex, ConnectoidSegment> implements ConnectoidLink {

  private static final Logger LOGGER = Logger.getLogger(ConnectoidLinkImpl.class.getCanonicalName());

  /** the connectoid this link joins to the network, for the zone of its centroid vertex */
  private final Connectoid connectoid;

  /** the type of the connectoid's access to that zone */
  private final ZoneConnectoidType connectoidType;

  /** producer of all modes known to the network, shared by the links of the factory that created this one */
  private final Supplier<? extends Collection<Mode>> allModesProducer;

  /**
   * The access zone entry of this link: that of its connectoid for the zone of the link's centroid vertex and the
   * link's type
   *
   * @return the access zone entry
   */
  private ConnectoidAccessZoneEntry findAccessZoneEntry() {
    return connectoid.getAccessZoneEntry(getCentroidVertex().getParent().getParentZone(), connectoidType);
  }

  /**
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroidVertex at one end of the connectoid
   * @param vertexB   the vertex at the other end of the connectoid
   * @param length    length of the current connectoid
   * @param connectoid the link joins to the network, for the zone of the centroid vertex
   * @param connectoidType of the connectoid's access to that zone
   * @param allModesProducer producing all modes known to the network, shared with the other links
   */
  protected ConnectoidLinkImpl(final IdGroupingToken groupId, final CentroidVertex centroidA,
      final DirectedVertex vertexB, final double length, final Connectoid connectoid,
      final ZoneConnectoidType connectoidType, final Supplier<? extends Collection<Mode>> allModesProducer) {
    super(groupId, centroidA, vertexB, length);
    this.connectoid = connectoid;
    this.connectoidType = connectoidType;
    this.allModesProducer = allModesProducer;
  }

  /**
   * Copy constructor. The copy refers to the same connectoid in either case, since the zoning owns it
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidLinkImpl(ConnectoidLinkImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.connectoid = other.connectoid;
    this.connectoidType = other.connectoidType;
    this.allModesProducer = other.allModesProducer;
  }

  /**
   * Verify if a mode may use this link: when the access zone entry of this link allows it, that of its connectoid for
   * the zone of the link's centroid vertex and the link's type, see {@link ConnectoidAccessZoneEntry#isModeAllowed}
   *
   * @param mode to verify
   * @return true when allowed, false otherwise
   */
  protected boolean isModeAllowed(Mode mode) {
    return findAccessZoneEntry().isModeAllowed(mode);
  }

  /**
   * The modes that may use this link: those its access zone entry allows out of all modes known to the network, see
   * {@link ConnectoidAccessZoneEntry#getAllowedModesFrom}
   *
   * @return the allowed modes, unmodifiable
   */
  protected Set<Mode> getAllowedModes() {
    return Set.copyOf(findAccessZoneEntry().getAllowedModesFrom(allModesProducer.get()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinkImpl shallowClone() {
    return new ConnectoidLinkImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinkImpl deepClone() {
    return new ConnectoidLinkImpl(this, true);
  }

}
