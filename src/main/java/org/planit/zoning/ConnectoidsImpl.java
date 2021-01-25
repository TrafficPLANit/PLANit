package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;

/**
 * Implementation of Connectoids class
 * 
 * @author markr
 *
 */
public class ConnectoidsImpl implements Connectoids {

  /** id generation token */
  protected IdGroupingToken idToken;

  /**
   * connectoids container
   */
  protected Map<Long, Connectoid> connectoidMap = new TreeMap<Long, Connectoid>();

  /**
   * Constructor
   * 
   * @param idToken to use
   */
  public ConnectoidsImpl(IdGroupingToken idToken) {
    this.idToken = idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Connectoid> iterator() {
    return connectoidMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connectoid register(Connectoid connectoid) {
    return connectoidMap.put(connectoid.getId(), connectoid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode, Zone parentZone, double length) throws PlanItException {
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(idToken, accessNode, parentZone, length);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode, Zone parentZone) throws PlanItException {
    return registerNew(accessNode, parentZone, Connectoid.DEFAULT_LENGTH_KM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode) throws PlanItException {
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(idToken, accessNode);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(EdgeSegment accessEdgeSegment, Zone parentZone, double length) throws PlanItException {
    DirectedConnectoid newConnectoid = new DirectedConnectoidImpl(idToken, accessEdgeSegment, parentZone, length);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(EdgeSegment accessEdgeSegment, Zone parentZone) throws PlanItException {
    return registerNew(accessEdgeSegment, parentZone, Connectoid.DEFAULT_LENGTH_KM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(EdgeSegment accessEdgeSegment) throws PlanItException {
    DirectedConnectoid newConnectoid = new DirectedConnectoidImpl(idToken, accessEdgeSegment);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connectoid get(long id) {
    return connectoidMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return connectoidMap.size();
  }

}
