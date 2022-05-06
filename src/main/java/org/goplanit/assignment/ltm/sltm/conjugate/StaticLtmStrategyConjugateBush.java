package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.assignment.ltm.sltm.Pas;
import org.goplanit.assignment.ltm.sltm.PasFlowShiftExecutor;
import org.goplanit.assignment.ltm.sltm.StaticLtmBushStrategyBase;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public abstract class StaticLtmStrategyConjugateBush extends StaticLtmBushStrategyBase<ConjugateDestinationBush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmStrategyConjugateBush.class.getCanonicalName());

  /** the conjugate virtual network we base our bushes on */
  private final ConjugateVirtualNetwork conjugateVirtualNetwork;

  /** the conjugate network layer we base our conjugate bushes on */
  private final ConjugateMacroscopicNetworkLayer conjugateNetworkLayer;

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  protected StaticLtmStrategyConjugateBush(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
      final StaticLtmSettings settings, final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    /* generate conjugate network - generate ids separate from other vertices/edges/segments by providing new token */
    var token = IdGenerator.createIdGroupingToken("conjugate for network " + getInfrastructureNetwork().getId());
    
    /* generate conjugate virtual network - generate ids separate from other vertices/edges/segments by providing new token */
    this.conjugateVirtualNetwork = transportModelNetwork.getZoning().getVirtualNetwork().createConjugate(token);
    this.conjugateNetworkLayer = getInfrastructureNetwork().getLayerByMode(getInfrastructureNetwork().getModes().getFirst()).createConjugate(token, conjugateVirtualNetwork);
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   * 
   * @return created empty bushes suitable for this strategy
   */
  protected ConjugateDestinationBush[] createEmptyBushes() {

    // TODO: we now create this mapping twice, see #initialiseBush, not efficient
    var centroid2ConjugateNodeMapping = conjugateVirtualNetwork.createCentroidToConjugateNodeMapping();

    Zoning zoning = getTransportNetwork().getZoning();
    ConjugateDestinationBush[] conjugateBushes = new ConjugateDestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands();
    for (var destination : zoning.getOdZones()) {
      ConjugateDestinationBush bush = null;
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          if (bush == null) {
            /* collect conjugate root nodes for this conjugate destination bush */
            var rootConjugateConnectoidNodes = centroid2ConjugateNodeMapping.get(destination.getCentroid());
            /* register new bush */
            bush = new ConjugateDestinationBush(conjugateNetworkLayer.getLayerIdGroupingToken(), destination, rootConjugateConnectoidNodes,
                conjugateNetworkLayer.getConjugateLinkSegments().size());
            conjugateBushes[(int) destination.getOdZoneId()] = bush;
            break;
          }
        }
      }
    }
    return conjugateBushes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialiseBush(ConjugateDestinationBush bush, Zoning zoning, OdDemands odDemands, ShortestBushGeneralised shortestBushAlgorithm) {
    // TODO: we now create this mapping twice, see #createEmptyBushes, not efficient
    var centroid2ConjugateNodeMapping = conjugateVirtualNetwork.createCentroidToConjugateNodeMapping();

    var destination = bush.getDestination();
    ShortestBushResult allToOneResult = null;

    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        //TODO: not rewritten yet requires use of conjugate dags and conugate shortest path algorithms based on original network costs
//        /* find all-to-one shortest paths */
//        if (allToOneResult == null) {
//          allToOneResult = shortestBushAlgorithm.executeAllToOne(destination.getCentroid());
//        }
//
//        /* initialise bush with this origin shortest path(s) */
//        var originDag = allToOneResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), origin.getCentroid(), destination.getCentroid());
//        if (originDag.isEmpty()) {
//          LOGGER.severe(String.format("Unable to create bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
//          continue;
//        }
//
//        bush.addOriginDemandPcuH(origin, currOdDemand);
//        initialiseBushForOrigin(bush, origin, currOdDemand, originDag, dummyLabel);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings) {
    // TODO: not implemented yet
    return null;
  }

  /**
   * Create conjugate bush based network loading implementation
   * 
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushConjugate createNetworkLoading() {
    return new StaticLtmLoadingBushConjugate(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushConjugate getLoading() {
    return (StaticLtmLoadingBushConjugate) super.getLoading();
  }

}
