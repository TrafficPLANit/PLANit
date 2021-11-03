package org.goplanit.output.formatter;

import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.reflection.ReflectionUtils;

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
    return (OutputFormatter) ReflectionUtils.createInstance(OutputFormatterCanonicalClassName, IdGroupingToken.collectGlobalToken());
  }

}
