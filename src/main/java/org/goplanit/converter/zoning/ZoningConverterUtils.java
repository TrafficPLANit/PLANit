package org.goplanit.converter.zoning;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;

/**
 * Utilities regarding Zoning conversions that might be useful for implementations of this particular converter
 *
 * @author markr
 *
 */
public class ZoningConverterUtils {

  /** create a subset of links from the passed in ones, removing all links for which we can be certain that geometry is located on the wrong side of the road infrastructure geometry. Note that
   * rails is not excluded in this exercise, i.e., it is assumed rail lines ar bi-rectional.
   * Right or wrong side is verified by checking if the link is one-way. If so, we can be sure (based on the driving direction of the country) if the geometry is located to the closest by (logical)
   * driving direction given the placement of the geometry, i.e., on the left hand side for left hand drive countries, on the right hand side for right hand driving countries
   *
   * @param location representing the location of concern expected to have access to the link
   * @param links to remove in-eligible ones from
   * @param isLeftHandDrive flag
   * @param accessModes to consider
   * @param geoUtils to use
   * @return remaining links that are deemed eligible
   */
  public static Collection<MacroscopicLink> excludeLinksOnWrongSideOf(Geometry location, Collection<MacroscopicLink> links, boolean isLeftHandDrive, Collection<Mode> accessModes, PlanitJtsCrsUtils geoUtils) {
    Collection<MacroscopicLink> matchedLinks = new HashSet<>(links);
    for(var link : links) {
      for(Mode accessMode : accessModes){

        /* road based PT modes are only accessible on one side, so they must stop with the waiting area in the correct driving direction, i.e., must avoid cross traffic, because otherwise they
         * have no doors at the right side, e.g., travellers have to cross the road to get to the vehicle, which should not happen */
        boolean mustAvoidCrossingTraffic = true;
        if(accessMode.getPhysicalFeatures().getTrackType().equals(TrackModeType.RAIL)) {
          mustAvoidCrossingTraffic = false;
        }

        MacroscopicLinkSegment oneWayLinkSegment = link.getLinkSegmentIfLinkIsOneWayForMode(accessMode);
        if(oneWayLinkSegment != null && mustAvoidCrossingTraffic) {
          /* use line geometry closest to connectoid location */
          LineSegment finalLineSegment = PlanitEntityGeoUtils.extractClosestLineSegmentToGeometryFromLinkSegment(location, oneWayLinkSegment, geoUtils);
          /* determine location relative to infrastructure */
          boolean isStationLeftOfOneWayLinkSegment = geoUtils.isGeometryLeftOf(location, finalLineSegment.p0, finalLineSegment.p1);
          if(isStationLeftOfOneWayLinkSegment != isLeftHandDrive) {
            /* travellers cannot reach doors of mode on this side of the road, so deemed not eligible */
            matchedLinks.remove(link);
            break; // from mode loop
          }
        }
      }
    }
    return matchedLinks;
  }

  /**
   * Exclude the closest link if it is situated on the wrong side of the road, then if links are remaining keep going with removing the then closest if it is also
   * on the wrong side of the road etc.
   *
   * @param location to verify against, typically a transfer zone location
   * @param links to update
   * @param isLeftHandDrive network driving direction
   * @param accessModes for the location
   * @param geoUtils to use
   * @return true when one or more closest links are removed, false otherwise
   */
  public static boolean excludeClosestLinksIncrementallyOnWrongSideOf(Geometry location, Collection<MacroscopicLink> links, boolean isLeftHandDrive, Collection<Mode> accessModes, PlanitJtsCrsUtils geoUtils) {
    boolean entriesRemoved = false;
    do{
      MacroscopicLink closestLink = (MacroscopicLink) PlanitGraphGeoUtils.findEdgeClosest(location, links, geoUtils);
      Collection<MacroscopicLink> result =
          ZoningConverterUtils.excludeLinksOnWrongSideOf(location, links, isLeftHandDrive,  accessModes, geoUtils);
      if(result!=null && !result.isEmpty()){
        /* closest is also viable, continue */
        break;
      }else {
        /* closest link is on the wrong side of the waiting area, let user know, possibly tagging error */
        links.remove(closestLink);
        entriesRemoved = true;
      }

      if(links.isEmpty()){
        break;
      }
    }while(true);
    return entriesRemoved;
  }
}
