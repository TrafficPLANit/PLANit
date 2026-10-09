package org.goplanit.assignment.common.pas;

import org.apache.commons.collections4.set.UnmodifiableSet;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CollectionUtils;
import org.goplanit.utils.reflection.ReflectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Container class for tracking PASs that share all but initial and final turn
 * 
 * @author markr
 * @param <V> type of vertex
 * @param <ES> type of segment
 */
public class PasGroup<V extends DirectedVertex, ES extends EdgeSegment> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PasGroup.class.getCanonicalName());

  /**
   * all PASs in the group
   */
  private final TreeSet<Pas<V,ES>> pass;

  public PasGroup(final Pas<V,ES> bootStrapPas){
    pass = new TreeSet<>();
    pass.add(bootStrapPas);
  }

  public TreeSet<Pas<V,ES>> getPass(){
    return pass;
  }
}
