package org.goplanit.converter.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.geo.GeoContainerUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.quadtree.Quadtree;

import java.util.*;
import java.util.logging.Logger;

/**
 * A commonly used data class and functionality supporting any zoning conversion from original format to PLANit
 * format. Can be extended for specific formats but provides base level tracking containers that may be useful
 */
public class ZoningConverterCommonData {

  /** the logger  to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningConverterCommonData.class.getCanonicalName());

  /* SPATIAL LINK TRACKING */

  /** to be able to map stand-alone stations and platforms to connectoids in the network, we must be able to spatially
   * find close by created links, this is what we do here. */
  // todo integrate with GTFS version in GtfsZoningHandlerData
  private Map<MacroscopicNetworkLayer, Quadtree> spatiallyIndexedPlanitLinksByLayer = new TreeMap<>();

  /** connectoid sub-set of data tracking functionality */
  private final ZoningConverterConnectoidData connectoidData;

  /** geo utilities based on network/zoning CRS */
  PlanitJtsCrsUtils geoUtils;

  /** network we use */
  private final MacroscopicNetwork referenceNetwork;

  /** zoning we use */
  private final Zoning referenceZoning;

  /**
   * Constructor with default connectoid data
   */
  public ZoningConverterCommonData(MacroscopicNetwork network, Zoning referenceZoning){
    this(network, referenceZoning, new ZoningConverterConnectoidData(referenceZoning, network));
  }

  /**
   * Constructor with provided connectoid data to use
   *
   * @param connectoidData to use
   */
  public ZoningConverterCommonData(
      MacroscopicNetwork network, Zoning referenceZoning, ZoningConverterConnectoidData connectoidData){
    this.connectoidData = connectoidData;
    this.referenceNetwork = network;
    this.referenceZoning = referenceZoning;

    this.geoUtils = new PlanitJtsCrsUtils(referenceNetwork.getCoordinateReferenceSystem());
  }

  public Zoning getReferenceZoning(){
    return referenceZoning;
  }

  public MacroscopicNetwork getReferenceNetwork(){
    return referenceNetwork;
  }

  /** initialise based on links in reference network
   *
   */
  public void recreateSpatiallyIndexedLinks() {
    for(MacroscopicNetworkLayer layer : referenceNetwork.getTransportLayers()) {
      spatiallyIndexedPlanitLinksByLayer.put(layer, GeoContainerUtils.toGeoIndexed(layer.getLinks()));
    }
  }

  /** access to geo utils
   *
   * @return the utils
   */
  public PlanitJtsCrsUtils getGeoUtils(){
    return geoUtils;
  }

  /**
   * Reset the PLANit data tracking containers
   */
  public void reset() {
    spatiallyIndexedPlanitLinksByLayer = new TreeMap<>();
    connectoidData.reset();
  }

  /* SPATIAL LINK INDEX RELATED METHODS */

  /** Remove provided links from local spatial index based on links
   *
   * @param links to remove
   */
  public void removeLinksFromSpatialLinkIndex(Collection<MacroscopicLink> links) {
    if(links != null) {
      // we do not know in which layer the link resides, so we try each layer
      links.forEach(this::removeLinkFromSpatialLinkIndex);
    }
  }

  /** Remove provided link from local spatial index
   *
   * @param link to remove
   */
  public void removeLinkFromSpatialLinkIndex(MacroscopicLink link) {
    if(link != null) {
      var envelope = link.createEnvelope();
      // we do not know in which layer the link resides, so we try each layer
      spatiallyIndexedPlanitLinksByLayer.forEach(
          (k,v) -> v.remove(envelope, link));
    }
  }

  /**
   * Add provided links to local spatial index based on their bounding box
   *
   * @param networkLayer the link belong to
   * @param links        to add
   */
  public void addLinksToSpatialLinkIndex(MacroscopicNetworkLayer networkLayer, Collection<MacroscopicLink> links) {
    if(links != null) {
      spatiallyIndexedPlanitLinksByLayer.computeIfAbsent(networkLayer, nl -> new Quadtree());
      links.forEach(
          link -> spatiallyIndexedPlanitLinksByLayer.get(networkLayer).insert(link.createEnvelope(), link));
    }
  }

  /**
   * Add provided links to local spatial index based on their bounding box
   *
   * @param networkLayer the link belong to
   * @param links        to add
   */
  public void addLinksToSpatialLinkIndex(MacroscopicNetworkLayer networkLayer, MacroscopicLink... links) {
    if(links != null) {
      spatiallyIndexedPlanitLinksByLayer.computeIfAbsent(networkLayer, nl -> new Quadtree());
      for(var link : links) {
        spatiallyIndexedPlanitLinksByLayer.get(networkLayer).insert(link.createEnvelope(), link);
      };
    }
  }

  /** Find links spatially based on the provided bounding box
   *
   * @param searchBoundingBox to use
   * @return links found intersecting or within bounding box provided
   */
  public Map<MacroscopicNetworkLayer, Collection<MacroscopicLink>> findLinksSpatially(Envelope searchBoundingBox) {
    var result = new TreeMap<MacroscopicNetworkLayer, Collection<MacroscopicLink>>();
    for(var entry : spatiallyIndexedPlanitLinksByLayer.entrySet()) {
      var foundLinks = findLinksSpatially(entry.getKey(), searchBoundingBox);
      if(CollectionUtils.nullOrEmpty(foundLinks)) {
        continue;
      }
      result.put(entry.getKey(), foundLinks);
    }
    return result;
  }

  /** Find links spatially based on the provided bounding box across all layers
   *
   * @param searchBoundingBox to use
   * @return links found intersecting or within bounding box provided
   */
  public Collection<MacroscopicLink> findLinksSpatiallyAcrossLayers(Envelope searchBoundingBox) {
    var result = new TreeSet<MacroscopicLink>();
    for(var entry : spatiallyIndexedPlanitLinksByLayer.entrySet()) {
      var foundLinks = findLinksSpatially(entry.getKey(), searchBoundingBox);
      if(CollectionUtils.nullOrEmpty(foundLinks)) {
        continue;
      }
      result.addAll(foundLinks);
    }
    return result;
  }

  /** Find links spatially based on the provided bounding box
   *
   * @param networkLayer restrict to looking within given layer
   * @param searchBoundingBox to use
   * @return links found intersecting or within bounding box provided
   */
  public Collection<MacroscopicLink> findLinksSpatially(
      MacroscopicNetworkLayer networkLayer, Envelope searchBoundingBox) {
    var spatiallyIndexedPlanitLinks = spatiallyIndexedPlanitLinksByLayer.get(networkLayer);
    if(spatiallyIndexedPlanitLinks == null){
      return Collections.emptySet();
    }
    return GeoContainerUtils.queryEdgeQuadtree(spatiallyIndexedPlanitLinks, searchBoundingBox);
  }

  /**
   * Find all nearby links within given search radius of geometry that do not have any banned modes but will have at
   * least one of the supported modes
   *
   * @param geometry geometry to use
   * @param bannedModes to consider
   * @param supportedModes to consider
   * @param maxSearchRadius to constrain by
   * @return found links
   */
  public Collection<MacroscopicLink> findNearbyModeCompatibleLinks(
      Geometry geometry,
      Collection<Mode> bannedModes,
      Set<Mode> supportedModes,
      double maxSearchRadius){

    // assume single layer
    var networkLayer = this.getReferenceNetwork().getLayerByMode(supportedModes.iterator().next());
    var boundingBox = getGeoUtils().createBoundingBox(
        geometry.getEnvelopeInternal(), maxSearchRadius);

    Collection<MacroscopicLink> spatiallyMatchedLinks = findLinksSpatially(networkLayer, boundingBox);
    // reduce to any active mode supporting links that are not rail-based links
    spatiallyMatchedLinks.removeIf(l -> !l.isAnyModeAllowedOnAnySegment(supportedModes) ||
        l.isAnyModeAllowedOnAnySegment(bannedModes));

    return spatiallyMatchedLinks;
  }

  /**
   * Add provided link to spatially indexed links in layer. Only use when a new link is created during the Osm parsing
   * since otherwise it should already be present
   *
   * @param networkLayer the link resides on
   * @param linkToAdd    to add
   */
  public void addLinkToSpatiallyIndexed(MacroscopicNetworkLayer networkLayer, MacroscopicLink linkToAdd){
    var spatiallyIndexedPlanitLinks = spatiallyIndexedPlanitLinksByLayer.get(networkLayer);
    if(spatiallyIndexedPlanitLinks == null){
      spatiallyIndexedPlanitLinks = GeoContainerUtils.toGeoIndexed(linkToAdd);
      spatiallyIndexedPlanitLinksByLayer.put(networkLayer, spatiallyIndexedPlanitLinks);
    }
    GeoContainerUtils.addToGeoIndexed(spatiallyIndexedPlanitLinks, linkToAdd);
  }

  /** access to common connectoid data tracking
   *
   * @return connectoid data
   */
  public ZoningConverterConnectoidData getConnectoidData() {
    return connectoidData;
  }
}
