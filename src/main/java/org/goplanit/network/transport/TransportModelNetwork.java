package org.goplanit.network.transport;

import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.zoning.Zoning;

/**
 * Entire transport network that is being modeled including both the physical and virtual aspects of it as well as the zoning.
 * It acts as a wrapper unifying the two components during the assignment stage.
 * <p>
 *   It also tracks movements if the user desired to generate those
 * </p>
 * 
 * @author markr
 *
 */
public interface TransportModelNetwork<G extends UntypedPhysicalNetwork<?, ?>, V extends UntypedVirtualNetwork<?>> {

  // Public

  /**
   * Integrate physical and virtual links within od zones (undirected connectoid access node and centroid).
   * One may want to recreate all managed ids when there is a possibility this is not the first and only call to
   * this method. This to ensure they are contiguous and start at zero if the transport model network is used for
   * assignment that relies on a contiguous numbering. If network is only used once, it can be ignored (set to false)
   *
   * @param resetAndRecreateManagedIds when true, reset and then recreate all internal managed ids of transport
   *                                   model network components (links, nodes, connectoids etc.), when false do not.
   * @return return transport model network integration was performed on to allow chaining
   */
  public abstract TransportModelNetwork<G,V> integrateTransportNetworkViaConnectoids(boolean resetAndRecreateManagedIds);

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   *
   * @param resetManagedIds when true rest managed ids for those entities that are reset/cleared, when false do not
   */
  public void removeVirtualNetworkFromPhysicalNetwork(boolean resetManagedIds);

  /**
   * Returns the total number of edge segments available in this traffic assignment by combining the physical and
   * non-physical link segments
   *
   * @return total number of physical and virtual edge segments
   */
  public default int getNumberOfEdgeSegmentsAllLayers(){
    return getNumberOfPhysicalLinkSegmentsAllLayers() + getNumberOfConnectoidSegments();
  }

  /**
   * Returns the total number of link segments available in this transport network across all eligible layers
   * 
   * @return the number of physical link segments in this network
   */
  public default int getNumberOfPhysicalLinkSegmentsAllLayers(){
    return TransportModelNetworkUtils.getNumberOfPhysicalLinkSegmentsAllLayers(getInfrastructureNetwork());
  }

  /**
   * Returns the total number of connectoid segments available in this transport network
   * 
   * @return the number of connectoid segments in this network
   */
  public default int getNumberOfConnectoidSegments(){
    return TransportModelNetworkUtils.getNumberOfConnectoidSegments(getVirtualNetwork());
  }

  /**
   * Returns the total physical vertices and centroid vertices (of od and/or transfer zones) in this transport network
   * 
   * @return the total number of vertices
   */
  public default int getNumberOfVerticesAllLayers(){
    return TransportModelNetworkUtils.getNumberOfVerticesAllLayers(
            getInfrastructureNetwork(), getVirtualNetwork());
  }

  /**
   * Returns combined raw array of all vertices in the transport network indexed by their internal id
   *
   * @return raw vertex array
   */
  public default DirectedVertex[] createIdIndexedVerticesAllLayers(){
    return TransportModelNetworkUtils.createIdIndexedVerticesAllLayers(
            getInfrastructureNetwork(), getVirtualNetwork());
  }

  /**
   * Returns the total number of physical nodes available in this transport network across all eligible layers
   *
   * @return the number of physical nodes in this network
   */
  public default int getNumberOfPhysicalNodesAllLayers(){
    return TransportModelNetworkUtils.getNumberOfPhysicalNodesAllLayers(getInfrastructureNetwork());
  }

  /**
   * log info on transport model network
   *
   * @param prefix to use for logging
   */
  public abstract void logInfo(String prefix);

  /**
   * Collect the physical network component of the transport network
   * 
   * @return physicalNetwork
   */
  public abstract G getInfrastructureNetwork();

  /**
   * Collect the virtual network component of the transport network
   * 
   * @return virtualNetwork
   */
  public abstract V getVirtualNetwork();

  /**
   * Collect the zoning structure
   * 
   * @return zoning
   */
  public abstract Zoning getZoning();

  /**
   * Retrieve conjugate version of this transport model network
   *
   * @param idToken to use for conjugate network id generation
   * @return conjugate transport model network
   */
  public abstract ConjugateTransportModelNetwork createConjugate(final IdGroupingToken idToken);
}
