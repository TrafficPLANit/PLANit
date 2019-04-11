package org.planit.exceptions;

import javax.annotation.Nonnull;

import org.planit.trafficassignment.TrafficAssignmentComponent;

@Deprecated
//TODO - At present this Exception is not used.  We need to check whether we still need it, and if so could we use PlanItException instead.
public class PlanItIncompatibilityException extends Exception {

	private static final long serialVersionUID = -2472775833174454936L;

	public PlanItIncompatibilityException(@Nonnull TrafficAssignmentComponent<?> t1, @Nonnull TrafficAssignmentComponent<?> t2){
		super("Traffic assignment components " + t1.getTrafficComponentType() + " and " + t2.getTrafficComponentType() + "are incompatible");
	}
}
