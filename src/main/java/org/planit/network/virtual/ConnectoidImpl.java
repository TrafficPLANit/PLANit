package org.planit.network.virtual;

import org.planit.exceptions.PlanItException;
import org.planit.graph.EdgeImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * connectoid connecting a zone to the physical road network, carrying two
 * connectoid segments in one or two directions which may carry additional
 * information for each particular direction of the connectoid.
 *
 * @author markr
 *
 */
public class ConnectoidImpl extends EdgeImpl implements Connectoid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 373775073620741347L;

  /**
   * unique internal identifier
   */
  protected final long connectoidId;

  /**
   * External Id of the connectoid
   */
  protected Object externalId = Long.MIN_VALUE;

  /**
   * Generate connectoid id
   *
   *@param parent for id generation
   *@return id of connectoid
   */
  protected static int generateConnectoidId(Object parent) {
    return IdGenerator.generateId(parent, Connectoid.class);
  }

  // Public

  /**
   * Constructor
   *
   *@param parent for id generation
   *@param centroidA the centroid at one end of the connectoid
   *@param nodeB the node at the other end of the connectoid
   *@param length length of the current connectoid
   *@param externalId externalId of the connectoid (can be null, in which case this has not be set
   *          in the input files)
   *@throws PlanItException thrown if there is an error
   */
  public ConnectoidImpl(Object parent, final Centroid centroidA, final Node nodeB, final double length, final Object externalId)
      throws PlanItException {
    super(parent, centroidA, nodeB, length);
    this.connectoidId = generateConnectoidId(parent);
    setExternalId(externalId);
  }

  /**
   * Constructor
   *
   * @param parent for id generation
   * @param centroidA the centroid at one end of the connectoid
   * @param nodeB the node at the other end of the connectoid
   * @param length length of the current connectoid
   * @throws PlanItException thrown if there is an error
   */
  public ConnectoidImpl(Object parent, final Centroid centroidA, final Node nodeB, final double length) throws PlanItException {
    super(parent, centroidA, nodeB, length);
    this.connectoidId = generateConnectoidId(parent);
  }

  /**
   * Register connectoidSegment.
   *
   * If there already exists a connectoidSegment for that direction it is replaced
   * and returned
   *
   * @param connectoidSegment connectoid segment to be registered
   * @param directionAB direction of travel
   * @return replaced ConnectoidSegment
   * @throws PlanItException
   *           thrown if there is an error
   */
  @Override
  public ConnectoidSegment registerConnectoidSegment(final ConnectoidSegment connectoidSegment,
      final boolean directionAB)
      throws PlanItException {
    return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
  }

  // Getters-Setters

  /**
   *
   * Return the id of this connectoid
   *
   * @return id of this connectoid
   */
  @Override
  public long getConnectoidId() {
    return connectoidId;
  }

  @Override
  public Object getExternalId() {
    return externalId;
  }

  @Override
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }
}
