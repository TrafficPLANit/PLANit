package org.planit.network;

import java.util.Set;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.graph.modifier.RemoveSubGraphListener;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TopologicalLayer;
import org.planit.utils.network.layers.TopologicalLayers;

/**
 * A network with topological transport layers, meaning that apart from representing a physical reality the result is topologically meaningful, has nodes, links, and some
 * geographic notion via a coordinate reference system
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayerNetwork<T extends TopologicalLayer, U extends TopologicalLayers<T>> extends TransportLayerNetwork<T, U> {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(TopologicalLayerNetwork.class.getCanonicalName());

  // Protected

  // Public

  /** the coordinate reference system used for all layers in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public TopologicalLayerNetwork(IdGroupingToken tokenId) {
    this(tokenId, PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public TopologicalLayerNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId);

    /* default crs */
    this.coordinateReferenceSystem = coordinateReferenceSystem;
  }

  /**
   * collect the used crs
   * 
   * @return coordinateReferencesystem used by this infrastructure network
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return this.coordinateReferenceSystem;
  }

  /**
   * set the coordinate reference system used for all layers
   * 
   * @param coordinateReferenceSystem to set
   */
  public void setCoordinateReferenceSystem(final CoordinateReferenceSystem coordinateReferenceSystem) {
    if (transportLayers.isEachLayerEmpty()) {
      this.coordinateReferenceSystem = coordinateReferenceSystem;
    } else {
      LOGGER.warning("Coordinate Reference System is already set. To change the CRS after instantiation, use transform() method");
    }
  }

  /**
   * change the coordinate system, which will result in an update of all geometries in the network layers from the original CRS to the new CRS. If the network is empty and no CRS
   * is set then this is identical to calling setCoordinateReferenceSystem, otherwise it will change the CRS while the set method will throw an exception
   * 
   * @param newCoordinateReferenceSystem to transform the network to
   * @throws PlanItException thrown if error
   */
  public void transform(final CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    for (TopologicalLayer layer : transportLayers) {
      layer.transform(coordinateReferenceSystem, newCoordinateReferenceSystem);
    }
  }

  /**
   * remove any dangling subnetworks from the network's layers if they exist and subsequently reorder the internal ids if needed
   * 
   * @throws PlanItException thrown if error
   * 
   */
  public void removeDanglingSubnetworks() throws PlanItException {
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
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest, null);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed. From the zoning remove any zones that
   * rely on infrastructure that is deemed dangling
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @param listeners         to apply
   * @throws PlanItException thrown if error
   */
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, final Set<RemoveSubGraphListener> listeners) throws PlanItException {
    for (TopologicalLayer infrastructureLayer : this.transportLayers) {
      infrastructureLayer.removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest, listeners);
    }
  }

}
