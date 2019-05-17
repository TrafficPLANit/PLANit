package org.planit.output.formatter;

import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;

/**
 * Factory to create output writers of any compatible type
 * 
 * @author markr
 *
 */
public final class OutputFormatterFactory {

    private static final Logger LOGGER = Logger.getLogger(OutputFormatterFactory.class.getName());

    /**
     * Create an output formatter based on the passed in class name
     * 
     * @param OutputFormatterCanonicalClassName
     *            canonical class name of the desired output formatter
     * @return created output formatter instance
     * @throws PlanItException
     *             thrown if there is an error
     */
    public static OutputFormatter createOutputFormatter(String OutputFormatterCanonicalClassName)
            throws PlanItException {
        Object newOutputFormatter = null;
        try {
            newOutputFormatter = Class.forName(OutputFormatterCanonicalClassName).getConstructor().newInstance();
            if (newOutputFormatter == null || !(newOutputFormatter instanceof OutputFormatter)) {
                throw new PlanItException("Provided output formatter class is not eligible for instantiation");
            }
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }
        return (OutputFormatter) newOutputFormatter;
    }

}
