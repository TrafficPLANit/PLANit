package org.goplanit.demands;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.component.PlanitComponent;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

/**
 * Container class for all demands registered on the project. In PlanIt we assume that all traffic flows between an origin and destination. Hence all demand for a given time period
 * and mode is provided between an origin and destination via ODDemand.
 *
 * Further, unlike other components, we let anyone register OdDemand compatible instances on this class to provide maximum flexibility in the underlying container since depending
 * on the od data different containers might be preferred for optimizing memory usage. Also not all OdDemand instances on the same Demands instance might utilize the same data
 * structure, hence the need to avoid a general approach across all entries within a Demands instance
 *
 * @author markr
 *
 */
public class Demands extends PlanitComponent<Demands> implements Serializable {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Demands.class.getCanonicalName());

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 144798248371260732L;

  /**
   * Trip demand matrices
   */
  protected final TreeMap<Long, TreeMap<Mode, OdDemands>> odDemandsByTimePeriodAndMode;

  /**
   * internal class instance containing all time periods on this demand instance
   */
  public final TimePeriods timePeriods;

  /**
   * internal class instance containing all user classes on this demand instance
   */
  public final UserClasses userClasses;

  /**
   * internal class instance containing all traveler types on this demand instance
   */
  public final TravelerTypes travelerTypes;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public Demands(IdGroupingToken groupId) {
    super(groupId, Demands.class);
    this.travelerTypes = new TravelerTypes(this);
    this.userClasses = new UserClasses(this);
    this.timePeriods = new TimePeriods(this);
    odDemandsByTimePeriodAndMode = new TreeMap<Long, TreeMap<Mode, OdDemands>>();
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Demands(Demands other, boolean deepCopy) {
    super(other, deepCopy);
    this.travelerTypes  = deepCopy ? other.travelerTypes.deepClone()  : other.travelerTypes.clone(); // container class so clone for copy
    this.userClasses    = deepCopy ? other.userClasses.deepClone()    : other.userClasses. clone();  // container class so clone for copy
    this.timePeriods    = deepCopy ? other.timePeriods.deepClone()    : other.timePeriods. clone();  // container class so clone for copy

    this.odDemandsByTimePeriodAndMode = new TreeMap<>();
    if(deepCopy) {
      for (var timePeriod : timePeriods) {
        var modes = other.getRegisteredModesForTimePeriod(timePeriod);
        for (var mode : modes) {
          OdDemands odDemandMatrix = other.get(mode, timePeriod);
          this.registerOdDemandPcuHour(
                  timePeriods.get(timePeriod.getId()), mode, odDemandMatrix.deepClone());
        }
      }
    }else{
      this.odDemandsByTimePeriodAndMode.putAll(other.odDemandsByTimePeriodAndMode);
    }
  }

  /**
   * Register provided odDemand
   *
   * @param timePeriod       the time period for this origin-demand object
   * @param mode             the mode for this origin-demand object
   * @param odDemandsPcuHour the origin-demand object to be registered in pcu/hour
   * @return oldOdDemand if there already existed an odDemand for the given mode and time period, the overwritten entry is returned
   */
  public OdDemands registerOdDemandPcuHour(final TimePeriod timePeriod, final Mode mode, final OdDemands odDemandsPcuHour) {
    if (!odDemandsByTimePeriodAndMode.containsKey(timePeriod.getId())) {
      odDemandsByTimePeriodAndMode.put(timePeriod.getId(), new TreeMap<Mode, OdDemands>());
    }
    return odDemandsByTimePeriodAndMode.get(timePeriod.getId()).put(mode, odDemandsPcuHour);
  }

  /**
   * Get an OdDemand by mode and time period in pcu/hour
   *
   * @param mode       the mode for which the ODDemand object is required
   * @param timePeriod the time period for which the ODDemand object is required
   * @return ODDemand object if found, otherwise null
   */

  public OdDemands get(final Mode mode, final TimePeriod timePeriod) {
    if (odDemandsByTimePeriodAndMode.containsKey(timePeriod.getId()) && odDemandsByTimePeriodAndMode.get(timePeriod.getId()).containsKey(mode)) {
      return odDemandsByTimePeriodAndMode.get(timePeriod.getId()).get(mode);
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
    if (odDemandsByTimePeriodAndMode.containsKey(timePeriod.getId())) {
      return odDemandsByTimePeriodAndMode.get(timePeriod.getId()).keySet();
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Demands clone() {
    return new Demands(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Demands deepClone() {
    return new Demands(this, true);
  }

  /**
   * reset all demands, traveler types, time periods and user classes
   */
  public void reset() {
    travelerTypes.clear();
    userClasses.clear();
    timePeriods.clear();
    odDemandsByTimePeriodAndMode.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    // TODO -> consider logging the traveler types, user classes, and time periods
    return null;
  }
}
