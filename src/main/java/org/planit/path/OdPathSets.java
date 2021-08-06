package org.planit.path;

import java.io.Serializable;
import java.util.TreeMap;

import org.planit.component.PlanitComponent;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * Contains one or more origin-destination based path sets that can be used in assignment. For now each individual path set takes on the form of the already available ODPathMatrix.
 * In future versions more flexible implementation are planned
 *
 * @author markr
 *
 */
public class OdPathSets extends PlanitComponent<OdPathSets> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -8742549499023004121L;

  /**
   * map holding all registered od path matrices by their unique id
   */
  protected final TreeMap<Long, ODPathMatrix> odPathMatrices;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public OdPathSets(IdGroupingToken groupId) {
    super(groupId, OdPathSets.class);
    this.odPathMatrices = new TreeMap<Long, ODPathMatrix>();
    ;
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public OdPathSets(OdPathSets other) {
    super(other);
    this.odPathMatrices = new TreeMap<Long, ODPathMatrix>(other.odPathMatrices);
  }

  /**
   * Collect the number of registered od path sets
   * 
   * @return number of od path sets
   */
  public int getNumberOfOdPathSets() {
    return odPathMatrices.size();
  }

  /**
   * Create an empty od path matrix and register it on this od path sets
   * 
   * @param zoning used to derive the size of the aquare zone based matrix
   * @return newly created od path matrix
   */
  public ODPathMatrix createAndRegisterOdPathMatrix(final Zoning zoning) {
    final ODPathMatrix newOdPathMatrix = new ODPathMatrix(getIdGroupingToken(), zoning.odZones);
    odPathMatrices.put(newOdPathMatrix.getId(), newOdPathMatrix);
    return newOdPathMatrix;
  }

  /**
   * register the passed in path matrix (not copied)
   * 
   * @param odPathMatrix to register
   */
  public void registerOdPathMatrix(final ODPathMatrix odPathMatrix) {
    odPathMatrices.put(odPathMatrix.getId(), odPathMatrix);
  }

  /**
   * verify if any od path matrices have been registered or not
   * 
   * @return true if any are registered, false otherwise
   */
  public Boolean hasRegisteredOdMatrices() {
    return !odPathMatrices.isEmpty();
  }

  /**
   * Collect the first od path matrix available
   * 
   * @return the first od path matrix available, if not available null is returned
   */
  public ODPathMatrix getFirstOdPathMatrix() {
    return hasRegisteredOdMatrices() ? odPathMatrices.firstEntry().getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathSets clone() {
    return new OdPathSets(this);
  }
}