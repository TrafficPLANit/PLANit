package org.planit.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.djutils.event.EventListenerInterface;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.virtual.Zone;

/**
 * Listener which is automatically registered to the creation of any traffic assignment component for which it gets notified. @see
 * #TrafficicAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements EventListenerInterface {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(InputBuilderListener.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = 4223028100274802893L;

  /**
   * Map which stores which external node Ids corresponding to Nodes
   */
  private Map<Object, Node> nodeExternalIdToNodeMap;

  /**
   * Map which stores external link segment type Ids corresponding to link segment types
   */
  private Map<Object, MacroscopicLinkSegmentType> linkSegmentTypeExternalIdToLinkSegmentTypeMap;

  /**
   * Map which stores Mode external Ids corresponding to Modes
   */
  private Map<Object, Mode> modeExternalIdToModeMap;

  /**
   * Map which stores traveler type by external Id
   */
  protected Map<Object, TravelerType> travelerTypeExternalIdToTravelerTypeMap;

  /**
   * Map which stores user class by external Id
   */
  protected Map<Object, UserClass> userClassExternalIdToUserClassMap;

  /**
   * Map which stores time periods by external Id
   */
  private Map<Object, TimePeriod> timePeriodExternalIdToTimePeriodMap;

  /**
   * Map which stores zones by external Id
   */
  private Map<Object, Zone> zoneExternalIdToZoneMap;

  /**
   * Map which stores link segments by external Id
   */
  private Map<Object, MacroscopicLinkSegment> linkSegmentExternalIdToLinkSegmentMap;

  /**
   * Flag to determine whether duplicate external Id should be considered an error (defaults to true)
   */
  private boolean errorIfDuplicateExternalId;

  /**
   * Stores an object by its external Id, after checking whether the external Id is a duplicate
   * 
   * @param <T>        type of object being stored
   * @param externalId external Id of object being stored
   * @param obj        object being stored
   * @param map        Map to store the object
   * @param objectName name of the object class
   * @return true if this entry is duplicate use of an externalId, false otherwise
   */
  private <T> boolean addObjectToExternalIdMap(Object externalId, T obj, Map<Object, T> map, String objectName) {
    boolean containsDuplicates = map.containsKey(externalId);
    map.put(externalId, obj);
    return containsDuplicates;
  }

  /**
   * Constructor
   */
  public InputBuilderListener() {
    nodeExternalIdToNodeMap = new HashMap<Object, Node>();
    linkSegmentTypeExternalIdToLinkSegmentTypeMap = new HashMap<Object, MacroscopicLinkSegmentType>();
    modeExternalIdToModeMap = new HashMap<Object, Mode>();
    travelerTypeExternalIdToTravelerTypeMap = new HashMap<Object, TravelerType>();
    userClassExternalIdToUserClassMap = new HashMap<Object, UserClass>();
    timePeriodExternalIdToTimePeriodMap = new HashMap<Object, TimePeriod>();
    zoneExternalIdToZoneMap = new HashMap<Object, Zone>();
    linkSegmentExternalIdToLinkSegmentMap = new HashMap<Object, MacroscopicLinkSegment>();
    errorIfDuplicateExternalId = true;
  }

  /**
   * Return a node for a specified external Id
   * 
   * @param externalId the external Id
   * @return node corresponding to the specified external Id
   */
  public Node getNodeByExternalId(Object externalId) {
    return nodeExternalIdToNodeMap.get(externalId);
  }

  /**
   * Stores a node by its external Id
   * 
   * @param externalId external Id of node
   * @param node       Node to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addNodeToExternalIdMap(Object externalId, Node node) {
    return addObjectToExternalIdMap(externalId, node, nodeExternalIdToNodeMap, "node");
  }

  /**
   * Return the link segment type for a specified external Id
   * 
   * @param externalId the external Id
   * @return the link segment type corresponding to the specified external Id
   */
  public MacroscopicLinkSegmentType getLinkSegmentTypeByExternalId(Object externalId) {
    return linkSegmentTypeExternalIdToLinkSegmentTypeMap.get(externalId);
  }

  /**
   * Stores a link segment type by its external Id
   * 
   * @param externalId                 external Id of link segment type
   * @param macroscopicLinkSegmentType to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addLinkSegmentTypeToExternalIdMap(Object externalId, MacroscopicLinkSegmentType macroscopicLinkSegmentType) {
    return addObjectToExternalIdMap(externalId, macroscopicLinkSegmentType, linkSegmentTypeExternalIdToLinkSegmentTypeMap, "link segment type");
  }

  /**
   * Return Mode for a specified external Id
   * 
   * @param externalId the specified external Id
   * @return mode corresponding to specified Id
   */
  public Mode getModeByExternalId(Object externalId) {
    return modeExternalIdToModeMap.get(externalId);
  }

  /**
   * Return all the registered modes
   * 
   * @return collection of registered modes
   */
  public Collection<Mode> getAllModes() {
    return modeExternalIdToModeMap.values();
  }

  /**
   * Stores a mode by its external Id
   * 
   * @param externalId external Id of this mode
   * @param mode       mode to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addModeToExternalIdMap(Object externalId, Mode mode) {
    return addObjectToExternalIdMap(externalId, mode, modeExternalIdToModeMap, "mode");
  }

  /**
   * Return traveler type for a specified external Id
   * 
   * @param externalId the external Id
   * @return the traveler type for the specified external Id
   */
  public TravelerType getTravelerTypeByExternalId(Object externalId) {
    return travelerTypeExternalIdToTravelerTypeMap.get(externalId);
  }

  /**
   * Stores a traveler type by its external Id
   * 
   * @param externalId   external Id of traveler type
   * @param travelerType traveler type to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addTravelerTypeToExternalIdMap(Object externalId, TravelerType travelerType) {
    return addObjectToExternalIdMap(externalId, travelerType, travelerTypeExternalIdToTravelerTypeMap, "traveller type");
  }

  /**
   * Return user class by external Id
   * 
   * @param externalId externalId of user class
   * @return specified user class
   */
  public UserClass getUserClassByExternalId(Object externalId) {
    return userClassExternalIdToUserClassMap.get(externalId);
  }

  /**
   * Stores a user class by its external Id
   * 
   * @param externalId external Id of user class
   * @param userClass  user class to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addUserClassToExternalIdMap(Object externalId, UserClass userClass) {
    return addObjectToExternalIdMap(externalId, userClass, userClassExternalIdToUserClassMap, "user class");
  }

  /**
   * Return the time period for a specified external Id
   * 
   * @param externalId external Id of time period
   * @return the specified time period
   */
  public TimePeriod getTimePeriodByExternalId(Object externalId) {
    return timePeriodExternalIdToTimePeriodMap.get(externalId);
  }

  /**
   * Returns whether a time period external Id matches set of external Ids for registered time periods
   * 
   * @param externalId the external time period Id being tested
   * @return true if the external Id matches a registered time period, false otherwise
   */
  public boolean isTimePeriodExternalIdRegistered(Object externalId) {
    return timePeriodExternalIdToTimePeriodMap.keySet().contains(externalId);
  }

  /**
   * Returns the number of registered time periods
   * 
   * @return the number of registered time periods
   */
  public int getNumberOfRegisteredTimePeriods() {
    return timePeriodExternalIdToTimePeriodMap.keySet().size();
  }

  /**
   * Returns a list of external ids of time periods
   * 
   * @return list of external ids of time periods
   */
  public List<Object> getTimePeriodExternalIds() {
    return new ArrayList<Object>(timePeriodExternalIdToTimePeriodMap.keySet());
  }

  /**
   * Stores a time period by its external Id
   * 
   * @param externalId external Id of time period
   * @param timePeriod time period to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addTimePeriodToExternalIdMap(Object externalId, TimePeriod timePeriod) {
    return addObjectToExternalIdMap(externalId, timePeriod, timePeriodExternalIdToTimePeriodMap, "time period");
  }

  /**
   * Returns the zone for a specified external Id
   * 
   * @param externalId the external Id
   * @return the zone corresponding to this external Id
   */
  public Zone getZoneByExternalId(Object externalId) {
    return zoneExternalIdToZoneMap.get(externalId);
  }

  /**
   * Stores a zone by its external Id
   * 
   * @param externalId external Id of zone
   * @param zone       zone to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addZoneToExternalIdMap(Object externalId, Zone zone) {
    return addObjectToExternalIdMap(externalId, zone, zoneExternalIdToZoneMap, "zone");
  }

  /**
   * Returns the link segment for a given external Id
   * 
   * @param externalId external Id of the link segment
   * @return the specified link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByExternalId(Object externalId) {
    return linkSegmentExternalIdToLinkSegmentMap.get(externalId);
  }

  /**
   * Stores a link segment by its external Id
   * 
   * @param externalId  external Id of link segment
   * @param linkSegment link segment to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addLinkSegmentToExternalIdMap(Object externalId, MacroscopicLinkSegment linkSegment) {
    return addObjectToExternalIdMap(externalId, linkSegment, linkSegmentExternalIdToLinkSegmentMap, "link segment");
  }

  /**
   * Return whether input files having duplicate external Ids should be treated as an error
   * 
   * @return true if duplicate Ids considered an error, false otherwise
   */
  public boolean isErrorIfDuplicateExternalId() {
    return errorIfDuplicateExternalId;
  }

  /**
   * Set whether input files having duplicate external Ids should be treated as an error
   * 
   * @param errorIfDuplicateExternalId true if duplicate Ids considered an error, false otherwise
   */
  public void setErrorIfDuplicateExternalId(boolean errorIfDuplicateExternalId) {
    this.errorIfDuplicateExternalId = errorIfDuplicateExternalId;
  }

}
