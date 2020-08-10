package org.planit.assignment.algorithmb;

import java.util.Set;
import java.util.logging.Logger;

import org.djutils.event.EventType;
import org.planit.assignment.StaticTrafficAssignment;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;

/**
 * Algorithm B implementation based on the work of Dial (2006). Due to a lack of explicit explanation for the implementation of the underlying algorithm, the implementation follows
 * the main principles explained in this work but the algorithm design is likely different than the one implemented for the results discussed in the original paper.
 * 
 * @author markr
 */
public class AlgorithmB extends StaticTrafficAssignment {

  /**
   * Serial UID
   */
  private static final long serialVersionUID = 3187519479500384861L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(AlgorithmB.class.getCanonicalName());

  /** simulation data for Algorithm B */
  private final AlgorithmBSimulationData simulationData = new AlgorithmBSimulationData();

  /**
   * Constructor
   * 
   * @param groupId
   */
  public AlgorithmB(IdGroupingToken groupId) {
    super(groupId);
  }

  @Override
  protected TrafficAssignmentBuilder createTrafficAssignmentBuilder(InputBuilderListener trafficComponentCreateListener, Demands demands, Zoning zoning,
      PhysicalNetwork physicalNetwork) throws PlanItException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void addRegisteredEventTypeListeners(EventType eventType) {
    // TODO Auto-generated method stub

  }

  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {
    // initialise the origin-based bushes for each origin and mode
//    initialiseBushes();
//
//    boolean converged = false;
//
//    Calendar iterationStartTime = Calendar.getInstance();
//    while (!converged) {
//      dualityGapFunction.reset();
//      smoothing.update(simulationData.getIterationIndex());
//
//      // NETWORK LOADING - PER MODE
//      for (final Mode mode : modes) {
//        // :TODO ugly -> you are not resetting 1 matrix but multiple NAMES ARE WRONG
//        // :TODO: slow -> only reset or do something when it is stored in the first place, this is
//        // not checked
//        simulationData.resetSkimMatrix(mode, getTransportNetwork().getZoning().zones);
//        simulationData.resetPathMatrix(mode, getTransportNetwork().getZoning().zones);
//        simulationData.resetModalNetworkSegmentFlows(mode, numberOfNetworkSegments);
//
//        final double[] modalLinkSegmentCosts = simulationData.getModalLinkSegmentCosts(mode);
//        executeAndSmoothTimePeriodAndMode(timePeriod, mode, modalLinkSegmentCosts);
//      }
//
//      dualityGapFunction.computeGap();
//      simulationData.incrementIterationIndex();
//      iterationStartTime = logIterationInformation(iterationStartTime, dualityGapFunction.getMeasuredNetworkCost(), dualityGapFunction.getGap());
//      for (final Mode mode : modes) {
//        final double[] modalLinkSegmentCosts = recalculateModalLinkSegmentCosts(mode, timePeriod);
//        simulationData.setModalLinkSegmentCosts(mode, modalLinkSegmentCosts);
//      }
//      converged = dualityGapFunction.hasConverged(simulationData.getIterationIndex());
//      outputManager.persistOutputData(timePeriod, modes, converged);
//    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getIterationIndex() {
    return getIterationData() == null ? 0 : getIterationData().getIterationIndex();
  }

  /**
   * Return the simulation data for the current iteration
   *
   * @return simulation data
   */
  public AlgorithmBSimulationData getIterationData() {
    return simulationData;
  }

}
