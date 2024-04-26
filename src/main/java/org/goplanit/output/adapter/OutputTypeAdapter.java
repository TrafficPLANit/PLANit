package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Top-level interface for all output type adapters
 * 
 * @author gman6028
 *
 */
public interface OutputTypeAdapter {

  /**
   * Return the output type corresponding to this output adapter
   * 
   * @return the output type corresponding to this output adapter
   */
  public abstract OutputType getOutputType();

  /**
   * Determine the iteration index that is relevant for the data related to the provided output type enum Generally, this equates to the actual current iteration index, but if the
   * data for example is trailing an iteration then this will collect the correct iteration index for this data as opposed to the iteration index of the simulation itself. Hence,
   * it is always safer to use this method when persisting data
   * 
   * @param subOutputTypeEnum, allowed to be null, in that case it is assumed there does not exist a suboutputtype for this output type
   * @return iterationIndexForData
   */
  public abstract Optional<Integer> getIterationIndexForSubOutputType(
      final SubOutputTypeEnum subOutputTypeEnum);

}
