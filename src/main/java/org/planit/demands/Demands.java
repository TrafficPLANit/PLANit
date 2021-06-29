package org.planit.demands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriodImpl;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.time.TimePeriodUtils;
import org.planit.utils.wrapper.LongMapWrapper;

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
  public class TravelerTypes extends LongMapWrapper<TravelerType> {

    /**
     * Constructor
     */
    public TravelerTypes() {
      super(new HashMap<Long, TravelerType>(), TravelerType::getId);
    }

    /**
     * Factory method to create and register a new travel type on the demands
     * 
     * @param name the name of the travel type
     * @return new traveler type created
     */
    public TravelerType createAndRegisterNew(String name) {
      TravelerType newTravelerType = new TravelerType(getIdGroupingToken(), name);
      register(newTravelerType);
      return newTravelerType;
    }

    /**
     * Collect the first registered traveler type.
     * 
     * @return first registered traveler type
     */
    public TravelerType getFirst() {
      return get(0);
    }

    /**
     * Retrieve a TravelerType by its XML Id
     * 
     * This method is not efficient, since it loops through all the registered traveler type in order to find the required entry.
     * 
     * @param xmlId the XML Id of the specified traveler type
     * @return the retrieved traveler type, or null if no traveler type was found
     */
    public TravelerType getByXmlId(String xmlId) {
      return findFirst(travelerType -> xmlId.equals(((TravelerType) travelerType).getXmlId()));
    }

  }

  /**
   * Inner class to register and store user classes for the current demand object
   * 
   * @author markr
   *
   */
  public class UserClasses extends LongMapWrapper<UserClass> {

    /**
     * Constructor
     */
    public UserClasses() {
      super(new HashMap<Long, UserClass>(), UserClass::getId);
    }

    /**
     * Factory method to create and register a new user class on the demands
     * 
     * @param name          the name for this user class
     * @param mode          the mode for this user class
     * @param travellerType the travel type for this user class
     * @return new traveler type created
     */
    public UserClass createAndRegisterNewUserClass(String name, Mode mode, TravelerType travellerType) {
      UserClass newUserClass = new UserClass(getIdGroupingToken(), name, mode, travellerType);
      register(newUserClass);
      return newUserClass;
    }

    /**
     * Collect the first registered user class.
     * 
     * @return first registered user class
     */
    public UserClass getFirst() {
      return get(0);
    }

    /**
     * Retrieve a UserClass by its XML Id
     * 
     * This method is not efficient, since it loops through all the registered user classes in order to find the required entry.
     * 
     * @param xmlId the XML Id of the specified user class
     * @return the retrieved user class, or null if no user class was found
     */
    public UserClass getUserClassByXmlId(String xmlId) {
      return findFirst(userClass -> xmlId.equals(((UserClass) userClass).getXmlId()));
    }
  }

  /**
   * Inner class to register and store time periods for the current demand object
   * 
   * @author garym, markr
   *
   */
  public class TimePeriods extends LongMapWrapper<TimePeriod> {

    /**
     * Constructor
     */
    public TimePeriods() {
      super(new HashMap<Long, TimePeriod>(), TimePeriod::getId);
    }

    /**
     * Factory method to create and register a new time period on the demands
     * 
     * @param description      the description for this time period
     * @param startTimeSeconds the start time in seconds since midnight (00:00)
     * @param durationSeconds  the duration in seconds since start time
     * @return new time period created
     * @throws PlanItException thrown if start time and/or duration are invalid
     */
    public TimePeriod createAndRegisterNewTimePeriod(String description, long startTimeSeconds, long durationSeconds) throws PlanItException {
      TimePeriod newTimePeriod = new TimePeriodImpl(getIdGroupingToken(), description, startTimeSeconds, durationSeconds);
      register(newTimePeriod);
      return newTimePeriod;
    }

    /**
     * Collect the first registered time period. This is not necessarily the earliest time period.
     * 
     * @return first registered time period
     */
    public TimePeriod getFirst() {
      return get(0);
    }

    /**
     * Returns a set of all registered time periods sorted by the start time, i.e., the way th time period is comparable
     * 
     * @return Set of all registered time periods
     */
    public SortedSet<TimePeriod> asSortedSetByStartTime() {
      SortedSet<TimePeriod> timePeriodSet = new TreeSet<TimePeriod>(TimePeriodUtils.comparatorByStartTime());
      timePeriodSet.addAll(getMap().values());
      return timePeriodSet;
    }

    /**
     * Retrieve a TimePeriod by its xml Id
     * 
     * This method is not efficient, since it loops through all the registered time periods in order to find the required time period.
     * 
     * @param xmlId the XML Id of the specified time period
     * @return the retrieved time period, or null if no time period was found
     */
    public TimePeriod getTimePeriodByXmlId(final String xmlId) {
      return findFirst(timePeriod -> xmlId.equals(((TimePeriod) timePeriod).getXmlId()));
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
