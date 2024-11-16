package org.goplanit.network.transport;

import org.goplanit.network.ConjugateMacroscopicNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.layer.physical.MovementsImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.*;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
public class ConjugateTransportModelNetwork extends UntypedTransportModelNetwork<ConjugateMacroscopicNetwork,ConjugateVirtualNetwork> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateTransportModelNetwork.class.getCanonicalName());

  /**
   * log info on transport model network assuming it has integrated virtual and physical network it reports
   * on the connectoid edges and segments to do so.
   */
  private void logInfo() {
    getVirtualNetwork().logInfo("");
  }

  /**
   * Holds the reference regular transport model network in non-conjugate form
   */
  protected final TransportModelNetwork<MacroscopicNetwork,VirtualNetwork> referenceTransportModelNetwork;

  /**
   * Create the conjugate physical network and its layers based on the reference network
   *
   * @param token to use for id generation
   * @param macroscopicNetwork reference network
   * @return created conjugate network
   */
  private ConjugateMacroscopicNetwork createConjugatePhysicalNetwork(
          IdGroupingToken token, MacroscopicNetwork macroscopicNetwork) {
    return macroscopicNetwork.createConjugate(token);
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
   * @param referenceTransportModelNetwork the original TransportNetwork
   */
  protected ConjugateTransportModelNetwork(
      TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> referenceTransportModelNetwork) {
    super();
    this.referenceTransportModelNetwork = referenceTransportModelNetwork;

    /* generate conjugate network - generate ids separate from other vertices/edges/segments by providing new token */
    var token = IdGenerator.createIdGroupingToken(
            "Conjugate for network " + getInfrastructureNetwork().getId());

    // create baseline conjugate versions without integrating them yet.
    this.virtualNetwork = createConjugateBaseVirtualNetwork(token);
    this.infrastructureNetwork =
            createConjugatePhysicalNetwork(token, referenceTransportModelNetwork.getInfrastructureNetwork());
    movements = new MovementsImpl(infrastructureNetwork.getIdGroupingToken());
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
    // integrate as we would normally
    // todo check if this works
    logInfo();
    return this;
  }

  /**
   * Remove the edges and edge segments on the vertices of both virtual and physical networks
   *
   * @param resetManagedIds when true rest managed ids for those entities that are reset/cleared, when false do not
   */
  public void removeVirtualNetworkFromPhysicalNetwork(boolean resetManagedIds) {
    for (ConnectoidEdge connectoidEdge : getVirtualNetwork().getLayer().getConnectoidEdges()) {
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
   * @return this transport model network
   */
  @Override
  public ConjugateTransportModelNetwork createConjugate() {
    LOGGER.warning("Unable to create conjugate version of already conjugate ntransport model network " +
            "(not supported yet), providing this conjugate network as result");
    return this;
  }

}
