package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.assignment.ltm.sltm.Pas;
import org.goplanit.assignment.ltm.sltm.PasFlowShiftExecutor;
import org.goplanit.assignment.ltm.sltm.StaticLtmBushStrategyBase;
import org.goplanit.assignment.ltm.sltm.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.network.transport.TransportModelNetworkUtils;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.zoning.Zoning;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmStrategyConjugateBush extends StaticLtmBushStrategyBase<ConjugateDestinationBush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmStrategyConjugateBush.class.getCanonicalName());

  /** because the bushes will be created and tracked in conjugate network form, we create a conjugate version of the
   * entire network from which the bushes draw */
  protected final ConjugateTransportModelNetwork conjugateTransportModelNetwork;

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  public StaticLtmStrategyConjugateBush(
          final IdGroupingToken idGroupingToken,
          long assignmentId,
          final TransportModelNetwork transportModelNetwork,
          final StaticLtmSettings settings,
          final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
    this.conjugateTransportModelNetwork = transportModelNetwork.createConjugate(
        TransportModelNetworkUtils.generateDerivedConjugateIdGoupingToken(transportModelNetwork));
    conjugateTransportModelNetwork.logInfo("");
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected ConjugateDestinationBush[] createEmptyBushes(Mode mode) {

    var centroid2ConjugateNodeMapping =
        VirtualNetworkUtils.createCentroidVertexToConjugateNodeMapping(
            conjugateTransportModelNetwork.getVirtualNetwork().getLayer());

    var conjugateNetworkLayer =
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    Zoning zoning = getTransportNetwork().getZoning();
    ConjugateDestinationBush[] conjugateBushes = new ConjugateDestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands(mode);
    for (var destination : zoning.getOdZones()) {
      ConjugateDestinationBush bush = null;
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          /* centroid vertex to which demand will be mapped */
          var destinationCentroidVertex = findCentroidVertex(destination);
          if(destinationCentroidVertex == null){
            LOGGER.severe(String.format("Destination zone (%s) without centroid vertex to connect to network, " +
                "this shouldn't happen", destination.getIdsAsString()));
            continue;
          }

          /* collect conjugate root node for this conjugate destination bush */
          var rootConjugateConnectoidNode =
              centroid2ConjugateNodeMapping.get(destinationCentroidVertex);

          /* register new bush */
          bush = new ConjugateDestinationBush(
              conjugateNetworkLayer.getLayerIdGroupingToken(),
              destinationCentroidVertex,
              rootConjugateConnectoidNode,
              /* all "real" turns as conjugate segment is a turn */
              conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers() );
          conjugateBushes[(int) destination.getOdZoneId()] = bush;
          break;
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
    throw new PlanItRunTimeException("commented out original code --> fix the code to make it usable again");

//    // TODO: we now create this mapping twice, see #createEmptyBushes, not efficient
//    var centroid2ConjugateNodeMapping = conjugateVirtualNetwork.createCentroidToConjugateNodeMapping();
//
//    var destinationCentroidVertex = bush.getRootZoneVertex();
//    var destination = destinationCentroidVertex.getParent().getParentZone();
//    ShortestBushResult allToOneResult = null;
//
//    for (var origin : zoning.getOdZones()) {
//      if (origin.idEquals(destinationCentroidVertex)) {
//        continue;
//      }
//
//      Double currOdDemand = odDemands.getValue(origin, destination);
//      if (currOdDemand != null && currOdDemand > 0) {
//
//        //TODO: not rewritten yet requires use of conjugate dags and conjugate shortest path algorithms based on original network costs
//        //TODO: CONTINUE HERE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
////        /* find all-to-one shortest paths */
////        if (allToOneResult == null) {
////          allToOneResult = shortestBushAlgorithm.executeAllToOne(destination.getCentroid());
////        }
////
////        /* initialise bush with this origin shortest path(s) */
////        var originDag = allToOneResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), origin.getCentroid(), destination.getCentroid());
////        if (originDag.isEmpty()) {
////          LOGGER.severe(String.format("Unable to create bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
////          continue;
////        }
////
////        bush.addOriginDemandPcuH(origin, currOdDemand);
////        initialiseBushForOrigin(bush, origin, currOdDemand, originDag, dummyLabel);
//      }
//    }
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
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushConjugate createNetworkLoading(MultiKeyMap<Object, Movement> segmentPair2MovementMap) {
    throw new PlanItRunTimeException("segmentPair2MovementMap embedded in loading, but should be abstracted out I think - TODO");
    //return new StaticLtmLoadingBushConjugate(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushConjugate getLoading() {
    return (StaticLtmLoadingBushConjugate) super.getLoading();
  }

  /**
   * Based on provided original network link segment costs see if we can update the existing collection of PASs
   *
   * @param mode             to use
   * @param linkSegmentCosts to use
   * @param updateGap        flag
   * @param logAll           flag
   * @return newly created PASs
   */
  @Override
  protected Pair<Collection<Pas>, Collection<Pas>> updateBushPass(
          Mode mode, double[] linkSegmentCosts, boolean updateGap, boolean logAll){
    // TODO: not yet implemented for conjugate, take inspiration from "normal" implementation
    return null;
  }

  /**
   *
   * @return description of this strategy for sLTM
   */
  @Override
  public String getDescription() {
    return "Conjugate destination-based Bush";
  }

}
