package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.*;

/**
 * Factory for creating new undirected connectoids on container
 * 
 * @author markr
 */
public class UndirectedConnectoidFactoryImpl
        extends ManagedIdEntityFactoryImpl<UndirectedConnectoid> implements UndirectedConnectoidFactory {

  /** container to use */
  protected final UndirectedConnectoids undirectedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId               to use
   * @param undirectedConnectoids to use
   */
  protected UndirectedConnectoidFactoryImpl(
          final IdGroupingToken groupId, final UndirectedConnectoids undirectedConnectoids) {
    super(groupId);
    this.undirectedConnectoids = undirectedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Zone accessZone, Node accessNode, double length) {
    UndirectedConnectoid newConnectoid = registerNew(accessNode);
    var entry = newConnectoid.createAccessZoneEntry(accessZone);
    entry.setLengthKm(length);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Zone accessZone, Node accessNode){
    return registerNew(accessZone, accessNode, ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode) {
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(getIdGroupingToken(), accessNode);
    undirectedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

}
