package org.planit.converter.demands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.time.TimePeriod;

/**
 * A demands reader implementation with built-in convenience containers that maps ids used by the external data source to relate entities to each other
 * to the created PLANit entries. 
 * 
 * @author markr
 *
 */
public abstract class DemandsReaderBase implements DemandsReader {

  /**
   * Map which stores traveler type by source Id
   */
  protected Map<String, TravelerType> sourceIdTravelerTypeMap;

  /**
   * Map which stores user class by source Id
   */
  protected Map<String, UserClass> sourceIdUserClassMap;

  /**
   * Map which stores time periods by source Id
   */
  protected Map<String, TimePeriod> sourceIdTimePeriodMap;


  /**
   * Stores an object by its source Id, after checking whether the source Id is a duplicate
   * 
   * @param <T>      type of object being stored
   * @param sourceId sourceId of object being stored
   * @param obj      object being stored
   * @param map      Map to store the object
   * @return true if this entry is duplicate, false otherwise
   */
  protected static <T> boolean addObjectToSourceIdMap(final String sourceId, final T obj, final Map<String, T> map) {
    boolean containsDuplicates = map.containsKey(sourceId);
    map.put(sourceId, obj);
    return containsDuplicates;
  }
  
  /**
   * Constructor
   */
  protected DemandsReaderBase() {
    sourceIdTravelerTypeMap = new HashMap<String, TravelerType>();
    sourceIdUserClassMap = new HashMap<String, UserClass>();
    sourceIdTimePeriodMap = new HashMap<String, TimePeriod>();
  }
  
  /**
   * Stores a traveler type by its sourceId
   * 
   * @param sourceId     sourceId of traveler type
   * @param travelerType traveler type to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addTravelerTypeToSourceIdMap(String sourceId, TravelerType travelerType) {
    return addObjectToSourceIdMap(sourceId, travelerType, sourceIdTravelerTypeMap);
  }

  /**
   * Stores a user class by its sourceId
   * 
   * @param sourceId  sourceId of user class
   * @param userClass user class to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addUserClassToSourceIdMap(String sourceId, UserClass userClass) {
    return addObjectToSourceIdMap(sourceId, userClass, sourceIdUserClassMap);
  }

  /**
   * Stores a time period by its sourceId
   * 
   * @param sourceId   sourceId of time period
   * @param timePeriod time period to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addTimePeriodToSourceIdMap(String sourceId, TimePeriod timePeriod) {
    return addObjectToSourceIdMap(sourceId, timePeriod, sourceIdTimePeriodMap);
  }  
  
  /**
   * Return traveler type for a specified sourceId
   * 
   * @param sourceId the source Id
   * @return the traveler type for the specified sourceId
   */
  public TravelerType getTravelerTypeBySourceId(String sourceId) {
    return sourceIdTravelerTypeMap.get(sourceId);
  }

  /**
   * Return user class by source Id
   * 
   * @param sourceId sourceId of user class
   * @return specified user class
   */
  public UserClass getUserClassBySourceId(String sourceId) {
    return sourceIdUserClassMap.get(sourceId);
  }

  /**
   * Return the time period for a specified sourceId
   * 
   * @param sourceId sourceId of time period
   * @return the specified time period
   */
  public TimePeriod getTimePeriodBySourceId(String sourceId) {
    return sourceIdTimePeriodMap.get(sourceId);
  }

  /**
   * Returns whether a time period for a given sourceId exists
   * 
   * @param sourceId the sourceId time period being tested
   * @return true if the external Id matches a registered time period, false otherwise
   */
  public boolean isTimePeriodSourceIdRegistered(String sourceId) {
    return sourceIdTimePeriodMap.keySet().contains(sourceId);
  }

  /**
   * Returns the number of registered time periods
   * 
   * @return the number of registered time periods
   */
  public int getNumberOfRegisteredTimePeriods() {
    return sourceIdTimePeriodMap.keySet().size();
  }

  /**
   * Returns a list of source ids of time periods
   * 
   * @return list of source ids of time periods
   */
  public List<String> getTimePeriodSourceIds() {
    return new ArrayList<String>(sourceIdTimePeriodMap.keySet());
  }  

}
