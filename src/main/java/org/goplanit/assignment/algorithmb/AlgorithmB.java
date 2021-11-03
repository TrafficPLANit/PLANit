package org.goplanit.assignment.algorithmb;

import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.StaticTrafficAssignment;
import org.goplanit.component.PlanitComponent;
import org.goplanit.interactor.InteractorAccessor;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.supply.networkloading.NetworkLoading;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

/**
 * Algorithm B implementation based on the work of Dial (2006). Due to a lack of explicit explanation for the implementation of the underlying algorithm, the implementation follows
 * the main principles explained in this work but the algorithm design is likely different than the one implemented for the results discussed in the original paper.
 * <p>
 * <b>INCOMPLETE IMPLEMENTATION</b>
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
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyNetworkDemandZoningCompatibility() {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {
    equilibration.executeTimePeriod(timePeriod, modes);
  }

  /**
   * Constructor
   * 
   * @param groupId group the id generator will be using when genarting the id
   */
  public AlgorithmB(IdGroupingToken groupId) {
    super(groupId);
    equilibration = new AlgorithmBEquilibration(this, this.getOutputManager());
  }

  /**
   * Constructor
   * 
   * @param groupId group the id generator will be using when genarting the id
   */
  public AlgorithmB(AlgorithmB algorithmB) {
    super(algorithmB);
    equilibration = algorithmB.equilibration;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitComponent<NetworkLoading> clone() {
    return new AlgorithmB(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends InteractorAccessor<?>> getCompatibleAccessor() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub
  }

}
