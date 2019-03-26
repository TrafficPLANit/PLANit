package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.IdGenerator;

/** A user class defines a combination of one or more characteristics of users in an aggregate representation of traffic
 * which partially dictate how they behave in traffic assignment. 
 * @author markr
 *
 */
public class UserClass {
	
	private long id;
	private String name;
	private Mode mode;
	private TravellerType travellerType;
	private long modeId = 0;
	private long travellerTypeId = 0;
	
	private static Map<Long, UserClass> userClasses = new HashMap<Long, UserClass>();
	
	/**
	 * constructor of user class
	 * @param id
	 * @param name
	 * @param mode
	 */
	public UserClass(String name, Mode mode, TravellerType travellerType) {
		this.id = IdGenerator.generateId(UserClass.class);
		this.name = name;
		this.mode = mode;
		this.travellerType = travellerType;
		userClasses.put(this.id, this);
	}
		
	public UserClass(long id, String name, Mode mode, TravellerType travellerType) {
		this.id = id;
		this.name = name;
		this.mode = mode;
		this.travellerType = travellerType;
		userClasses.put(this.id, this);
	}
	
	public UserClass(long id, String name, long modeId, long travellerTypeId) {
		this.id = id;
		this.name = name;
		this.modeId = modeId;
		this.travellerTypeId = travellerTypeId;
		this.mode = Mode.getById(modeId);
		this.travellerType = TravellerType.getById(travellerTypeId);
		userClasses.put(this.id, this);
	}
    public static UserClass getById(long id) {
    	return userClasses.get(id);
    }
	
	public static void putById(UserClass userClass) {
		userClasses.put(userClass.getId(), userClass);
	}
	
	public TravellerType getTravellerType() {
		return travellerType;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public Mode getMode() {
		return mode;
	}

	public long getModeId() {
		return modeId;
	}

	public long getTravellerTypeId() {
		return travellerTypeId;
	}

}
