package org.planit.exceptions;

/**
 * General exception for PlanIt which wraps exceptions thrown during execution
 * 
 * @author markr
 *
 */
public class PlanItException extends Exception {

    /**
     * Logger for this class
     */
    private static final long serialVersionUID = 567458653348604906L;

    /**
     * Constructor using String - catches logic-driven exceptions
     * 
     * @param exceptionDescription
     *            text containing description of logic error
     */
    public PlanItException(String exceptionDescription) {
        super(exceptionDescription);
    }

    /**
     * Constructor using Exception - wraps run-time exceptions
     * 
     * @param parentException
     *            source exception
     */
    public PlanItException(Exception parentException) {
        super(parentException);
    }
}
