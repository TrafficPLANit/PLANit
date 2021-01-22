package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.zoning.Connectoids;
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
  public Connectoid registerNew(Zone parentzone, Node accessNode, double length) throws PlanItException {
    Connectoid newConnectoid = new ConnectoidImpl(idToken, parentzone, accessNode, length);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connectoid registerNew(Zone parentzone, Node accessNode) throws PlanItException {
    // TODO Auto-generated method stub
    return null;
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
