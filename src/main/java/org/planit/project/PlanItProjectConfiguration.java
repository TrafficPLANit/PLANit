package org.planit.project;

import java.util.List;

import org.planit.configuration.Configuration;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.userclass.Mode;
import org.planit.userclass.UserClass;

@Deprecated
/**
 * Use org.planit.configuration.Configuration instead
 * 
 * @author gman6028
 *
 */
public class PlanItProjectConfiguration extends TrafficAssignmentComponent<PlanItProjectConfiguration> {
	
	private List<UserClass> userClasses;
	
	private List<Mode> modes;

	public PlanItProjectConfiguration(List<UserClass> userClasses, List<Mode> modes) {
		this.userClasses = userClasses;
		this.modes = modes;
	}
	
	public PlanItProjectConfiguration(Configuration configuration) {
		this(configuration.getUserClasses(), configuration.getModes());
	}

	public List<UserClass> getUserClasses() {
		return userClasses;
	}

	public void setUserClasses(List<UserClass> userClasses) {
		this.userClasses = userClasses;
	}

	public List<Mode> getModes() {
		return modes;
	}

	public void setModes(List<Mode> modes) {
		this.modes = modes;
	}

}
