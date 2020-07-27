package org.planit.route;

import java.util.TreeMap;

import org.planit.network.virtual.Zoning;
import org.planit.od.odroute.ODRouteMatrix;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * Contains one or more origin-destination based route sets that can be used in assignment. For now each individual route set takes on the form of the already available
 * ODPathMatrix. In future versions more flexible implementation are planned
 *
 * @author markr
 *
 */
public class ODRouteSets extends TrafficAssignmentComponent<ODRouteSets> {

  /** generated UID */
  private static final long serialVersionUID = -8742549499023004121L;

  /**
   * map holding all registered od route matrices by their unique id
   */
  protected final TreeMap<Long, ODRouteMatrix> odRouteMatrices = new TreeMap<Long, ODRouteMatrix>();

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public ODRouteSets(IdGroupingToken groupId) {
    super(groupId, ODRouteSets.class);
  }

  /**
   * Create an empty od route matrix and register it on this od route sets
   * 
   * @param zoning used to derive the size of the aquare zone based matrix
   * @return newly created od route matrix
   */
  public ODRouteMatrix createAndRegisterOdRouteMatrix(final Zoning zoning) {
    final ODRouteMatrix newOdRouteMatrix = new ODRouteMatrix(groupId, zoning.zones);
    odRouteMatrices.put(newOdRouteMatrix.getId(), newOdRouteMatrix);
    return newOdRouteMatrix;
  }

  /**
   * register the passed in route matrix (not copied)
   * 
   * @param odRouteMatrix to register
   */
  public void registerOdRouteMatrix(final ODRouteMatrix odRouteMatrix) {
    odRouteMatrices.put(odRouteMatrix.getId(), odRouteMatrix);
  }

  /**
   * verify if any od route matrices have been registered or not
   * 
   * @return true if any are registered, false otherwise
   */
  public Boolean hasRegisteredOdMatrices() {
    return !odRouteMatrices.isEmpty();
  }

  /**
   * Collect the first od route matrix available
   * 
   * @return the first od route matrix available, if not available null is returned
   */
  public ODRouteMatrix getFirstODRouteMatrix() {
    return hasRegisteredOdMatrices() ? odRouteMatrices.firstEntry().getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

}
