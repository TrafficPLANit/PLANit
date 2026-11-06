package org.goplanit.zoning.connectoid;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;

/**
 * Undirected connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, each connection
 * will yield a connectoid edge and two connectoid segments when
 * constructing the transport network internally based on the referenced node
 *
 * @author markr
 *
 */
public class OdConnectoidImpl extends ConnectoidImpl implements OdConnectoid {

  /** generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 373775073620741347L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(OdConnectoidImpl.class.getCanonicalName());

  // Protected

  /** unique id across od connectoids */
  protected long odConnectoidId;

  /**
   * Generate undirected connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of undirected connectoid
   */
  protected static long generateOdConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, OdConnectoid.OD_CONNECTOID_ID_CLASS);
  }

  /**
   * Set the undirected connectoid id
   * 
   * @param odConnectoidId to use
   */
  protected void setOdConnectoidId(long odConnectoidId) {
    this.odConnectoidId = odConnectoidId;
  }

  /**
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessVertex the node in the network (layer) the connectoid connects with
   */
  public OdConnectoidImpl(final IdGroupingToken idToken, final DirectedVertex accessVertex) {
    super(idToken, accessVertex);
    setOdConnectoidId(generateOdConnectoidId(idToken));
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  protected OdConnectoidImpl(final OdConnectoidImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setReferenceVertex(other.getReferenceVertex());
    setOdConnectoidId(other.getOdConnectoidId());
  }

  // Public

  // Getters-Setters

  /**
   * collect the undirected connectoid's unique id
   * 
   * @return undirected connectoid id
   */
  public long getOdConnectoidId() {
    return odConnectoidId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setOdConnectoidId(generateOdConnectoidId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidImpl shallowClone() {
    return new OdConnectoidImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidImpl deepClone() {
    return new OdConnectoidImpl(this, true);
  }

}
