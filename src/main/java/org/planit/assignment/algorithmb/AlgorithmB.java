package org.planit.assignment.algorithmb;

import java.util.Set;
import java.util.logging.Logger;

import org.djutils.event.EventType;
import org.planit.assignment.StaticTrafficAssignment;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.time.TimePeriod;
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
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(AlgorithmB.class.getCanonicalName());

  /** equilibration functionality for Algorithm B */
  private final AlgorithmBEquilibration equilibration;

  /**
   * Constructor
   * 
   * @param groupId group the id generator will be using when genarting the id
   */
  public AlgorithmB(IdGroupingToken groupId) {
    super(groupId);
    equilibration = new AlgorithmBEquilibration(this, this.getOutputManager());
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

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {
    equilibration.executeTimePeriod(timePeriod, modes);
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
    return equilibration.getIterationData();
  }

}
