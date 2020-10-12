package org.planit.assignment.algorithmb;

import java.util.Set;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.OutputManager;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.virtual.Zone;

/**
 * All the functionality to equilibrate algorithm B
 * 
 * @author markr
 *
 */
public class AlgorithmBEquilibration {

  /** simulation data for Algorithm B */
  private final AlgorithmBSimulationData simulationData;

  /** assignment configuration information */
  private final AlgorithmB assignment;

  /** output manager of the assignment */
  private final OutputManager outputManager;

  /**
   * initialiseBushes
   */
  private void initialiseBushes() {
    TransportNetwork transportNetwork = assignment.getTransportNetwork();
    double[] edgeSegmentCosts = null; // <-- to do
    DijkstraShortestPathAlgorithm dijkstra = new DijkstraShortestPathAlgorithm(edgeSegmentCosts, transportNetwork.getTotalNumberOfEdgeSegments(),
        transportNetwork.getTotalNumberOfVertices());

    for (Zone zone : assignment.getTransportNetwork().getZoning().zones) {
      /* for each origin create initial bush with shortest path */
    }
  }

  /**
   * Constructor
   * 
   * @param outputManager
   * 
   * @param outputManager
   */
  public AlgorithmBEquilibration(AlgorithmB assignment, OutputManager outputManager) {
    this.assignment = assignment;
    this.outputManager = outputManager;
    this.simulationData = new AlgorithmBSimulationData();
  }

  /**
   * The method that performs the equilibration for a given time period using AlgorithmB
   * 
   * @param timePeriod the time period
   * @param modes      the modes active in this period
   * @throws PlanItException thrown if error
   */
  public void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {
    // initialise the origin-based bushes for each origin and mode
    initialiseBushes();

    boolean converged = false;
    while (!converged) {
      for (Zone origin : assignment.getTransportNetwork().getZoning().zones) {
        boolean bushIsOptimal = false;
        // TODO Bush originBasedBush = getBush(origin);
//        do {
//          // alter bush by adding newly discovered min path (segments) to bush
//          // TODO improveBush(originBasedBush);
//          // equilibrate bush WITHOUT considering network
//          // remove unused max path (segments) from bush
//          // shift flows between used alternative segments in bush
//          // TODO equilibrateBush(originBasedBush);
//        } while (!isOptimal(originBasedBush));
      }

      converged = false; // update!
      outputManager.persistOutputData(timePeriod, modes, converged);
    }
  }

  /**
   * Collect the iteration information collected during the equilibration
   * 
   * @return simulation data
   */
  public AlgorithmBSimulationData getIterationData() {
    return simulationData;
  }

}
