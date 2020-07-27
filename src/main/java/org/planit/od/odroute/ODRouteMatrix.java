package org.planit.od.odroute;

import org.planit.network.virtual.Zoning.Zones;
import org.planit.od.ODDataImpl;
import org.planit.route.Route;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.Zone;

/**
 * This class stores the route objects from each origin to each destination.
 *
 * @author gman6028
 *
 */
public class ODRouteMatrix extends ODDataImpl<Route> {

  /**
   * Array storing path for each origin-destination pair
   */
  private final Route[][] matrixContents;

  /**
   * Unique identifier
   */
  protected final long id;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param zones   the zones being used
   */
  public ODRouteMatrix(final IdGroupingToken groupId, final Zones zones) {
    super(zones);
    this.id = IdGenerator.generateId(groupId, ODRouteMatrix.class);
    final int numberOfTravelAnalysisZones = zones.getNumberOfZones();
    matrixContents = new Route[numberOfTravelAnalysisZones][numberOfTravelAnalysisZones];
  }

  /**
   * Returns the path for a specified origin and destination
   *
   * @param origin      the specified origin zone
   * @param destination the specified destination zone
   * @return the path from the origin to the destination
   */
  @Override
  public Route getValue(final Zone origin, final Zone destination) {
    final int originId = (int) origin.getId();
    final int destinationId = (int) destination.getId();
    return matrixContents[originId][destinationId];
  }

  /**
   * Set the path from a specified origin to a specified destination
   *
   * @param origin      the specified origin zone
   * @param destination the specified destination zone
   * @param path        the Path object from the origin to the destination
   *
   */
  @Override
  public void setValue(final Zone origin, final Zone destination, final Route path) {
    final int originId = (int) origin.getId();
    final int destinationId = (int) destination.getId();
    matrixContents[originId][destinationId] = path;
  }

  /**
   * Returns an iterator which can iterate through all the origin-destination cells in the matrix
   *
   * @return iterator through all the origin-destination cells
   */
  @Override
  public ODRouteIterator iterator() {
    return new ODRouteIterator(matrixContents, zones);
  }

  // getters - setters

  /**
   * @return unique identifier of this route od route matrix instance
   */
  public long getId() {
    return this.id;
  }

}
