package org.goplanit.network.transport;

import org.goplanit.network.ConjugateMacroscopicNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.physical.MovementsImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;

import java.util.logging.Logger;

/**
 * Entire transport network in conjugate form including both the (conjugate) physical and (conjugate) virtual aspects
 * of it as well as the zoning. It acts as a wrapper unifying the two components during the assignment stage.
 * <p>
 *   It is built on an existing transport model network which it keeps internally as a reference currently
 * </p>
 * 
 * @author markr
 *
 */
public class ConjugateTransportModelNetwork
    extends UntypedTransportModelNetwork<ConjugateMacroscopicNetwork,ConjugateVirtualNetwork> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateTransportModelNetwork.class.getCanonicalName());

  /**
   * Holds the reference regular transport model network in non-conjugate form
   */
  protected final TransportModelNetwork<MacroscopicNetwork,VirtualNetwork> referenceTransportModelNetwork;

  /**
   * Create the conjugate physical network and its layers based on the reference network
   * <p>
   *   When conjugate virtual network is provided, the conjugate virtual network is directly integrated/connected to
   *   the conjugate physical network based on the connections present in the original physical and virtual networks
   * </p>
   *
   * @param token to use for id generation
   * @param macroscopicNetwork reference network to based conjugate version on
   * @param conjugateVirtualNetwork (optional) when present, integrate conjugate physical network with virtual network
   * @return created conjugate network
   */
  private ConjugateMacroscopicNetwork createConjugatePhysicalNetwork(
          IdGroupingToken token,
          MacroscopicNetwork macroscopicNetwork,
          ConjugateVirtualNetwork conjugateVirtualNetwork) {
    return macroscopicNetwork.createConjugate(token, conjugateVirtualNetwork);
  }

  /**
   * create the conjugate virtual network, but do not create connectoid edges yet as this is to be
   * dealt with when integration with the physical network is explicitly invoked
   *
   * @param token to use
   */
  private ConjugateVirtualNetwork createConjugateBaseVirtualNetwork(IdGroupingToken token) {
    var referenceVirtualNetwork = referenceTransportModelNetwork.getZoning().getVirtualNetwork();
    /* generate conjugate virtual network - generate ids separate from other vertices/edges/segments by providing new token */
    return referenceVirtualNetwork.createConjugate(token);
  }

  /**
   * Constructor
   *
   * @param idToken to use
   * @param referenceTransportModelNetwork the original TransportNetwork
   */
  protected ConjugateTransportModelNetwork(final IdGroupingToken idToken,
      TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> referenceTransportModelNetwork) {
    super();
    this.movements = new MovementsImpl(idToken);
    this.referenceTransportModelNetwork = referenceTransportModelNetwork;

    // create baseline conjugate versions without integrating them yet.
    this.virtualNetwork = createConjugateBaseVirtualNetwork(idToken);
    // provide base conjugate virtual network so integration is automatic while constructing the physical
    // conjugate network
    this.infrastructureNetwork =
            createConjugatePhysicalNetwork(
                idToken, referenceTransportModelNetwork.getInfrastructureNetwork(), virtualNetwork);
  }

  /**
   * Create conjugate network and virtual network and then integrate as normal to create the conjugate transport model
   * network.
   *
   * @param resetAndRecreateManagedIds when true, reset and then recreate all internal managed ids of transport model network components (links, nodes, connectoids etc.), when false do not.
   * @return the final network (this)
   */
  @Override
  public ConjugateTransportModelNetwork integrateTransportNetworkViaConnectoids(boolean resetAndRecreateManagedIds){
    // todo: would be better to provide general code for integrating the two making the code in the conjugate
    //  physical network more general so it can be reused here. For now we accept we simply do not support this yet
    LOGGER.warning("method integrateTransportNetworkViaConnectoids is not supported yet, instead it is expected that " +
        "integration occurred upon creation by supplying the conjugate virtual network to the conjugate physical " +
        "network when creating the physical conjugate network, ignore call");
    return this;
  }

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   *
   * @param resetManagedIds when true rest managed ids for those entities that are reset/cleared, when false do not
   */
  public void removeVirtualNetworkFromPhysicalNetwork(boolean resetManagedIds) {
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
   * Not possible when already a conjugate network, so return itself and log user warning
   *
   * @param idToken to use
   * @return this transport model network
   */
  @Override
  public ConjugateTransportModelNetwork createConjugate(final IdGroupingToken idToken) {
    LOGGER.warning("Unable to create conjugate version of already conjugate ntransport model network " +
            "(not supported yet), providing this conjugate network as result");
    return this;
  }

}
