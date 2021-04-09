package org.planit.network;

import java.util.Set;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;

/**
 * An infrastructure layer represents the infrastructure suited for a number of modes. This can be in the form of a physical network or by some other (more aggregate)
 * representation. The combination of infrastructure layers can be used to construct an intermodal network. Each layer supports one or more modes
 * 
 * @author markr
 *
 */
public interface TopologicalLayer extends InfrastructureLayer {

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
  public abstract void removeDanglingSubnetworks(final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, final Set<RemoveSubGraphListener<?, ?>> listeners) throws PlanItException;

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
