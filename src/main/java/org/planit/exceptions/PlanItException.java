package org.planit.exceptions;

public class PlanItException extends Exception {

	private static final long serialVersionUID = 567458653348604906L;
	
	public PlanItException (String exceptionDescription) {
		super(exceptionDescription);
	}
	
	public PlanItException (Exception parentException) {
		super(parentException);
	}	
}
