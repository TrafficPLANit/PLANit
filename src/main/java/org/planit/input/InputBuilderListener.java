package org.planit.input;

import java.util.ArrayList;
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
   * Map which stores which xml node Ids corresponding to Nodes
   */
  protected Map<String, Node> xmlIdNodeMap;

  /**
   * Map which stores link segments by xml Id
   */
  protected Map<String, MacroscopicLinkSegment> xmlIdLinkSegmentMap;

  /**
   * Map which stores xml link segment type Ids corresponding to link segment types
   */
  protected Map<String, MacroscopicLinkSegmentType> xmlIdLinkSegmentTypeMap;

  /**
   * Map which stores Mode xml Ids corresponding to Modes
   */
  protected Map<String, Mode> xmlIdModeMap;

  /**
   * Map which stores traveler type by xml Id
   */
  protected Map<String, TravelerType> xmlIdTravelerTypeMap;

  /**
   * Map which stores user class by xml Id
   */
  protected Map<String, UserClass> xmlIdUserClassMap;

  /**
   * Map which stores time periods by xml Id
   */
  protected Map<String, TimePeriod> xmlIdTimePeriodMap;

  /**
   * Map which stores zones by xml Id
   */
  protected Map<String, Zone> xmlIdZoneMap;

  /**
   * Flag to determine whether duplicate xml Id should be considered an error (defaults to true)
   */
  private boolean errorIfDuplicateXmlId = DEFAULT_ERROR_ON_DUPLICATE_XML_ID;

  /**
   * Stores an object by its xml Id, after checking whether the external Id is a duplicate
   * 
   * @param <T>        type of object being stored
   * @param xmlId      xmlId of object being stored
   * @param obj        object being stored
   * @param map        Map to store the object
   * @param objectName name of the object class
   * @return true if this entry is duplicate use of an xml id, false otherwise
   */
  private <T> boolean addObjectToXmlIdMap(String xmlId, T obj, Map<String, T> map, String objectName) {
    boolean containsDuplicates = map.containsKey(xmlId);
    map.put(xmlId, obj);
    return containsDuplicates;
  }

  /** default setting */
  public static boolean DEFAULT_ERROR_ON_DUPLICATE_XML_ID = true;

  /**
   * Constructor
   */
  public InputBuilderListener() {
    xmlIdNodeMap = new HashMap<String, Node>();
    xmlIdLinkSegmentTypeMap = new HashMap<String, MacroscopicLinkSegmentType>();
    xmlIdModeMap = new HashMap<String, Mode>();
    xmlIdTravelerTypeMap = new HashMap<String, TravelerType>();
    xmlIdUserClassMap = new HashMap<String, UserClass>();
    xmlIdTimePeriodMap = new HashMap<String, TimePeriod>();
    xmlIdZoneMap = new HashMap<String, Zone>();
    xmlIdLinkSegmentMap = new HashMap<String, MacroscopicLinkSegment>();
  }

  /**
   * Return a node for a specified xml Id
   * 
   * @param xmlId the external Id
   * @return node corresponding to the specified external Id
   */
  public Node getNodeByXmlId(String xmlId) {
    return xmlIdNodeMap.get(xmlId);
  }

  /**
   * Stores a node by its xmlId Id
   * 
   * @param xmlId xml Id of node
   * @param node  Node to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addNodeToXmlIdMap(String xmlId, Node node) {
    return addObjectToXmlIdMap(xmlId, node, xmlIdNodeMap, "node");
  }

  /**
   * Return the link segment type for a specified xmlId Id
   * 
   * @param xmlId the xml Id
   * @return the link segment type corresponding to the specified xmlId
   */
  public MacroscopicLinkSegmentType getLinkSegmentTypeByXmlId(String xmlId) {
    return xmlIdLinkSegmentTypeMap.get(xmlId);
  }

  /**
   * Stores a link segment type by its xmlId
   * 
   * @param xmlId                      xml Id of link segment type
   * @param macroscopicLinkSegmentType to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addLinkSegmentTypeToXmlIdMap(String xmlId, MacroscopicLinkSegmentType macroscopicLinkSegmentType) {
    return addObjectToXmlIdMap(xmlId, macroscopicLinkSegmentType, xmlIdLinkSegmentTypeMap, "link segment type");
  }

  /**
   * Return Mode for a specified xml Id
   * 
   * @param xmlId the specified external Id
   * @return mode corresponding to specified Id
   */
  public Mode getModeByXmlId(String xmlId) {
    return xmlIdModeMap.get(xmlId);
  }

  /**
   * Return all the registered modes
   * 
   * @return collection of registered modes
   */
  public Map<String, Mode> getAllModesByXmlId() {
    return xmlIdModeMap;
  }

  /**
   * Stores a mode by its xmlId Id
   * 
   * @param xmlId xml Id of this mode
   * @param mode  mode to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addModeToXmlIdMap(String xmlId, Mode mode) {
    return addObjectToXmlIdMap(xmlId, mode, xmlIdModeMap, "mode");
  }

  /**
   * Return traveler type for a specified xmlId
   * 
   * @param xmlId the xml Id
   * @return the traveler type for the specified xmlId
   */
  public TravelerType getTravelerTypeByXmlId(String xmlId) {
    return xmlIdTravelerTypeMap.get(xmlId);
  }

  /**
   * Stores a traveler type by its xmlId
   * 
   * @param xmlId        xmlId of traveler type
   * @param travelerType traveler type to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addTravelerTypeToXmlIdMap(String xmlId, TravelerType travelerType) {
    return addObjectToXmlIdMap(xmlId, travelerType, xmlIdTravelerTypeMap, "traveller type");
  }

  /**
   * Return user class by external Id
   * 
   * @param xmlId xmlId of user class
   * @return specified user class
   */
  public UserClass getUserClassByXmlId(String xmlId) {
    return xmlIdUserClassMap.get(xmlId);
  }

  /**
   * Stores a user class by its xmlId
   * 
   * @param xmlId     xmlId of user class
   * @param userClass user class to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addUserClassToXmlIdMap(String xmlId, UserClass userClass) {
    return addObjectToXmlIdMap(xmlId, userClass, xmlIdUserClassMap, "user class");
  }

  /**
   * Return the time period for a specified xmlId
   * 
   * @param xmlId xmlId of time period
   * @return the specified time period
   */
  public TimePeriod getTimePeriodByXmlId(String xmlId) {
    return xmlIdTimePeriodMap.get(xmlId);
  }

  /**
   * Returns whether a time period for a given xmlId exists
   * 
   * @param xmlId the xmlId time period being tested
   * @return true if the external Id matches a registered time period, false otherwise
   */
  public boolean isTimePeriodXmlIdRegistered(String xmlId) {
    return xmlIdTimePeriodMap.keySet().contains(xmlId);
  }

  /**
   * Returns the number of registered time periods
   * 
   * @return the number of registered time periods
   */
  public int getNumberOfRegisteredTimePeriods() {
    return xmlIdTimePeriodMap.keySet().size();
  }

  /**
   * Returns a list of external ids of time periods
   * 
   * @return list of external ids of time periods
   */
  public List<String> getTimePeriodXmlIds() {
    return new ArrayList<String>(xmlIdTimePeriodMap.keySet());
  }

  /**
   * Stores a time period by its xmlId
   * 
   * @param xmlId      xmlId of time period
   * @param timePeriod time period to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addTimePeriodToXmlIdMap(String xmlId, TimePeriod timePeriod) {
    return addObjectToXmlIdMap(xmlId, timePeriod, xmlIdTimePeriodMap, "time period");
  }

  /**
   * Returns the zone for a specified xmlId
   * 
   * @param xmlId the xmlId
   * @return the zone corresponding to this xmlId
   */
  public Zone getZoneByXmlId(String xmlId) {
    return xmlIdZoneMap.get(xmlId);
  }

  /**
   * Stores a zone by its xmlId
   * 
   * @param xmlId xmlId of zone
   * @param zone  zone to be stored
   * @return true if this use of xmlId is a duplicate, false otherwise
   */
  public boolean addZoneToXmlIdMap(String xmlId, Zone zone) {
    return addObjectToXmlIdMap(xmlId, zone, xmlIdZoneMap, "zone");
  }

  /**
   * Returns the link segment for a given xmlId
   * 
   * @param xmlId xmlId of the link segment
   * @return the specified link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByXmlId(String xmlId) {
    return xmlIdLinkSegmentMap.get(xmlId);
  }

  /**
   * Stores a link segment by its xmlId
   * 
   * @param xmlId       xmlId of link segment
   * @param linkSegment link segment to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  public boolean addLinkSegmentToXmlIdMap(String xmlId, MacroscopicLinkSegment linkSegment) {
    return addObjectToXmlIdMap(xmlId, linkSegment, xmlIdLinkSegmentMap, "link segment");
  }

  /**
   * Return whether input files having duplicate xmlIds should be treated as an error
   * 
   * @return true if duplicate Ids considered an error, false otherwise
   */
  public boolean isErrorIfDuplicateXmlId() {
    return errorIfDuplicateXmlId;
  }

  /**
   * Set whether input files having duplicate xmlIds should be treated as an error
   * 
   * @param errorIfDuplicateXmlId true if duplicate Ids considered an error, false otherwise
   */
  public void setErrorIfDuplicateXmlId(boolean errorIfDuplicateXmlId) {
    this.errorIfDuplicateXmlId = errorIfDuplicateXmlId;
  }

}
