package org.planit.demands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * Container class for all demands registered on the project. In PlanIt we assume that all traffic flows between an origin and destination. Hence all demand for a given time period
 * and mode is provided between an origin and destination via ODDemand.
 *
 * Further, unlike other components, we let anyone register ODDemand compatible instances on this class to provide maximum flexibility in the underlying container since depending
 * on the od data different containers might be preferred for optimizing memory usage. Also not all ODDemand instances on the same Demands instance might utilize the same data
 * structure, hence the need to avoid a general approach across all entries within a Demands instance
 *
 * @author markr
 *
 */
public class Demands extends TrafficAssignmentComponent<Demands> implements Serializable {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Demands.class.getCanonicalName());

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 144798248371260732L;

  /**
   * Trip demand matrices
   */
  protected final TreeMap<Long, TreeMap<Mode, ODDemandMatrix>> odDemands;

  /**
   * Inner class to register and store traveler types for the current demand object
   * 
   * @author markr
   *
   */
  public class TravelerTypes implements Iterable<TravelerType> {

    /**
     * traveler types are stored in an ordered fashion using a hash map
     */
    private final Map<Long, TravelerType> travelerTypeMap;

    /**
     * Register a traveler type
     * 
     * @param travelerType traveler type to be registered
     */
    protected void registerTravelerType(TravelerType travelerType) {
      travelerTypeMap.put(travelerType.getId(), travelerType);
    }

    /**
     * Constructor
     */
    public TravelerTypes() {
      this.travelerTypeMap = new HashMap<Long, TravelerType>();
    }

    /**
     * Factory method to create and register a new travel type on the demands
     * 
     * @param externalId the external id
     * @param name       the name of the travel type
     * @return new traveler type created
     */
    public TravelerType createAndRegisterNewTravelerType(long externalId, String name) {
      TravelerType newTravelerType = new TravelerType(groupId, externalId, name);
      registerTravelerType(newTravelerType);
      return newTravelerType;
    }

    /**
     * Retrieve a traveler type by its id
     * 
     * @param id id of the traveler type
     * @return retrieved traveler type, if not present null is returned
     */
    public TravelerType getTravelerTypeById(final long id) {
      return travelerTypeMap.get(id);
    }

    /**
     * Collect the first registered traveler type.
     * 
     * @return first registered traveler type
     */
    public TravelerType getFirst() {
      return getTravelerTypeById(0);
    }

    /**
     * Iterator for traveler types (non-sorted)
     * 
     * @return iterator
     */
    @Override
    public Iterator<TravelerType> iterator() {
      return travelerTypeMap.values().iterator();
    }

    /**
     * Retrieve a TravelerType by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value, to find the traveler type when using long values for external ids.
     * 
     * @param externalId    the external Id of the specified traveler type
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved traveler type, or null if no user class was found
     */
    public TravelerType getTravelerTypeByExternalId(Object externalId, boolean convertToLong) {
      if (convertToLong) {
        try {
          long value = Long.valueOf(externalId.toString());
          return getTravelerTypeByExternalId(value);
        } catch (NumberFormatException e) {
          // do nothing - if conversion to long is not possible, use the general method instead
        }
      }
      return getTravelerTypeByExternalId(externalId);
    }

    /**
     * Retrieve a TravelerType by its external Id
     * 
     * This method is not efficient, since it loops through all the registered traveler type in order to find the required entry.
     * 
     * @param externalId the external Id of the specified traveler type
     * @return the retrieved traveler type, or null if no traveler type was found
     */
    public TravelerType getTravelerTypeByExternalId(Object externalId) {
      for (TravelerType travelerType : travelerTypes) {
        if (travelerType.getExternalId().equals(externalId)) {
          return travelerType;
        }
      }
      return null;
    }

    /**
     * Collect the number of registered traveler types
     * 
     * @return number of registered traveler types
     */
    public int getNumberOfTravelerTypes() {
      return travelerTypeMap.size();
    }

  }

  /**
   * Inner class to register and store user classes for the current demand object
   * 
   * @author markr
   *
   */
  public class UserClasses implements Iterable<UserClass> {

    /**
     * user classes are stored in an ordered fashion using a hash map
     */
    private final Map<Long, UserClass> userClassMap;

    /**
     * Register a user class
     * 
     * @param userClass user class to be registered
     */
    protected void registerUserClass(UserClass userClass) {
      userClassMap.put(userClass.getId(), userClass);
    }

    /**
     * Constructor
     */
    public UserClasses() {
      this.userClassMap = new HashMap<Long, UserClass>();
    }

    /**
     * Factory method to create and register a new user class on the demands
     * 
     * @param externalId    the external id for this user class
     * @param name          the name for this user class
     * @param mode          the mode for this user class
     * @param travellerType the travel type for this user class
     * @return new traveler type created
     */
    public UserClass createAndRegisterNewUserClass(long externalId, String name, Mode mode, TravelerType travellerType) {
      UserClass newUserClass = new UserClass(groupId, externalId, name, mode, travellerType);
      registerUserClass(newUserClass);
      return newUserClass;
    }

    /**
     * Collect the number of registered user classes
     * 
     * @return number of user classes
     */
    public int getNumberOfUserClasses() {
      return userClassMap.size();
    }

    /**
     * Retrieve a user class by its id
     * 
     * @param id id of the user class
     * @return retrieved user class
     */
    public UserClass getUserClassById(final long id) {
      return userClassMap.get(id);
    }

    /**
     * Collect the first registered user class.
     * 
     * @return first registered user class
     */
    public UserClass getFirst() {
      return getUserClassById(0);
    }

    /**
     * Iterator for user classes (non-sorted)
     * 
     * @return iterator
     */
    @Override
    public Iterator<UserClass> iterator() {
      return userClassMap.values().iterator();
    }

    /**
     * Retrieve a UserClass by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value, to find the user class when using long values for external ids.
     * 
     * @param externalId    the external Id of the specified user class
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved user class, or null if no user class was found
     */
    public UserClass getUserClassByExternalId(Object externalId, boolean convertToLong) {
      if (convertToLong) {
        try {
          long value = Long.valueOf(externalId.toString());
          return getUserClassByExternalId(value);
        } catch (NumberFormatException e) {
          // do nothing - if conversion to long is not possible, use the general method instead
        }
      }
      return getUserClassByExternalId(externalId);
    }

    /**
     * Retrieve a UserClass by its external Id
     * 
     * This method is not efficient, since it loops through all the registered user classes in order to find the required entry.
     * 
     * @param externalId the external Id of the specified user class
     * @return the retrieved user class, or null if no user class was found
     */
    public UserClass getUserClassByExternalId(Object externalId) {
      for (UserClass userClass : userClasses) {
        if (userClass.getExternalId().equals(externalId)) {
          return userClass;
        }
      }
      return null;
    }

  }

  /**
   * Inner class to register and store time periods for the current demand object
   * 
   * @author garym
   *
   */
  public class TimePeriods implements Iterable<TimePeriod> {

    /**
     * time periods are stored using a hashmap
     */
    private final Map<Long, TimePeriod> timePeriodMap;

    /**
     * Register a time period
     * 
     * @param timePeriod time period to be registered
     */
    protected void registerTimePeriod(final TimePeriod timePeriod) {
      timePeriodMap.put(timePeriod.getId(), timePeriod);
    }

    /**
     * Constructor
     */
    public TimePeriods() {
      this.timePeriodMap = new HashMap<Long, TimePeriod>();
    }

    /**
     * Factory method to create and register a new time period on the demands
     * 
     * @param externalId       the external id for this time period
     * @param description      the description for this time period
     * @param startTimeSeconds the start time in seconds since midnight (00:00)
     * @param durationSeconds  the duration in seconds since start time
     * @return new time period created
     * @throws PlanItException thrown if start time and/or duration are invalid
     */
    public TimePeriod createAndRegisterNewTimePeriod(long externalId, String description, long startTimeSeconds, long durationSeconds) throws PlanItException {
      TimePeriod newTimePeriod = new TimePeriod(groupId, externalId, description, startTimeSeconds, durationSeconds);
      registerTimePeriod(newTimePeriod);
      return newTimePeriod;
    }

    /**
     * Retrieve a time period by its id
     * 
     * @param id id of the time period
     * @return retrieved time period
     */
    public TimePeriod getTimePeriodById(final long id) {
      return timePeriodMap.get(id);
    }

    /**
     * Collect the number of registered time periods
     * 
     * @return numver of time periods
     */
    public int getNumberOfTimePeriods() {
      return timePeriodMap.size();
    }

    /**
     * Collect the first registered time period. This is not necessarily the earliest time period.
     * 
     * @return first registered time period
     */
    public TimePeriod getFirst() {
      return getTimePeriodById(0);
    }

    /**
     * Returns a set of all registered time periods sorted by the start time, i.e., the way th time period is comparable
     * 
     * @return Set of all registered time periods
     */
    public SortedSet<TimePeriod> asSortedSetByStartTime() {
      SortedSet<TimePeriod> timePeriodSet = new TreeSet<TimePeriod>(timePeriodMap.values());
      return timePeriodSet;
    }

    /**
     * Iterator for time periods (non-sorted)
     * 
     * @return iterator
     */
    @Override
    public Iterator<TimePeriod> iterator() {
      return timePeriodMap.values().iterator();
    }

    /**
     * Retrieve a TimePeriod by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value, to find the time period when time period objects use long values for external ids.
     * 
     * @param externalId    the external Id of the specified time period
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved time period, or null if no time period was found
     */
    public TimePeriod getTimePeriodByExternalId(final Object externalId, final boolean convertToLong) {
      if (convertToLong) {
        try {
          long value = Long.valueOf(externalId.toString());
          return getTimePeriodByExternalId(value);
        } catch (NumberFormatException e) {
          // do nothing - if conversion to long is not possible, use the general method instead
        }
      }
      return getTimePeriodByExternalId(externalId);
    }

    /**
     * Retrieve a TimePeriod by its external Id
     * 
     * This method is not efficient, since it loops through all the registered time periods in order to find the required time period.
     * 
     * @param externalId the external Id of the specified time period
     * @return the retrieved time period, or null if no time period was found
     */
    public TimePeriod getTimePeriodByExternalId(final Object externalId) {
      for (TimePeriod timePeriod : timePeriods) {
        if (timePeriod.getExternalId().equals(externalId)) {
          return timePeriod;
        }
      }
      return null;
    }

  }

  /**
   * internal class instance containing all time periods on this demand instance
   */
  public final TimePeriods timePeriods = new TimePeriods();

  /**
   * internal class instance containing all user classes on this demand instance
   */
  public final UserClasses userClasses = new UserClasses();

  /**
   * internal class instance containing all traveler types on this demand instance
   */
  public final TravelerTypes travelerTypes = new TravelerTypes();

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public Demands(IdGroupingToken groupId) {
    super(groupId, Demands.class);
    odDemands = new TreeMap<Long, TreeMap<Mode, ODDemandMatrix>>();
  }

  /**
   * Register provided odDemand
   *
   * @param timePeriod     the time period for this origin-demand object
   * @param mode           the mode for this origin-demand object
   * @param odDemandMatrix the origin-demand object to be registered
   * @return oldODDemand if there already existed an odDemand for the given mode and time period, the overwritten entry is returned
   */
  public ODDemandMatrix registerODDemand(final TimePeriod timePeriod, final Mode mode, final ODDemandMatrix odDemandMatrix) {
    if (!odDemands.containsKey(timePeriod.getId())) {
      odDemands.put(timePeriod.getId(), new TreeMap<Mode, ODDemandMatrix>());
    }
    return odDemands.get(timePeriod.getId()).put(mode, odDemandMatrix);
  }

  /**
   * Get an ODDemand by mode and time period
   *
   * @param mode       the mode for which the ODDemand object is required
   * @param timePeriod the time period for which the ODDemand object is required
   * @return ODDemand object if found, otherwise null
   */

  public ODDemandMatrix get(final Mode mode, final TimePeriod timePeriod) {
    if (odDemands.containsKey(timePeriod.getId()) && odDemands.get(timePeriod.getId()).containsKey(mode)) {
      return odDemands.get(timePeriod.getId()).get(mode);
    } else {
      return null;
    }
  }

  /**
   * Get modes registered for the given time period
   *
   * @param timePeriod the specified time period
   * @return Set of modes for this time period
   */
  public Set<Mode> getRegisteredModesForTimePeriod(final TimePeriod timePeriod) {
    if (odDemands.containsKey(timePeriod.getId())) {
      return odDemands.get(timePeriod.getId()).keySet();
    } else {
      return null;
    }
  }

}
