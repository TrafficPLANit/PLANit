package org.planit.demand;

import java.util.Set;
import java.util.TreeMap;

import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.userclass.Mode;

/**
 * Container class for all demands registered on the project. In PlanIt we assume that
 * all traffic flows between an origin and destination. Hence all demand for a given time period
 * and mode is provided between an origin and destination via ODDemand.
 * 
 * Further, unlike other components, we let anyone register ODDemand compatible instances on this class
 * to provide maximum flexibility in the underlying container since depending on the od data different containers
 * might be preferred for optimizing memory usage. Also not all ODDemand instances on the same Demands instance might
 * utilize the same data structure, hence the need to avoid a general approach across all entries within a Demands instance
 * 
 * @author markr
 *
 */
public class Demands extends TrafficAssignmentComponent<Demands>{
		
	/**
	 * Trip demand matrices
	 */
	protected final TreeMap<TimePeriod,TreeMap<Mode,ODDemand>> odDemands = new TreeMap<TimePeriod,TreeMap<Mode,ODDemand>>();	
		
	/**
	 * Constructor
	 */
	public Demands() {	
		super();
	}
	
	
	/** Register provided odDemand
	 * @param timePeriod
	 * @param mode
	 * @param odDemand
	 * @return oldODDemand, in case there already existed an odDemand for the given mode and time period, the overwritten entry is returned
	 */
	public ODDemand registerODDemand(TimePeriod timePeriod, Mode mode, ODDemand odDemand) {
		odDemands.putIfAbsent(timePeriod,new TreeMap<Mode,ODDemand>());
		TreeMap<Mode,ODDemand> tripMatrixByMode = odDemands.get(timePeriod);
		return tripMatrixByMode.put(mode, odDemand);		
	}
	
	/** Get an ODDemand by mode and time period
	 * @param mode
	 * @param timePeriod
	 * @return odDemand, if any, otherwise null
	 */
	public ODDemand get(Mode mode, TimePeriod timePeriod) {
		if(odDemands.containsKey(timePeriod) && odDemands.get(timePeriod).containsKey(mode))
		{
			return odDemands.get(timePeriod).get(mode);			
		}else{
			return null;			
		}
	}
	
	/** Get time periods registered
	 * @return
	 */
	public Set<TimePeriod> getRegisteredTimePeriods() {
		return odDemands.keySet();
	}
	
	/** Get modes registered for the given time period
	 * @param timePeriod
	 * @return
	 */
	public Set<Mode> getRegisteredModesForTimePeriod(TimePeriod timePeriod) {
		if(odDemands.containsKey(timePeriod)) {
			return odDemands.get(timePeriod).keySet();
		}else{
			return null;
		}
	}	
	

}
