package org.planit.assignment.ltm.sltm.consumer;

import java.util.Iterator;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.SplittingRateDataComplete;
import org.planit.od.path.OdPaths;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.zoning.OdZone;

/**
 * Consumer to apply whenever we initialise (or continue) a loading where the solution scheme requires tracking all nodes' splitting rates whenever the node is traversed by one or
 * more paths. For a solution scheme with physical queues activated we always track all used nodes (Regardles if they are (initially) blocking) whereas for point queues we only
 * track all used nodes when we move the the sophisticated solution method of Raadsen and Bliemer due to the basic solution scheme being unable to solve. In both cases we require
 * tracking all used nodes' splitting rates in order to be able to perform the iterative sending flow update successfully where sending flows are propagated from node to node
 * without conducting a path based loading, but instead rely on sending flows being passed on from one node to the other via the interconnected links. Therefore, all used nodes'
 * splitting rates must be available in order to be able to do this.
 * 
 * @author markr
 *
 */
public class ActivateSplittingRatesUsedNodesConsumer implements TriConsumer<OdZone, OdZone, Double> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ActivateSplittingRatesUsedNodesConsumer.class.getCanonicalName());

  /**
   * Splitting rate data to initialise based on activated used (e.g. non zero demand path passing by) nodes
   */
  private final SplittingRateDataComplete splittingRateData;

  /**
   * PAths to use
   */
  private final OdPaths odPaths;

  /**
   * constructor
   * 
   * @param odPaths           to use
   * @param splittingRateData to use
   */
  public ActivateSplittingRatesUsedNodesConsumer(final SplittingRateDataComplete splittingRateData, final OdPaths odPaths) {
    this.odPaths = odPaths;
    this.splittingRateData = splittingRateData;
  }

  /**
   * Update the turn flow for the path of the given origin,destination,demand combination
   */
  @Override
  public void accept(OdZone origin, OdZone destination, Double odDemand) {
    /* path */
    DirectedPath odPath = odPaths.getValue(origin, destination);
    if (odPath.isEmpty()) {
      LOGGER.warning(String.format("IGNORE: encountered empty path %s", odPath.getXmlId()));
      return;
    }

    Iterator<EdgeSegment> edgeSegmentIter = odPath.iterator();
    // skip first (no entry segments)
    edgeSegmentIter.next();
    while (edgeSegmentIter.hasNext()) {
      this.splittingRateData.activateTrackedNode(edgeSegmentIter.next().getUpstreamVertex());
    }
  }
}
