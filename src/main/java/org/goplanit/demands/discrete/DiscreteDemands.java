package org.goplanit.demands.discrete;

import org.goplanit.component.PlanitComponent;
import org.goplanit.demands.TimePeriods;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.household.Households;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.time.TimePeriod;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Container class for all discrete demands (agents). For now this is only used for conversion support rather than
 * directly as part of the more macroscopic oriented assignment in PLANit, but the idea is it will also allow for
 * conversion from discrete to continuous or macroscopic demands that are used in PLANit already through the normal
 * demands setup
 * <p>
 * In future we can contemplate supporting this directly also for assignment
 * </p>
 *
 * @author markr
 *
 */
public class DiscreteDemands extends PlanitComponent<DiscreteDemands> implements Serializable {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DiscreteDemands.class.getCanonicalName());

  // Protected

  /** time periods */
  protected final TimePeriods timePeriods;

  /**  the households */
  protected final Households households;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public DiscreteDemands(IdGroupingToken groupId) {
    super(groupId, DiscreteDemands.class);
    this.timePeriods = new TimePeriods(groupId);
    this.households = new Households(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public DiscreteDemands(DiscreteDemands other, boolean deepCopy) {
    super(other, deepCopy);

    if(deepCopy) {
      var timePeriodMapper = new ManagedIdDeepCopyMapper<TimePeriod>();
      var householdMapper = new ManagedIdDeepCopyMapper<Household>();

      this.timePeriods = other.timePeriods.deepCloneWithMapping(timePeriodMapper);
      this.households = other.households.deepCloneWithMapping(householdMapper);

      /* DISCRETE DEMANDS */
      // todo

    }else{
      this.timePeriods    = other.timePeriods.shallowClone();
      this.households    = other.households.shallowClone();

      /* DISCRETE DEMANDS */
      // todo
    }

  }


  /**
   * Access to the time periods
   * @return time periods container
   */
  public TimePeriods getTimePeriods(){
    return timePeriods;
  }

  /**
   * Access to the households
   * @return households container
   */
  public Households getHouseholds(){
    return households;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemands shallowClone() {
    return new DiscreteDemands(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemands deepClone() {
    return new DiscreteDemands(this, true);
  }

  /**
   * Log general information on this demands to the user
   *
   * @param prefix to use
   */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s#time periods: %d", prefix, timePeriods.size()));
    //todo
  }

  /**
   * reset all demands, traveler types, time periods and user classes
   */
  public void reset() {
    timePeriods.clear();
    //todo
  }

  /*
  * {@inheritDoc}
  */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    //todo
    return null;
  }

}
