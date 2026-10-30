package org.goplanit.zoning.modifier.event.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;

/**
 * What the connectoids lost while {@link UpdateConnectoidsOnLinkSegmentChangeHandler} kept them in step with the link
 * segments they use for access, in the order it happened
 *
 * @author markr
 */
public final class UpdateConnectoidsOnLinkSegmentChangeResult {

  /**
   * What a connectoid lost when a link segment it uses for access changed: a mode one of its access zone entries
   * allowed, an access link segment of an entry, a whole entry, or the connectoid itself. Each loss is stated once, at
   * the level it stopped at, so an access link segment whose removal took its entry with it is stated as the loss of
   * the entry
   */
  public static final class ConnectoidAccessLoss {

    /** what was lost */
    public enum Kind {

      /** a mode an entry allowed explicitly, the entry staying */
      ALLOWED_MODE,

      /** an access link segment of an entry, the entry staying */
      ACCESS_LINK_SEGMENT,

      /** an access zone entry, the connectoid keeping other entries */
      ACCESS_ZONE_ENTRY,

      /** the connectoid, left without any access zone entry */
      CONNECTOID
    }

    /** what was lost */
    private final Kind kind;

    /** the connectoid that lost it */
    private final Connectoid connectoid;

    /** the access zone entry concerned */
    private final ConnectoidAccessZoneEntry accessZoneEntry;

    /** the access link segment concerned, null when the loss is not about one */
    private final EdgeSegment accessLinkSegment;

    /** the mode lost, null when the loss is not about one */
    private final Mode mode;

    /**
     * Constructor
     *
     * @param kind what was lost
     * @param connectoid that lost it
     * @param accessZoneEntry concerned
     * @param accessLinkSegment concerned, null when the loss is not about one
     * @param mode lost, null when the loss is not about one
     */
    private ConnectoidAccessLoss(Kind kind, Connectoid connectoid, ConnectoidAccessZoneEntry accessZoneEntry,
        EdgeSegment accessLinkSegment, Mode mode) {
      this.kind = kind;
      this.connectoid = connectoid;
      this.accessZoneEntry = accessZoneEntry;
      this.accessLinkSegment = accessLinkSegment;
      this.mode = mode;
    }

    /**
     * What was lost
     *
     * @return kind of loss
     */
    public Kind getKind() {
      return kind;
    }

    /**
     * The connectoid that lost it
     *
     * @return connectoid
     */
    public Connectoid getConnectoid() {
      return connectoid;
    }

    /**
     * The access zone entry concerned
     *
     * @return access zone entry
     */
    public ConnectoidAccessZoneEntry getAccessZoneEntry() {
      return accessZoneEntry;
    }

    /**
     * The access link segment concerned
     *
     * @return access link segment, null when the loss is not about one
     */
    public EdgeSegment getAccessLinkSegment() {
      return accessLinkSegment;
    }

    /**
     * The mode lost
     *
     * @return mode, null when the loss is not about one
     */
    public Mode getMode() {
      return mode;
    }
  }

  /** what the connectoids lost, in the order it happened */
  private final List<ConnectoidAccessLoss> accessLosses = new ArrayList<>();

  /**
   * Constructor, nothing lost yet
   */
  public UpdateConnectoidsOnLinkSegmentChangeResult() {
  }

  /**
   * Record what a connectoid lost
   *
   * @param kind what was lost
   * @param connectoid that lost it
   * @param accessZoneEntry concerned
   * @param accessLinkSegment concerned, null when the loss is not about one
   * @param mode lost, null when the loss is not about one
   */
  public void addAccessLoss(ConnectoidAccessLoss.Kind kind, Connectoid connectoid,
      ConnectoidAccessZoneEntry accessZoneEntry, EdgeSegment accessLinkSegment, Mode mode) {
    accessLosses.add(new ConnectoidAccessLoss(kind, connectoid, accessZoneEntry, accessLinkSegment, mode));
  }

  /**
   * What the connectoids lost so far, in the order it happened
   *
   * @return losses, unmodifiable
   */
  public List<ConnectoidAccessLoss> getAccessLosses() {
    return Collections.unmodifiableList(accessLosses);
  }
}
