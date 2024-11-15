package org.goplanit.network.transport;

import org.goplanit.network.ConjugateMacroscopicNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
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
public class ConjugateTransportModelNetwork implements TransportModelNetwork{

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
  protected final TransportModelNetwork referenceTransportModelNetwork;

  /**
   * Holds the conjugate infrastructure road network that is being modelled
   */
  protected final ConjugateMacroscopicNetwork conjugateInfrastructureNetwork;

  /**
   * Holds the conjugate virtual network that is being modelled
   */
  protected final ConjugateVirtualNetwork conjugateVirtualNetwork;

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
  protected ConjugateTransportModelNetwork(TransportModelNetwork referenceTransportModelNetwork) {
    this.referenceTransportModelNetwork = referenceTransportModelNetwork;

    /* generate conjugate network - generate ids separate from other vertices/edges/segments by providing new token */
    var token = IdGenerator.createIdGroupingToken(
            "Conjugate for network " + getInfrastructureNetwork().getId());

    // create baseline conjugate versions without integrating them yet.

    this.conjugateVirtualNetwork = createConjugateBaseVirtualNetwork(token);
    this.conjugateInfrastructureNetwork =
            createConjugatePhysicalNetwork(token, (MacroscopicNetwork) referenceTransportModelNetwork.getInfrastructureNetwork());

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
   * Collect the physical network component of the transport network
   *
   * @return physicalNetwork
   */
  public ConjugateMacroscopicNetwork getInfrastructureNetwork() {
    return conjugateInfrastructureNetwork;
  }

  /**
   * Collect the conjugate virtual network component of the conjugate transport network
   *
   * @return virtualNetwork
   */
  public ConjugateVirtualNetwork getVirtualNetwork() {
    return this.conjugateVirtualNetwork;
  }

  /**
   * Collect the zoning structure
   *
   * @return zoning
   */
  public Zoning getZoning() {
    return referenceTransportModelNetwork.getZoning();
  }

  /**
   * Create a (new) mapping from zones (transfer and or OD) to their centroid vertex.
   *
   * @param OdZones when true OdZones will be included in the mapping, not included otherwise
   * @param transferZones when true transferZones will be included in the mapping, not included otherwise
   * @return mapping that was created
   */
  public Map<Zone, CentroidVertex> createZoneToCentroidVertexMapping(boolean OdZones, boolean transferZones){
    return getZoning().getVirtualNetwork().getVertices().stream().filter(
        cVertex ->
            (OdZones && (cVertex.getParent().getParentZone() instanceof OdZone)) || (transferZones && (cVertex.getParent().getParentZone() instanceof TransferZone))).collect(
                Collectors.toMap(cVertex -> cVertex.getParent().getParentZone(), cVertex -> cVertex));
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
