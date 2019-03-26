package org.planit.configuration;

import java.util.List;

import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.userclass.TravellerType;
import org.planit.userclass.UserClass;

/**
 * Configuration store for MetroScan model parameters
 * 
 * @author gman6028
 *
 */
public class Configuration {
	 
	private List<Mode> modes;
	private List<TravellerType> travellerTypes;
	private List<UserClass> userClasses;
	private List<TimePeriod> timeperiods;

/**
 * Returns list of user classes
 * 
 * @return  	List of UserClass objects
 */
	public List<UserClass> getUserClasses() {
		return userClasses;
	}
	
/**
 * Stores list of user classes
 * 
 * @param userClasses			List of UserClass objects
 */
	public void setUserClasses(List<UserClass> userClasses) {
		this.userClasses = userClasses;
	}	
	
/**
 * Returns list of transport modes
 * 
 * @return				List of Mode objects
 */
	public List<Mode> getModes() {
		return modes;
	}
	
/**
 * Stores list of transport modes
 * 
 * @param modes				List of Mode objects
 */
	public void setModes(List<Mode> modes) {
		this.modes = modes;
	}
	
/**
 * Returns list of traveller types
 * 
 * @return			List of TravellerType objects
 */
	public List<TravellerType> getTravellerTypes() {
		return travellerTypes;
	}
	
/**
 * Stores list of traveller types
 * 
 * @param travellerTypes		List of TravellerType objects
 */
	public void setTravellerTypes(List<TravellerType> travellerTypes) {
		this.travellerTypes = travellerTypes;
	}
	
/**
 * Returns list of time periods
 * 
 * @return			List of TimePeriod objects
 */
	public List<TimePeriod> getTimeperiods() {
		return timeperiods;
	}
	
/**
 * Stores list of time periods
 * 
 * @param timeperiods			List of TimePeriod objects
 */
	public void setTimeperiods(List<TimePeriod> timeperiods) {
		this.timeperiods = timeperiods;
	}
	
}
