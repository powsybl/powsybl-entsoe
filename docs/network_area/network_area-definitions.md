# Network Area Definitions

This page details the different implementations of network areas and how their net positions are computed.

## Border-based Implementations

Border-based areas compute the net position by looking at the "perimeter" of the area. The net position is the sum of active power flows leaving the area through its boundaries.

### Implementations
- **BorderBasedCountryArea**: Defined by a list of `Country` objects.
- **BorderBasedVoltageLevelsArea**: Defined by a list of voltage level IDs.

### Net Position Calculation

For border-based areas, the net position is calculated as:

$$NP = \sum P_{leaving\_flow}$$

This includes:
- Flows on **Tie lines** (Boundary lines).
- Flows on **Lines** and **HVDC lines** where one end is inside the area and the other is outside.
- For voltage-level based areas, it also includes **Transformers** (two-windings and three-windings) connecting a voltage level inside the area to one outside.

The flow is counted positively if it leaves the area. For AC lines, the flow is usually calculated as $(P_{side1} - P_{side2}) / 2$ to account for losses, oriented from the internal side to the external side.

## Injection-based Implementations

Injection-based areas compute the net position by summing all power injections located inside the area.

### Implementations
- **InjectionBasedCountryArea**: All generators and loads within the specified countries.
- **InjectionBasedVoltageLevelsArea**: All generators and loads within the specified voltage levels.

### Net Position Calculation

The net position is calculated as the sum of internal generations minus the sum of internal loads:

$$NP = \sum P_{generators} - \sum P_{loads}$$

Specifically, it uses:
- `Generator.getTargetP()` for productions.
- `Load.getP0()` for consumptions.

## Load Flow Balancing

Both types of implementations support an optional parameter `subtractLoadFlowBalancing` in the `getNetPosition(boolean)` method.

### Why is it needed?
In a load flow study, the calculated physical flows on lines might not perfectly match the initial target injections due to:
- Network losses.
- The presence of a slack mechanism (single slack bus or distributed slack) that absorbs the global mismatch.
- Differences between target power (`targetP`, `p0`) and actual computed power (`p`) after load flow convergence.

### Effect on calculation
When `subtractLoadFlowBalancing` is set to `true`:

1.  **For Injection-based areas**: Currently, the implementation might ignore this flag or handle it by returning the raw injection balance. (Refer to specific implementation details).
2.  **For Border-based areas**: The net position is adjusted by the local "mismatch" found within the area:

    $$NP_{adjusted} = NP_{physical\_borders} - (\sum (P_{target} - P_{computed}))$$

    This helps in aligning the border-based net position with the intended injection-based balance by removing the influence of the load flow's balancing mechanism (like slack adjustments) if they occurred inside the area.
