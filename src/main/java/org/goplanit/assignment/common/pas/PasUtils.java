package org.goplanit.assignment.common.pas;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.Collection;
import java.util.Comparator;

public class PasUtils {

  public static <V extends ConjugateDirectedVertex, ES extends ConjugateEdgeSegment> Collection<PasGroup<V,ES>>
  createConjugatePasGroups(Collection<Pas<V,ES>> conjugatePass){

    //todo: we do not consider if opposite original entry turns are conflicting, so it can be that
    // a turn from 1>5 ... 6>7 is lumped in a group with 5>3 .... 7>9 for example. This may not be bad thing
    // but our grouping is rather loose at this point

    // track by original diverge/merge vertices
    var pasGroups = new MultiKeyMap<Object, PasGroup<V,ES>>();
    for(var conjPas : conjugatePass){
      var originalDivergeVertex = conjPas.getFirstEdgeSegment(true).getOriginalCentreVertex();
      var originalMergeVertex = conjPas.getLastEdgeSegment(true).getOriginalCentreVertex();

      var pasGroup = pasGroups.get(originalDivergeVertex, originalMergeVertex);
      if(pasGroup == null){
        pasGroup = new PasGroup(conjPas);
        pasGroups.put(originalDivergeVertex, originalMergeVertex, pasGroup);
      }else {
        pasGroup.getPass().add(conjPas);
      }
    }
    return pasGroups.values();
  }
}
