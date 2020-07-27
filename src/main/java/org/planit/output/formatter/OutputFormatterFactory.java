package org.planit.output.formatter;

import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Factory to create output writers of any compatible type
 * 
 * @author markr
 *
 */
public final class OutputFormatterFactory {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(OutputFormatterFactory.class.getCanonicalName());

  /**
   * Create an output formatter based on the passed in class name
   * 
   * @param OutputFormatterCanonicalClassName canonical class name of the desired output formatter
   * @return created output formatter instance
   * @throws PlanItException thrown if there is an error
   */
  public static OutputFormatter createOutputFormatter(String OutputFormatterCanonicalClassName) throws PlanItException {
    Object newOutputFormatter = null;
    try {
      newOutputFormatter = Class.forName(OutputFormatterCanonicalClassName).getConstructor().newInstance(IdGroupingToken.collectGlobalToken());
      PlanItException.throwIf(newOutputFormatter == null || !(newOutputFormatter instanceof OutputFormatter), "Provided output formatter class is not eligible for instantiation");
    } catch (Exception e) {
      throw new PlanItException(e);
    }
    return (OutputFormatter) newOutputFormatter;
  }

}
