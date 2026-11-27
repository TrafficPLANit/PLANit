package org.goplanit.pricing;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.problem.Problems;

/**
 * Checks of a cap, before it is made by its factory
 * <p>
 * A factory only makes and registers, so the checks of a cap sit here, next to the caps, where any caller (the pricing
 * reader, a converter, a test) can ask them: whether the modes a cap names are charged by its schemes, and whether its
 * amount can be paid at all given the minimums of those schemes.
 * </p>
 *
 * @author markr
 */
public class PriceCapUtils {

  /** Not to be instantiated */
  private PriceCapUtils() {
  }

  /**
   * The modes a cap names limited to those its schemes charge, as an override or a minimum is limited, see
   * {@link SchemePricingImpl#limitToModes(Object, PricedModeSelection, Collection, java.util.function.Function,
   * Problems, String, String)}: a mode none of them charges says nothing for the cap, so it is left off, each one
   * recorded as a problem
   *
   * @param schemes the cap covers
   * @param modes the cap names
   * @param problems to record each mode left off in, and the cap when it is left without modes
   * @return the modes the cap keeps, null when none is left
   */
  public static PricedModeSelection limitToChargedModes(
      List<? extends PricingScheme> schemes,
      PricedModeSelection modes,
      Problems<PricingProblemKind> problems) {
    List<Mode> chargedModes = schemes.stream()
        .flatMap(scheme -> scheme.getChargedModes().stream())
        .distinct()
        .collect(Collectors.toList());
    return SchemePricingImpl.limitToModes(modes, modes, chargedModes, selection -> selection, problems, "", "cap");
  }

  /**
   * Find each minimum above a cap's amount, for a mode it caps on a scheme it covers, as a problem refusing the cap: a
   * use could not cost at least the minimum and at most the cap, for example a cap of 1.50 under a minimum of 2.00 for
   * cars
   *
   * @param schemes the cap covers
   * @param amountInCurrency the cap's amount, null when it counts uses
   * @param modes the cap's modes
   * @return a problem per minimum above the amount, none when the cap counts uses
   */
  public static Problems<PricingProblemKind> findMinimumsAboveAmount(
      List<? extends PricingScheme> schemes,
      Double amountInCurrency,
      PricedModeSelection modes) {
    var problems = new Problems<PricingProblemKind>();
    for (var scheme : amountInCurrency == null ? List.<PricingScheme>of() : schemes) {
      for (var mode : scheme.getChargedModes()) {
        double minimum = modes.appliesTo(mode) ? scheme.getPricing().findModesMinimumPrice(mode) : 0;
        problems.addIf(minimum > amountInCurrency, PricingProblemKind.CAP_BELOW_MINIMUM, "",
            "scheme " + scheme.getIdsAsString(),
            "mode " + mode.getIdsAsString() + " minimum " + minimum + " above amount " + amountInCurrency);
      }
    }
    return problems;
  }
}
