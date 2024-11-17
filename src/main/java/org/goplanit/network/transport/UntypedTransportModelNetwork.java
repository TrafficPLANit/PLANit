package org.goplanit.network.transport;

import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.layer.physical.MovementsImpl;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.network.layer.physical.Movements;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.zoning.Zoning;

import java.util.logging.Logger;

/**
 * Untyped version base abstract class for any transport network that is being modeled including both the
 * physical and virtual aspects of it as well as the zoning. It acts as a wrapper unifying the two components
 * during the assignment stage.
 * <p>
 *   It also tracks movements if the user desired to generate those
 * </p>
 * 
 * @author markr
 *
 */
public abstract class UntypedTransportModelNetwork<G extends UntypedPhysicalNetwork<?, ?>, V extends UntypedVirtualNetwork<?>>
    implements TransportModelNetwork<G,V>{

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UntypedTransportModelNetwork.class.getCanonicalName());

  /**
   * log info on transport model network assuming it has integrated virtual and physical network it reports
   * on the connectoid edges and segments to do so.
   */
  private void logInfo() {
    getVirtualNetwork().logInfo("");
    if(!movements.isEmpty()){
      LOGGER.info(String.format("#Movements: %d", getMovements().size()));
    }
  }

  /**
   * Holds the infrastructure road network that is being modelled
   */
  protected G infrastructureNetwork;

  /**
   * Holds the virtual network that is being modelled
   */
  protected V virtualNetwork;

  /**
   * Holds the zoning structure interfacing with the physical network. The zoning's virtual network may differ from
   * the one registered here, e.g., if we have create a conjugate or other alternate representation.
   */
  protected Zoning zoning;

  /**
   * Optional container to fill with all permissible movements in the transport network
   */
  protected Movements movements;

  /**
   * Add Edge to both vertices
   *
   * @param edge Edge to be added to upstream and downstream vertices
   */
  protected void connectVerticesToEdge(Edge edge) {
    edge.getVertexA().addEdge(edge);
    edge.getVertexB().addEdge(edge);
  }

  /**
   * Remove Edge from both vertices
   *
   * @param edge Edge to be removed from upstream and downstream vertices
   */
  protected void disconnectVerticesFromEdge(Edge edge) {
    edge.getVertexA().removeEdge(edge);
    edge.getVertexB().removeEdge(edge);
  }

  /**
   * Constructor assuming caller will populate all members themselves (not recommended)
   *
   */
  protected UntypedTransportModelNetwork() {
    this.infrastructureNetwork = null;
    this.zoning = null;
    this.virtualNetwork = null;
    this.movements = null;
  }

  /**
   * Constructor
   *
   * @param infrastructureNetwork the network used to generate this TransportNetwork
   * @param zoning                the Zoning used to generate this TransportNetwork
   * @param virtualNetwork        to use
   */
  public UntypedTransportModelNetwork(G infrastructureNetwork, Zoning zoning, V virtualNetwork) {
    this.infrastructureNetwork = infrastructureNetwork;
    this.zoning = zoning;
    this.virtualNetwork = virtualNetwork;
    this.movements = new MovementsImpl(infrastructureNetwork.getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeVirtualNetworkFromPhysicalNetwork(boolean resetManagedIds) {
    // todo: move to interface if conjugate implementation has same implementation
    for (ConnectoidDirectedEdge connectoidEdge : getVirtualNetwork().getLayer().getConnectoidLinks()) {
      disconnectVerticesFromEdge(connectoidEdge);
    }

    /* clear out contents */
    if(resetManagedIds){
      getVirtualNetwork().reset();
    }else{
      getVirtualNetwork().clear();
    }
  }

  /**
   * {@inheritDoc}
   * TODO: this does not properly support layers yet, so it is assumed we want movements across all layers without
   */
  public void generatePermissibleMovements() {
    getMovements().reset();
    for(var layer : getInfrastructureNetwork().getTransportLayers()){
      for(var node : layer.getNodes()){
        for(var entrySegment : node.getEntryEdgeSegments()){
          for(var exitSegment : node.getExitLinkSegments()){

            // never allow u-turn movement
            if(entrySegment.hasOppositeDirectionSegment() && entrySegment.getOppositeDirectionSegment() == exitSegment){
              continue;
            }

            getMovements().getFactory().registerNew(entrySegment, exitSegment);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public G getInfrastructureNetwork() {
    return infrastructureNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V getVirtualNetwork() {
    return virtualNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zoning getZoning() {
    return zoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Movements getMovements(){
    return movements;
  }

}
