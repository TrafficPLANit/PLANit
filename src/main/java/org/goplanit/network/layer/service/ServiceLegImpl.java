package org.goplanit.network.layer.service;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A service leg connects two service nodes. Underlying are one or more physical links represented by this single service leg.
 * 
 * @author markr
 *
 */
public class ServiceLegImpl extends DirectedEdgeImpl<ServiceNode, ServiceLegSegment> implements ServiceLeg {

  /** Generated UID */
  private static final long serialVersionUID = 822966574857604397L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceLegImpl.class.getCanonicalName());

  /**
   * Constructor which injects link lengths directly
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @param nodeA   first vertex in the link
   * @param nodeB   second vertex in the link
   */
  protected ServiceLegImpl(final IdGroupingToken tokenId, final ServiceNode nodeA, final ServiceNode nodeB) {
    super(tokenId, nodeA, nodeB);
  }

  /**
   * Copy Constructor. network layer links are shallow copied.
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected ServiceLegImpl(ServiceLegImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * @return true when all underlying service leg segments have a geometry, false otherwise
   */
  @Override
  public boolean hasGeometry() {
    if (getLegSegments() == null || getLegSegments().isEmpty()) {
      return false;
    } else {
      return getLegSegments().stream().allMatch(ls -> ls.hasGeometry());
    }
  }

  /**
   * @return true when geometry is present and all underlying links are in ab direction, false otherwise
   */
  @Override
  public boolean isGeometryInAbDirection() {
    throw new PlanItRunTimeException("Not possible to determine geometry direction unambiguously as geomtry is derived from service leg segments");
  }

  /**
   * Not allowed on service leg, perform on underlying links instead. Always throws exception
   * 
   * @param transformer to use
   */
  @Override
  public void transformGeometry(MathTransform transformer) throws MismatchedDimensionException, TransformException {
    throw new TransformException("Not allowed to transform geometry on service leg since it holds no geometry. Consider transforming underlying parent links instead");
  }

  /**
   * Create based on underlying links that have a geometry
   * 
   * @return composite envelope, null if no underlying links, or links have no geometry
   */
  @Override
  public Envelope createEnvelope() {
    if (getLegSegments() == null || getLegSegments().isEmpty()) {
      return null;
    }
    Envelope envelope = null;
    for(ServiceLegSegment legSegment : getLegSegments()){
      for (var linkSegment : legSegment.getPhysicalParentSegments()) {
        if (linkSegment.hasGeometry()) {
          if (envelope == null) {
            envelope = linkSegment.getParentLink().createEnvelope();
          } else {
            envelope.expandToInclude(linkSegment.getParentLink().createEnvelope());
          }
        }
      }
    }
    return envelope;
  }

  /**
   * Because each service leg segment may comprise different physical link(s) (segments) they may have different
   * geometry for either direction, so no single geometry may exist on the (non-directional) leg. Hence, we construct
   * an as-the-crow flies line between the service nodes instead
   * 
   * @return straight line between service node a position and service node b position
   */
  @Override
  public LineString getGeometry() {
    return PlanitJtsUtils.createLineString(
            getServiceNodeA().getPosition().getCoordinate(), getServiceNodeB().getPosition().getCoordinate());
  }

  /**
   * Not allowed, set geometry via underlying leg segment physical links instead
   * 
   * @param lineString to use
   */
  @Override
  public void setGeometry(LineString lineString) {
    LOGGER.warning("Not allowed to set geometry on service leg, do so on underlying links instead");
  }

  /**
   * Will apply #LengthType.AVERAGE to obtain average length across service leg segments
   * (in case both service leg segments are mapped to the leg and have different lengths due to different underlying
   * physical link segments).
   *
   * @return found length, if no underlying service leg segments are present, length is set to infinite
   */
  @Override
  public double getLengthKm() {
    return getLengthKm(LengthType.AVERAGE);
  }

  /**
   * determine length based on desired length type (in case both service leg segments are mapped to the leg and
   * have different lengths due to different underlying physical link segments)
   *
   * @param lengthType to apply
   * @return found length, if no underlying service leg segments are present, length is set to zero
   */
  @Override
  public double getLengthKm(LengthType lengthType){
    double resultIfAbsent = 0;
    var streamOfLinkSegmentLengths = getLegSegments().stream().mapToDouble(ls -> ls.getLengthKm());
    switch (lengthType){
      case MAX:
        return streamOfLinkSegmentLengths.max().orElse(resultIfAbsent);
      case MIN:
        return streamOfLinkSegmentLengths.min().orElse(resultIfAbsent);
      case AVERAGE:
        return streamOfLinkSegmentLengths.average().orElse(resultIfAbsent);
      default:
        LOGGER.warning(String.format("Unsupported length type %s when constructing length for service leg %s, revert to default %.2f", getIdsAsString(), resultIfAbsent));
        return  resultIfAbsent;
    }
  }

  /**
   * Not allowed, set length via underlying links instead
   * 
   * @param lengthInKm to use
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    LOGGER.warning("Not allowed to set length on service leg, do so on underlying leg segment's physical links instead");
  }

  /**
   * Validate based on edge that it is, but also make sure that the references to parent network are consistent, i.e., the service nodes reside on the parent links in the right
   * location as well
   */
  @Override
  public boolean validate() {
    boolean valid = super.validate();
    if (valid && hasEdgeSegment()) {
      for(var serviceLegSegment : getLegSegments()) {
        serviceLegSegment.validate();
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegImpl shallowClone(){
    return new ServiceLegImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegImpl deepClone(){
    return new ServiceLegImpl(this, true);
  }
}
