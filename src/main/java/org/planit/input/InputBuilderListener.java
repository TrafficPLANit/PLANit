package org.planit.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.djutils.event.EventListenerInterface;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
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
   * Map which stores which source node Ids corresponding to Nodes (can be native xml id, or some external thrid party source id)
   */
  protected Map<String, Node> sourceIdNodeMap;

  /**
   * Map which stores link segments by source Id
   */
  protected Map<String, MacroscopicLinkSegment> sourceIdLinkSegmentMap;

  /**
   * Map which stores source link segment type Ids corresponding to link segment types
   */
  protected Map<String, MacroscopicLinkSegmentType> sourceIdLinkSegmentTypeMap;

  /**
   * Map which stores Mode source Ids corresponding to Modes
   */
  protected Map<String, Mode> sourceIdModeMap;

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
   * Map which stores zones by source Id
   */
  protected Map<String, Zone> sourceIdZoneMap;

  /**
   * Flag to determine whether duplicate source Id should be considered an error (defaults to true)
   */
  private boolean errorIfDuplicateSourceId = DEFAULT_ERROR_ON_DUPLICATE_SOURCE_ID;

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   * 
   * @param <T>        type of object being stored
   * @param sourceId   sourceId of object being stored
   * @param obj        object being stored
   * @param map        Map to store the object
   * @param objectName name of the object class
   * @return true if this entry is duplicate use of an xml id, false otherwise
   */
  private <T> boolean addObjectToSourceIdMap(String sourceId, T obj, Map<String, T> map, String objectName) {
    boolean containsDuplicates = map.containsKey(sourceId);
    map.put(sourceId, obj);
    return containsDuplicates;
  }
  
  // protected
  
  /**
   * Stores a node by its sourceId
   * 
   * @param sourceId source Id of node
   * @param node     Node to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  protected boolean addNodeToSourceIdMap(String sourceId, Node node) {
    return addObjectToSourceIdMap(sourceId, node, sourceIdNodeMap, "node");
  }
  
  
  /**
   * Stores a mode by its sourceId Id
   * 
   * @param sourceId xml Id of this mode
   * @param mode     mode to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addModeToSourceIdMap(String sourceId, Mode mode) {
    return addObjectToSourceIdMap(sourceId, mode, sourceIdModeMap, "mode");
  }  

  /**
   * Stores a traveler type by its sourceId
   * 
   * @param sourceId     sourceId of traveler type
   * @param travelerType traveler type to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addTravelerTypeToSourceIdMap(String sourceId, TravelerType travelerType) {
    return addObjectToSourceIdMap(sourceId, travelerType, sourceIdTravelerTypeMap, "traveller type");
  }  

  /**
   * Stores a user class by its sourceId
   * 
   * @param sourceId  sourceId of user class
   * @param userClass user class to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addUserClassToSourceIdMap(String sourceId, UserClass userClass) {
    return addObjectToSourceIdMap(sourceId, userClass, sourceIdUserClassMap, "user class");
  }  

  /**
   * Stores a time period by its sourceId
   * 
   * @param sourceId   sourceId of time period
   * @param timePeriod time period to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addTimePeriodToSourceIdMap(String sourceId, TimePeriod timePeriod) {
    return addObjectToSourceIdMap(sourceId, timePeriod, sourceIdTimePeriodMap, "time period");
  }  

  /**
   * Stores a zone by its sourceId
   * 
   * @param sourceId sourceId of zone
   * @param zone     zone to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addZoneToSourceIdMap(String sourceId, Zone zone) {
    return addObjectToSourceIdMap(sourceId, zone, sourceIdZoneMap, "zone");
  }  
  
  /**
   * Stores a link segment by its sourceId
   * 
   * @param sourceId    sourceId of link segment
   * @param linkSegment link segment to be stored
   * @return true if this use of externalId is a duplicate, false otherwise
   */
  protected boolean addLinkSegmentToSourceIdMap(String sourceId, MacroscopicLinkSegment linkSegment) {
    return addObjectToSourceIdMap(sourceId, linkSegment, sourceIdLinkSegmentMap, "link segment");
  }  

  /** default setting */
  public static boolean DEFAULT_ERROR_ON_DUPLICATE_SOURCE_ID = true;

  /**
   * Constructor
   */
  public InputBuilderListener() {
    sourceIdNodeMap = new HashMap<String, Node>();
    sourceIdLinkSegmentTypeMap = new HashMap<String, MacroscopicLinkSegmentType>();
    sourceIdModeMap = new HashMap<String, Mode>();
    sourceIdTravelerTypeMap = new HashMap<String, TravelerType>();
    sourceIdUserClassMap = new HashMap<String, UserClass>();
    sourceIdTimePeriodMap = new HashMap<String, TimePeriod>();
    sourceIdZoneMap = new HashMap<String, Zone>();
    sourceIdLinkSegmentMap = new HashMap<String, MacroscopicLinkSegment>();
  }

  /**
   * Return a node for a specified sourceId Id
   * 
   * @param sourceId the external Id
   * @return node corresponding to the specified external Id
   */
  public Node getNodeByXmlId(String sourceId) {
    return sourceIdNodeMap.get(sourceId);
  }

  /**
   * Return the link segment type for a specified sourceId
   * 
   * @param sourceId the source Id
   * @return the link segment type corresponding to the specified sourceId
   */
  public MacroscopicLinkSegmentType getLinkSegmentTypeBySourceId(String sourceId) {
    return sourceIdLinkSegmentTypeMap.get(sourceId);
  }

  /**
   * Return Mode for a specified xml Id
   * 
   * @param sourceId the specified external Id
   * @return mode corresponding to specified Id
   */
  public Mode getModeBySourceId(String sourceId) {
    return sourceIdModeMap.get(sourceId);
  }

  /**
   * Return all the registered modes
   * 
   * @return collection of registered modes
   */
  public Map<String, Mode> getAllModesBySourceId() {
    return sourceIdModeMap;
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
   * Stores a link segment type by its sourceId
   * 
   * @param sourceId                   xml Id of link segment type
   * @param macroscopicLinkSegmentType to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  protected boolean addLinkSegmentTypeToSourceIdMap(String sourceId, MacroscopicLinkSegmentType macroscopicLinkSegmentType) {
    return addObjectToSourceIdMap(sourceId, macroscopicLinkSegmentType, sourceIdLinkSegmentTypeMap, "link segment type");
  }

  /**
   * Return user class by external Id
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
   * Returns a list of external ids of time periods
   * 
   * @return list of external ids of time periods
   */
  public List<String> getTimePeriodSourceIds() {
    return new ArrayList<String>(sourceIdTimePeriodMap.keySet());
  }
  
  /**
   * Returns the zone for a specified sourceId
   * 
   * @param sourceId the sourceId
   * @return the zone corresponding to this sourceId
   */
  public Zone getZoneBySourceId(String sourceId) {
    return sourceIdZoneMap.get(sourceId);
  }

  /**
   * Returns the link segment for a given sourceId
   * 
   * @param sourceId sourceId of the link segment
   * @return the specified link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByXmlId(String sourceId) {
    return sourceIdLinkSegmentMap.get(sourceId);
  }

  /**
   * returns the link segment by a given external id Extremely slow, because it is not indexed at the moment
   * 
   * @param network    to look in
   * @param externalId to look for
   * @return link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByExternalId(InfrastructureNetwork network, String externalId) {
    for (InfrastructureLayer layer : network.infrastructureLayers) {
      if (layer instanceof MacroscopicPhysicalNetwork) {
        MacroscopicLinkSegment linkSegment = ((MacroscopicPhysicalNetwork) layer).linkSegments.getByExternalId(externalId);
        if (linkSegment != null) {
          return linkSegment;
        }
      }
    }
    return null;
  }

  /**
   * Return whether input files having duplicate sourceIds should be treated as an error
   * 
   * @return true if duplicate Ids considered an error, false otherwise
   */
  public boolean isErrorIfDuplicateSourceId() {
    return errorIfDuplicateSourceId;
  }

  /**
   * Set whether input files having duplicate sourceIds should be treated as an error
   * 
   * @param errorIfDuplicateSourceId true if duplicate Ids considered an error, false otherwise
   */
  public void setErrorIfDuplicateSourceId(boolean errorIfDuplicateSourceId) {
    this.errorIfDuplicateSourceId = errorIfDuplicateSourceId;
  }

}
