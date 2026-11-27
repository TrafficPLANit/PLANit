package org.goplanit.pricing.modifier.event.handler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.utils.graph.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.utils.graph.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RemoveTollRoadEvent;
import org.goplanit.utils.network.layer.modifier.event.RemoveLinkSegmentTypeEvent;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.pricing.PricingSchemeGateImpl;
import org.goplanit.pricing.PricingSchemeImpl;
import org.goplanit.pricing.PricingSchemeOnTollRoadsImpl;
import org.goplanit.pricing.PricingSchemeSectionImpl;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.problem.Problems;

/**
 * Keeps a network pricing in step with every change the layer modifiers make to the network it refers to: the link
 * segments mode schemes list in their own priced link segments, the sections and the gates follow breaks and removals,
 * a removed link segment type is taken off the mode schemes' own priced link segments and the sections, and a removed
 * toll road off the schemes on it
 * <p>
 * A pricing is a component of its own, so it follows the network as the zoning does, through one handler for every
 * network change, registered on each layer modifier as an internal listener. The layer's own handlers run first, so a
 * toll road left empty is already removed, and its removal announced, when this handler hears of the link segment.
 * For example breaking a link of a section leaves both parts in the section, so a pass still pays the section's start
 * once; removing the last link segment of a section removes it, with the section price rules referring to it, and a
 * scheme left without toll roads is removed, as is a mode scheme left with nothing included, which would otherwise
 * charge on every link segment of its layers. Each such loss is warned about once, under the scheme it affects.
 * </p>
 *
 * @author markr
 */
public class UpdatePricingOnNetworkChangeHandler implements DirectedGraphModifierListener, TollRoadModifierListener,
    LinkSegmentTypeModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(UpdatePricingOnNetworkChangeHandler.class.getCanonicalName());

  /**
   * Warn about a scheme's loss, once, under the scheme
   *
   * @param scheme affected
   * @param kind of loss
   * @param detail what was lost
   */
  private static void warn(PricingScheme scheme, PricingProblemKind kind, String detail) {
    var problems = new Problems<PricingProblemKind>();
    problems.add(kind, "", "", detail);
    problems.logIfAny(LOGGER, "Scheme " + scheme.getIdsAsString());
  }

  /**
   * Warn about an event this handler is given but does not handle
   *
   * @param type of the event
   */
  private static void warnNotSupported(EventType type) {
    LOGGER.warning(String.format("%s does not support event type %s",
        UpdatePricingOnNetworkChangeHandler.class.getName(), type));
  }

  /** the pricing to keep in step */
  private final NetworkPricing networkPricing;

  /**
   * The schemes holding a section or gate, or charging on a toll road
   *
   * @param holds true for a scheme to give
   * @return the schemes
   */
  private List<PricingScheme> findSchemes(Predicate<PricingScheme> holds) {
    return networkPricing.streamSchemes().filter(holds).collect(Collectors.toList());
  }

  /**
   * Remove a section left without link segments or types, and drop it, and the section price rules referring to it,
   * from the schemes holding it
   *
   * @param section left empty
   */
  private void removeSection(PricingSchemeSection section) {
    networkPricing.getSections().remove(section);
    for (var scheme : findSchemes(s -> ListUtils.containsByIdentity(((PricingSchemeImpl<?>) s).getSectionsOrNone(),
        section))) {
      ((PricingSchemeImpl<?>) scheme).updateSectionAndGateMapping(s -> s == section ? null : s, g -> g);
      warn(scheme, PricingProblemKind.SECTION_OR_GATE_LEFT_EMPTY, "section " + section.getIdsAsString());
    }
  }

  /**
   * Remove a section when it is left with neither link segments nor types
   *
   * @param section that lost link segments or types
   */
  private void removeSectionIfLeftEmpty(PricingSchemeSection section) {
    if (!section.hasLinkSegmentsOrLinkSegmentTypes()) {
      removeSection(section);
    }
  }

  /**
   * Take what the network removes off every mode scheme's own priced link segments; a scheme it leaves with neither an
   * included type nor an included link segment, after holding one, is removed through the pricing's modifier, which
   * announces it, with a warning naming it, as it would otherwise charge on every link segment of its layers
   *
   * @param removal taking what the network removed off a coverage
   * @param removed what the network removed, for the warning
   */
  private void removeFromModeSchemes(Consumer<ModePricingSchemeLinkSegmentCoverage> removal, String removed) {
    for (var scheme : networkPricing.getModeSchemes().stream().collect(Collectors.toList())) {
      var coverage = scheme.getLinkSegmentCoverage();
      boolean hadIncludes = coverage.hasIncludedLinkSegmentTypesOrLinkSegments();
      removal.accept(coverage);
      if (hadIncludes && !coverage.hasIncludedLinkSegmentTypesOrLinkSegments()) {
        warn(scheme, PricingProblemKind.LEFT_WITHOUT_INCLUDED_TYPES_OR_LINK_SEGMENTS, removed);
        networkPricing.getModifier().removePricingScheme(scheme);
      }
    }
  }

  /**
   * Take a removed link segment type off the mode schemes' own priced link segments and the sections, removing a
   * section left with neither link segments nor types
   *
   * @param removed link segment type
   */
  private void removeLinkSegmentType(MacroscopicLinkSegmentType removed) {
    removeFromModeSchemes(coverage -> coverage.removeLinkSegmentType(removed),
        "link segment type " + removed.getIdsAsString());
    for (var section : networkPricing.getSections().stream()
        .filter(section -> ListUtils.containsByIdentity(section.getLinkSegmentTypes(), removed))
        .collect(Collectors.toList())) {
      ((PricingSchemeSectionImpl) section).removeLinkSegmentType(removed);
      removeSectionIfLeftEmpty(section);
    }
  }

  /**
   * Remove a gate left with neither entries nor exits, and drop it, and the pairs referring to it, from the schemes
   * holding it
   *
   * @param gate left empty
   */
  private void removeGate(PricingSchemeGate gate) {
    networkPricing.getGates().remove(gate);
    for (var scheme : findSchemes(s -> ListUtils.containsByIdentity(((PricingSchemeImpl<?>) s).getGatesOrNone(),
        gate))) {
      ((PricingSchemeImpl<?>) scheme).updateSectionAndGateMapping(s -> s, g -> g == gate ? null : g);
      warn(scheme, PricingProblemKind.SECTION_OR_GATE_LEFT_EMPTY, "gate " + gate.getIdsAsString());
    }
  }

  /**
   * Keep both parts of a broken link segment where the original was: listed by a mode scheme, in a section, and at a
   * gate, where the entry keeps the part starting where the original started and the exit the part ending where it
   * ended
   * <p>
   * Breaking a link at a node leaves two link segments where there was one, so to the pricing nothing changes: both
   * charge as the original did, and a pass over a broken section still pays the section's start once.
   * </p>
   *
   * @param event of the break
   */
  private void onBreak(BreakEdgeSegmentEvent event) {
    var original = (LinkSegment) event.getOriginalEdgeSegment();
    if (original == event.getNewlyBrokenEdgeSegment() || !(original instanceof MacroscopicLinkSegment) ||
        !(event.getNewlyBrokenEdgeSegment() instanceof MacroscopicLinkSegment)) {
      return;
    }
    var kept = (MacroscopicLinkSegment) original;
    var brokenOff = (MacroscopicLinkSegment) event.getNewlyBrokenEdgeSegment();
    networkPricing.getModeSchemes().forEach(scheme -> {
      var coverage = scheme.getLinkSegmentCoverage();
      if (ListUtils.containsByIdentity(coverage.getIncludedLinkSegments(), kept)) {
        coverage.includeLinkSegments(List.of(brokenOff));
      }
      if (ListUtils.containsByIdentity(coverage.getExcludedLinkSegments(), kept)) {
        coverage.excludeLinkSegments(List.of(brokenOff));
      }
    });
    var sections = networkPricing.getSections();
    for (var section : sections.getByLinkSegment(kept)) {
      sections.update(section, s -> ((PricingSchemeSectionImpl) s).addLinkSegmentAfter(kept, brokenOff));
    }
    boolean keptIsUpstreamPart = kept.getDownstreamVertex() == (Node) event.getVertexToBreakAt();
    var gates = networkPricing.getGates();
    for (var gate : gates.getByLinkSegment(kept)) {
      gates.update(gate, g -> ((PricingSchemeGateImpl) g).replaceLinkSegment(kept,
          keptIsUpstreamPart ? kept : brokenOff, keptIsUpstreamPart ? brokenOff : kept));
    }
  }

  /**
   * Take a removed link segment off the mode schemes listing it, the sections and the gates, removing a mode scheme
   * left with nothing included and a section or gate left empty
   * <p>
   * A pricing must not refer to a link segment that is gone. A mode scheme left with nothing included would charge on
   * every link segment of its layers, so it goes too, with a warning, as does a section or gate with nothing left.
   * </p>
   *
   * @param removed link segment
   */
  private void removeLinkSegment(LinkSegment removed) {
    if (!(removed instanceof MacroscopicLinkSegment)) {
      return;
    }
    var linkSegment = (MacroscopicLinkSegment) removed;
    removeFromModeSchemes(coverage -> coverage.removeLinkSegment(linkSegment),
        "link segment " + linkSegment.getIdsAsString());
    var sections = networkPricing.getSections();
    for (var section : sections.getByLinkSegment(linkSegment)) {
      sections.update(section,
          s -> ((PricingSchemeSectionImpl) s).removeLinkSegmentsMatching(held -> held == linkSegment));
      removeSectionIfLeftEmpty(section);
    }
    var gates = networkPricing.getGates();
    for (var gate : gates.getByLinkSegment(linkSegment)) {
      gates.update(gate, g -> ((PricingSchemeGateImpl) g).removeEntriesAndExitsMatching(
          held -> held == linkSegment, held -> held == linkSegment));
      if (!gate.hasEntryOrExitLinkSegments()) {
        removeGate(gate);
      }
    }
  }

  /**
   * Constructor
   *
   * @param networkPricing to keep in step
   */
  public UpdatePricingOnNetworkChangeHandler(final NetworkPricing networkPricing) {
    this.networkPricing = networkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { BreakEdgeSegmentEvent.EVENT_TYPE, RemoveEdgeSegmentEvent.EVENT_TYPE,
        RemoveTollRoadEvent.EVENT_TYPE, RemoveLinkSegmentTypeEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      onBreak(BreakEdgeSegmentEvent.class.cast(event));
    } else if (event.getType().equals(RemoveEdgeSegmentEvent.EVENT_TYPE)) {
      removeLinkSegment((LinkSegment) RemoveEdgeSegmentEvent.class.cast(event).getRemovedEdgeSegment());
    } else {
      warnNotSupported(event.getType());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    warnNotSupported(event.getType());
  }

  /**
   * {@inheritDoc}
   * <p>
   * A removed toll road is taken off every scheme on it; a scheme left with none is removed through the pricing's
   * modifier, which announces it, with a warning naming the scheme
   * </p>
   */
  @Override
  public void onTollRoadModifierEvent(TollRoadModificationEvent event) {
    if (!event.getType().equals(RemoveTollRoadEvent.EVENT_TYPE)) {
      warnNotSupported(event.getType());
      return;
    }
    var tollRoad = ((RemoveTollRoadEvent) event).getRemovedTollRoad();
    for (var scheme : findSchemes(s -> s instanceof PricingSchemeOnTollRoadsImpl &&
        ListUtils.containsByIdentity(((PricingSchemeOnTollRoadsImpl<?>) s).getTollRoads(), tollRoad))) {
      var schemeOnTollRoads = (PricingSchemeOnTollRoadsImpl<?>) scheme;
      schemeOnTollRoads.removeTollRoad(tollRoad);
      if (schemeOnTollRoads.getTollRoads().isEmpty()) {
        warn(scheme, PricingProblemKind.LEFT_WITHOUT_TOLL_ROADS, "toll road " + tollRoad.getIdsAsString());
        networkPricing.getModifier().removePricingScheme(scheme);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * A removed link segment type is taken off the mode schemes' own priced link segments and the sections; a change of
   * a link segment's type needs nothing, as types are tested on demand
   * </p>
   */
  @Override
  public void onLinkSegmentTypeModifierEvent(LinkSegmentTypeModificationEvent event) {
    if (!event.getType().equals(RemoveLinkSegmentTypeEvent.EVENT_TYPE)) {
      warnNotSupported(event.getType());
      return;
    }
    removeLinkSegmentType(((RemoveLinkSegmentTypeEvent) event).getRemovedLinkSegmentType());
  }
}
