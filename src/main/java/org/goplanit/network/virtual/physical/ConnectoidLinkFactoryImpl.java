package org.goplanit.network.virtual.physical;

import java.util.Collection;
import java.util.function.Supplier;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinkFactory;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;

/**
 * Factory for creating connectoid links on connectoid link container
 * 
 * @author markr
 */
public class ConnectoidLinkFactoryImpl extends GraphEntityFactoryImpl<ConnectoidLink> implements ConnectoidLinkFactory {

  /** producer of all modes known to the network, shared by the links this factory creates */
  private Supplier<? extends Collection<Mode>> allModesProducer;

  /**
   * Constructor
   *
   * @param groupId         to use
   * @param connectoidEdges to use
   */
  protected ConnectoidLinkFactoryImpl(final IdGroupingToken groupId, GraphEntities<ConnectoidLink> connectoidEdges) {
    super(groupId, connectoidEdges);
  }

  /**
   * {@inheritDoc}
   */
  public ConnectoidLink registerNew(CentroidVertex centroidVertex, DirectedVertex nonCentroidVertex, double lengthKm,
      Connectoid connectoid, ZoneConnectoidType connectoidType){
      /* create and register connectoid edge */
    ConnectoidLink newConnectoidLink = new ConnectoidLinkImpl(
            getIdGroupingToken(), centroidVertex, nonCentroidVertex, lengthKm, connectoid, connectoidType,
            allModesProducer);
      getGraphEntities().register(newConnectoidLink);
    return newConnectoidLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAllModesProducer(Supplier<? extends Collection<Mode>> allModesProducer) {
    this.allModesProducer = allModesProducer;
  }
}
