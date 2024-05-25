package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.Movement;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Custom iterator for static ltm directed path implementation to mimick iteratoing over edge segments
 * when internal structure is based on list of movements
 *
 * @author markr
 */
public class StaticLtmDirectedPathIterator implements Iterator<EdgeSegment> {

  private static final Logger LOGGER = Logger.getLogger(StaticLtmDirectedPathIterator.class.getCanonicalName());

  /**
   * the movement based path definition
   */
  private Movement[] thePath;

  private int index = 0;

  private int maxIndex;

  private boolean finalSegmentDone = false;

  /**
   * Constructor of this decorator
   *
   * @param thePath path to iterate over
   */
  public StaticLtmDirectedPathIterator(Movement[] thePath) {
    this.thePath = thePath;
    this.index = 0;
    this.maxIndex = thePath.length-1;
  }

  @Override
  public boolean hasNext() {
    return index < maxIndex || !finalSegmentDone;
  }

  @Override
  public EdgeSegment next() {
    if(index < maxIndex){
      return thePath[index++].getSegmentFrom();
    }else if(!finalSegmentDone){
      finalSegmentDone = true;
      return thePath[thePath.length-1].getSegmentTo();
    }
    return null;
  }
}