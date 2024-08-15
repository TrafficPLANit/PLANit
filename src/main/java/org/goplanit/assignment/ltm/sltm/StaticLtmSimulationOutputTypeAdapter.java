package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.adapter.SimulationOutputTypeAdapterImpl;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.RunTimesTracker;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Adapter providing access to the data of the StaticLtm class relevant for path outputs without exposing the internals
 * of the traffic assignment class itself
 *
 * @author markr
 *
 */
public class StaticLtmSimulationOutputTypeAdapter extends SimulationOutputTypeAdapterImpl {

  /**
   * Given a mode extract a property from path strategy assuming this is the strategy available
   *
   * @param functionToApply function to apply
   * @return property value
   */
  protected Optional<?> getPathStrategyProperty(Function<StaticLtmPathStrategy, ?> functionToApply) {
    if(!getAssignment().settings.getSltmType().equals(StaticLtmType.PATH_BASED)){
      return getUnavailableOutputType();
    }

    var sltStrategy = ((StaticLtmPathStrategy) getAssignment().getAssignmentStrategy());
    return Optional.of(functionToApply.apply(sltStrategy));
  }


  /**
   * Access to run time tracking per iteration for persistence
   *
   * @return run time for latest route choice iteration
   */
  @Override
  protected Optional<?> getRouteChoiceIterationRunTime() {
    return Optional.of(getAssignment().getIterationData().getRunTimesTracker().get(RunTimesTracker.GENERAL).getIterationTimeInMillis());
  }

  /**
   * Access to path count of sLTM in case explicit paths are available
   *
   * @return total path count for latest route choice iteration
   */
  @Override
  protected Optional<?> getTotalPathCount(Mode mode) {
    return getPathStrategyProperty( (pathStrategy) -> pathStrategy.getOdMultiPaths(mode).determineTotalPaths());
  }

  /**
   *  {@inheritDoc}
   */
  @Override
  protected Optional<?> getAddedPathCount(Mode mode) {
    return getPathStrategyProperty( (pathStrategy) -> pathStrategy.getAddedPathsCounter().longValue());
  }

  /**
   *  {@inheritDoc}
   */
  protected Optional<?> getRemovedPathCount(Mode mode) {
    return getPathStrategyProperty( (pathStrategy) -> pathStrategy.getRemovedPathsCounter().longValue());
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtm getAssignment() {
    return (StaticLtm) super.getAssignment();
  }

  /**
   * Constructor
   *
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public StaticLtmSimulationOutputTypeAdapter(final OutputType outputType, final TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

}
