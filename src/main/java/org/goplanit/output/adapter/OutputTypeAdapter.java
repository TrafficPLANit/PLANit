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

  public static String PROPERTY_NOT_AVAILABLE = "N/A";

  /**
   * Return the output type corresponding to this output adapter
   * 
   * @return the output type corresponding to this output adapter
   */
  public abstract OutputType getOutputType();

  /**
   * Determine the iteration index of the data of the provided output type enum. Generally this is the current
   * iteration index, but if the data is, for example, trailing an iteration, this gives the correct iteration index of
   * the data rather than that of the simulation itself. So it is always safer to use this method when persisting data
   * 
   * @param subOutputTypeEnum, allowed to be null, in that case it is assumed there does not exist a suboutputtype
   *                           for this output type
   * @return iterationIndexForData
   */
  public abstract Optional<Integer> getIterationIndexForSubOutputType(
      final SubOutputTypeEnum subOutputTypeEnum);

}
