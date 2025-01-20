package org.goplanit.output.adapter;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.time.TimePeriod;

import java.util.Optional;

/**
 * Interface defining the methods required for a bush link (segment) output adapter, i.e., for link segments that are
 * part of a bush. For now, these include both conjugate and non-conjugate bushes, so we keep the link segment as the
 * base class to be universally usable (albeit limited in what can be outputed).
 * 
 * @author markr
 *
 */
public interface BushLinkOutputTypeAdapter extends UntypedBushLinkOutputTypeAdapter<EdgeSegment> {


}
