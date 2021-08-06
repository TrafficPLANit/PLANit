package org.planit.converter.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;

/**
 * A network reader implementation with built-in convenience containers that maps ids used by the external data source to relate entities to each other to the created PLANit
 * entries.
 * 
 * @author markr
 *
 */
public abstract class NetworkReaderImpl implements NetworkReader {

  /**
   * Map which stores which source node Ids corresponding to PLANit nodes
   */
  protected final Map<String, Node> sourceIdNodeMap;

  /**
   * Map which stores which source link Ids correspond to PLANit links
   */
  protected final Map<String, Link> sourceIdLinkMap;

  /**
   * Map which stores link segments by source Id
   */
  protected final Map<String, MacroscopicLinkSegment> sourceIdLinkSegmentMap;

  /**
   * Map which stores source link segment type Ids corresponding to PLANit link segment types
   */
  protected final Map<String, MacroscopicLinkSegmentType> sourceIdLinkSegmentTypeMap;

  /**
   * Map which stores Mode source Ids corresponding to PLANit Modes
   */
  protected final Map<String, Mode> sourceIdModeMap;

  /**
   * Stores an object by its source Id, after checking whether the external Id is a duplicate
   * 
   * @param <T>      type of object being stored
   * @param sourceId sourceId of object being stored
   * @param obj      object being stored
   * @param map      Map to store the object
   * @return true if this entry is duplicate use of an xml id, false otherwise
   */
  protected static <T> boolean addObjectToSourceIdMap(final String sourceId, final T obj, final Map<String, T> map) {
    boolean containsDuplicates = map.containsKey(sourceId);
    map.put(sourceId, obj);
    return containsDuplicates;
  }

  /**
   * Constructor
   */
  protected NetworkReaderImpl() {
    this.sourceIdNodeMap = new HashMap<String, Node>();
    this.sourceIdLinkSegmentTypeMap = new HashMap<String, MacroscopicLinkSegmentType>();
    this.sourceIdModeMap = new HashMap<String, Mode>();
    this.sourceIdLinkMap = new HashMap<String, Link>();
    this.sourceIdLinkSegmentMap = new HashMap<String, MacroscopicLinkSegment>();
  }

  /**
   * Stores a node by its sourceId
   * 
   * @param sourceId source Id of node
   * @param node     Node to be stored
   * @return true if this is a duplicate, false otherwise
   */
  public boolean addNodeToSourceIdMap(String sourceId, Node node) {
    return addObjectToSourceIdMap(sourceId, node, sourceIdNodeMap);
  }

  /**
   * Stores a node by its sourceId
   * 
   * @param sourceId source Id of link
   * @param link     Link to be stored
   * @return true if this is a duplicate, false otherwise
   */
  public boolean addLinkToSourceIdMap(String sourceId, Link link) {
    return addObjectToSourceIdMap(sourceId, link, sourceIdLinkMap);
  }

  /**
   * Stores a mode by its sourceId
   * 
   * @param sourceId of this mode
   * @param mode     mode to be stored
   * @return true if this is a duplicate, false otherwise
   */
  public boolean addModeToSourceIdMap(String sourceId, Mode mode) {
    return addObjectToSourceIdMap(sourceId, mode, sourceIdModeMap);
  }

  /**
   * Stores a link segment by its sourceId
   * 
   * @param sourceId    sourceId of link segment
   * @param linkSegment link segment to be stored
   * @return true if this is a duplicate, false otherwise
   */
  public boolean addLinkSegmentToSourceIdMap(String sourceId, MacroscopicLinkSegment linkSegment) {
    return addObjectToSourceIdMap(sourceId, linkSegment, sourceIdLinkSegmentMap);
  }

  /**
   * Stores a link segment type by its sourceId
   * 
   * @param sourceId                   source Id of link segment type
   * @param macroscopicLinkSegmentType to be stored
   * @return true if this use of sourceId is a duplicate, false otherwise
   */
  public boolean addLinkSegmentTypeToSourceIdMap(String sourceId, MacroscopicLinkSegmentType macroscopicLinkSegmentType) {
    return addObjectToSourceIdMap(sourceId, macroscopicLinkSegmentType, sourceIdLinkSegmentTypeMap);
  }

  /**
   * Return a node for a specified sourceId
   * 
   * @param sourceId the external Id
   * @return node corresponding to the specified sourceId
   */
  public Node getNodeBySourceId(String sourceId) {
    return sourceIdNodeMap.get(sourceId);
  }

  /**
   * Return all the registered nodes (unmodifiable)
   * 
   * @return collection of registered nodes
   */
  public Map<String, Node> getAllNodesBySourceId() {
    return Collections.unmodifiableMap(sourceIdNodeMap);
  }

  /**
   * Return all the registered links by source id rather than internal id (unmodifiable)
   * 
   * @return collection of registered links
   */
  public Map<String, Link> getAllLinksBySourceId() {
    return Collections.unmodifiableMap(sourceIdLinkMap);
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
   * Return Mode for a specified source Id
   * 
   * @param sourceId the specified external Id
   * @return mode corresponding to specified Id
   */
  public Mode getModeBySourceId(String sourceId) {
    return sourceIdModeMap.get(sourceId);
  }

  /**
   * Return all the registered modes (unmodifiable)
   * 
   * @return collection of registered modes
   */
  public Map<String, Mode> getAllModesBySourceId() {
    return Collections.unmodifiableMap(sourceIdModeMap);
  }

  /**
   * Returns the link segment for a given sourceId
   * 
   * @param sourceId sourceId of the link segment
   * @return the specified link segment
   */
  public MacroscopicLinkSegment getLinkSegmentBySourceId(String sourceId) {
    return sourceIdLinkSegmentMap.get(sourceId);
  }

  /**
   * Return all the registered link segments (unmodifiable)
   * 
   * @return collection of registered link segments
   */
  public Map<String, MacroscopicLinkSegment> getAllLinkSegmentsBySourceId() {
    return Collections.unmodifiableMap(sourceIdLinkSegmentMap);
  }

}
