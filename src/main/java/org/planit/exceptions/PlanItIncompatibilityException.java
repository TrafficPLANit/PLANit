package org.planit.exceptions;

import javax.annotation.Nonnull;

import org.planit.trafficassignment.TrafficAssignmentComponent;

public class PlanItIncompatibilityException extends Exception {

	private static final long serialVersionUID = -2472775833174454936L;

	public PlanItIncompatibilityException(@Nonnull TrafficAssignmentComponent<?> t1, @Nonnull TrafficAssignmentComponent<?> t2){
		super("Traffic assignment components " + t1.getTrafficComponentType() + " and " + t2.getTrafficComponentType() + "are incompatible");
	}
}
