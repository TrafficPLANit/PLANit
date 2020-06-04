package org.planit.demands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Mode;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;

/**
 * Container class for all demands registered on the project. In PlanIt we
 * assume that all traffic flows between an origin and destination. Hence all
 * demand for a given time period and mode is provided between an origin and
 * destination via ODDemand.
 *
 * Further, unlike other components, we let anyone register ODDemand compatible
 * instances on this class to provide maximum flexibility in the underlying
 * container since depending on the od data different containers might be
 * preferred for optimizing memory usage. Also not all ODDemand instances on the
 * same Demands instance might utilize the same data structure, hence the need
 * to avoid a general approach across all entries within a Demands instance
 *
 * @author markr
 *
 */
public class Demands extends TrafficAssignmentComponent<Demands> implements Serializable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Demands.class.getCanonicalName());

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 144798248371260732L;

  /**
   * Map of registered User Classes
   */
  private Map<Long, UserClass> userClassMap;

  /**
   * Map of registered Traveler Types
   */
  private Map<Long, TravelerType> travelerTypeMap;

  // register to be eligible in PLANit
  static {
    try {
      TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(Demands.class);
    } catch (final PlanItException e) {
      e.printStackTrace();
    }
  }

  /**
   * unique identifier for this demand set
   */
  protected long id;

  /**
   * Trip demand matrices
   */
  protected final TreeMap<Long, TreeMap<Mode, ODDemandMatrix>> odDemands;

  /**
   * Inner class to register and store time periods for the current demand object
   * 
   * @author garym
   *
   */
  public class TimePeriods {

    private Map<Long, TimePeriod> timePeriodMap;

    public TimePeriods() {
      this.timePeriodMap = new HashMap<Long, TimePeriod>();
    }

    /**
     * Register a time period
     * 
     * @param timePeriod time period to be registered
     */
    public void registerTimePeriod(TimePeriod timePeriod) {
      timePeriodMap.put(timePeriod.getId(), timePeriod);
    }

    /**
     * Retrieve a time period by its id
     * 
     * @param id id of the time period
     * @return retrieved time period
     */
    public TimePeriod getTimePeriodById(long id) {
      return timePeriodMap.get(id);
    }

    /**
     * Returns a set of all registered time periods
     * 
     * @return Set of all registered time periods
     */
    public SortedSet<TimePeriod> getRegisteredTimePeriods() {
      SortedSet<TimePeriod> timePeriodSet = new TreeSet<TimePeriod>(timePeriodMap.values());
      return timePeriodSet;
    }

    /**
     * Retrieve a TimePeriod by its external Id
     * 
     * This method has the option to convert the external Id parameter into a long value,
     * to find the time period when time period objects use long values for external ids.
     * 
     * @param externalId the external Id of the specified time period
     * @param convertToLong if true, the external Id is converted into a long before beginning the search
     * @return the retrieved time period, or null if no  time period was found
     * @throws PlanItException thrown if the external Id cannot be cast into a long
     */
    public TimePeriod getTimePeriodByExternalId(Object externalId, boolean convertToLong) {
      if (convertToLong) {
        try {          
          long value = Long.valueOf(externalId.toString());
          return getTimePeriodByExternalId(value);
        } catch (NumberFormatException e) {
          //do nothing - if conversion to long is not possible, use the general method instead
        }          
      }
      return getTimePeriodByExternalId(externalId);
    }

    /**
     * Retrieve a TimePeriod by its external Id
     * 
     * This method is not efficient, since it loops through all the registered time periods in order
     * to find the required time period. The equivalent method in InputBuilderListener is more
     * efficient and should be used in preference to this in Java code.
     * 
     * @param externalId the external Id of the specified time period
     * @return the retrieved time period, or null if no time period was found
     */
    public TimePeriod getTimePeriodByExternalId(Object externalId) {
      for (TimePeriod timePeriod : timePeriodMap.values()) {
        if (timePeriod.getExternalId().equals(externalId)) {
          return timePeriod;
        }
      }
      return null;
    }

  }

  /**
   * internal class instance containing all link specific functionality
   */
  public final TimePeriods timePeriods = new TimePeriods();

  /**
   * Constructor
   */
  public Demands() {
    super();
    this.id = IdGenerator.generateId(Demands.class);
    odDemands = new TreeMap<Long, TreeMap<Mode, ODDemandMatrix>>();
    userClassMap = new HashMap<Long, UserClass>();
    travelerTypeMap = new HashMap<Long, TravelerType>();
  }

  /**
   * Returns a set of all registered time periods
   * 
   * This method should only be used by the Python interface.
   * 
   * @return Set of all registered time periods
   */
  public Set<TimePeriod> getRegisteredTimePeriods() {
    return timePeriods.getRegisteredTimePeriods();
  }

  /**
   * Retrieve the TimePeriods local class
   * 
   * This method should only be called by the Python interface
   * 
   * @return the TimePeriods local class
   */
  public TimePeriods getTimePeriods() {
    return timePeriods;
  }

  /**
   * Register provided odDemand
   *
   * @param timePeriod the time period for this origin-demand object
   * @param mode the mode for this origin-demand object
   * @param odDemandMatrix the origin-demand object to be registered
   * @return oldODDemand if there already existed an odDemand for the given mode
   *         and time period, the overwritten entry is returned
   */
  public ODDemandMatrix registerODDemand(final TimePeriod timePeriod, final Mode mode,
      final ODDemandMatrix odDemandMatrix) {
    if (!odDemands.containsKey(timePeriod.getId())) {
      odDemands.put(timePeriod.getId(), new TreeMap<Mode, ODDemandMatrix>());
    }
    return odDemands.get(timePeriod.getId()).put(mode, odDemandMatrix);
  }

  /**
   * Get an ODDemand by mode and time period
   *
   * @param mode
   *          the mode for which the ODDemand object is required
   * @param timePeriod
   *          the time period for which the ODDemand object is required
   * @return ODDemand object if found, otherwise null
   */

  public ODDemandMatrix get(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    if (!odDemands.containsKey(timePeriod.getId())) {
      String errorMessage = "No demands matrix for time period " + timePeriod.getId();
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (odDemands.containsKey(timePeriod.getId()) && odDemands.get(timePeriod.getId()).containsKey(mode)) {
      return odDemands.get(timePeriod.getId()).get(mode);
    } else {
      return null;
    }
  }

  /**
   * Get modes registered for the given time period
   *
   * @param timePeriod
   *          the specified time period
   * @return Set of modes for this time period
   */
  public Set<Mode> getRegisteredModesForTimePeriod(final TimePeriod timePeriod) {
    if (odDemands.containsKey(timePeriod.getId())) {
      return odDemands.get(timePeriod.getId()).keySet();
    } else {
      return null;
    }
  }

  /**
   * Return the id of this demand object
   *
   * @return id of this demand object
   */
  public long getId() {
    return this.id;
  }

  /**
   * Register UserClass
   * 
   * @param userClass the UserClass to be registered
   */
  public void registerUserClass(UserClass userClass) {
    userClassMap.put(userClass.getId(), userClass);
  }

  /**
   * Retrieve UserClass by its Id
   * 
   * @param id the Id of the UserClass
   * @return the retrieved UserClass
   */
  public UserClass getUserClassById(long id) {
    return userClassMap.get(id);
  }

  /**
   * Register TravelerType
   * 
   * @param travelerType the TravelerType to be registered
   */
  public void registerTravelerType(TravelerType travelerType) {
    travelerTypeMap.put(travelerType.getId(), travelerType);
  }

  /**
   * Retrieve TravelerType by its Id
   * 
   * @param id the Id of the TravelerType
   * @return the retrieved TravelerType
   */
  public TravelerType getTravelerTypeById(long id) {
    return travelerTypeMap.get(id);
  }

}
