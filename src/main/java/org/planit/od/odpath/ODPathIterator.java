package org.planit.od.odpath;

import org.planit.network.virtual.Zoning;
import org.planit.od.ODDataIteratorImpl;
import org.planit.path.Path;

/**
 * Iterator which runs through rows and columns of a matrix of Path objects, making the Path, row
 * and column of each cell available
 * 
 * @author gman6028
 *
 */
public class ODPathIterator extends ODDataIteratorImpl<Path> {

  /**
   * array containing the Path object for each OD cell
   */
  private Path[][] matrixContents;

  /**
   * Constructor
   * 
   * @param matrixContents matrix of Path objects for each origin-destination cell
   * @param zones the zones in the current zoning
   */
  public ODPathIterator(Path[][] matrixContents, Zoning.Zones zones) {
    super(zones);
    this.matrixContents = matrixContents;
  }

  /**
   * Returns the path in the current cell
   * 
   * @return the Path in the current cell
   */
  @Override
  public Path getCurrentValue() {
    return matrixContents[originId][destinationId];
  }

  /**
   * Returns the path in the next cell and increments the current position
   * 
   * @return the path the next cell
   */
  @Override
  public Path next() {
    updateCurrentLocation();
    return matrixContents[originId][destinationId];
  }

}
