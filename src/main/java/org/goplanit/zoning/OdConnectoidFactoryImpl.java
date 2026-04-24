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
public class OdConnectoidFactoryImpl
        extends ManagedIdEntityFactoryImpl<OdConnectoid> implements OdConnectoidFactory {

  /** container to use */
  protected final OdConnectoids undirectedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId               to use
   * @param undirectedConnectoids to use
   */
  protected OdConnectoidFactoryImpl(
          final IdGroupingToken groupId, final OdConnectoids undirectedConnectoids) {
    super(groupId);
    this.undirectedConnectoids = undirectedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoid registerNew(Node accessNode) {
    OdConnectoid newConnectoid = new OdConnectoidImpl(getIdGroupingToken(), accessNode);
    undirectedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

}
