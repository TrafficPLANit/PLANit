package org.planit.network;

import java.util.Set;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;

/**
 * An topological layer represents a layer suited for a number of modes containing of nodes and links. Since it is topologically meaningful it can have a coordinate reference
 * system as well.
 * 
 * @author markr
 *
 */
public interface TopologicalLayer extends TransportLayer {

  /**
   * transform all underlying geometries in the layer from the given crs to the new crs
   * 
   * @param fromCoordinateReferenceSystem presumed current crs
   * @param toCoordinateReferenceSystem   to tranform to crs
   * @throws PlanItException thrown if error
   */
  public abstract void transform(CoordinateReferenceSystem fromCoordinateReferenceSystem, CoordinateReferenceSystem toCoordinateReferenceSystem) throws PlanItException;

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param listeners         to call back during removal of danlging subnetworks
   * @throws PlanItException thrown if error
   */
  public abstract void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, final Set<RemoveSubGraphListener<?, ?>> listeners)
      throws PlanItException;

  /**
   * Number of nodes
   * 
   * @return number of nodes
   */
  public abstract long getNumberOfNodes();

  /**
   * Number of links
   * 
   * @return number of links
   */
  public abstract long getNumberOfLinks();

  /**
   * Number of link segments
   * 
   * @return number of link segments
   */
  public abstract long getNumberOfLinkSegments();

  /**
   * remove any dangling subnetworks from the layer if they exist and subsequently reorder the internal ids if needed
   * 
   * @throws PlanItException thrown if error
   */
  public default void removeDanglingSubnetworks() throws PlanItException {
    removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @throws PlanItException thrown if error
   */
  public default void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest, null);
  }

}
