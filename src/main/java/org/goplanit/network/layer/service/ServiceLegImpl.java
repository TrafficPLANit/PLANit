package org.goplanit.network.layer.service;

import java.util.logging.Logger;

import org.goplanit.graph.directed.DirectedEdgeImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
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
   * Not allowed in service context as each segment may have different lengths derived from underlying physical leg segments
   * 
   * @return found length
   */
  @Override
  public double getLengthKm() {
    throw new PlanItRunTimeException("Not possible to determine length based on service leg, underlying leg segments may have different lengths");
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
   * Not allowed, collect geometry via underlying links instead
   * 
   * @return null
   */
  @Override
  public LineString getGeometry() {
    return null;
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
  public ServiceLegImpl clone(){
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
