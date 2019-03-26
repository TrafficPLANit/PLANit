package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.IdGenerator;

/**
 * Traveler type is a placeholder for all different types of traveler characteristics that affect the user class in
 * the route choice component of traffic assignment. Together with the mode this largely defines each user class
 * TODO: Not used yet in UserClass class
 *  
 * @author markr
 *
 */
public class TravellerType {

	/**
	 * Unique feature id
	 */
	private final long id;
	private final String name;
	
	private static Map<Long, TravellerType> travellerTypes = new HashMap<Long, TravellerType>();
	
	public TravellerType(long id, String name) {
		this.id = id;
		this.name = name;
		travellerTypes.put(this.id, this);
	}

	public TravellerType(String name) {
		this.id = IdGenerator.generateId(TravellerType.class);
		this.name = name;
		travellerTypes.put(this.id, this);
	}
	
	public static TravellerType getById(long id) {
		return travellerTypes.get(id);
	}
	
	public static void putById(TravellerType travellerType) {
		travellerTypes.put(travellerType.getId(), travellerType);
	}
	
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
