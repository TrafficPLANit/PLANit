package org.planit.utils;

import java.util.HashMap;

/** Convenience class to track unique ids across different classes that decide to use a generator for their id members
 * @author markr
 *
 */
public class IdGenerator {

	/** track unique id's per specific class */
	//protected static final HashMap<Class<? extends Object>,Integer> idTypes = new HashMap<Class<? extends Object>, Integer>();
	private static HashMap<Class<? extends Object>,Integer> idTypes;
	
	/** Create a new idGenerator for this type such that we track unique id's within this class
	 * @param theClass
	 * @return initialId;
	 */
	protected static void createNewIdType(Class<? extends Object> theClass){
		Integer initialId = 0; // choose 0 as this way we can use each id as an index in an array without additional effort
		idTypes.put(theClass, initialId);
	}
	
	/** Generate a unique id for the chosen class
	 * @param theClass
	 * @return
	 */
	public static int generateId(Class<? extends Object> theClass) {
		if(!idTypes.containsKey(theClass)) {
			createNewIdType(theClass);
		}else {
			int id = idTypes.get(theClass);
			id++;
			idTypes.put(theClass, id);
		}
		return idTypes.get(theClass);
	}
	
	public static void reset() {
		 idTypes = new HashMap<Class<? extends Object>, Integer>();
	}
}
