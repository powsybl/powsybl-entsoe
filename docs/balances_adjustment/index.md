# Balance Adjustment

The `powsybl-balances-adjustment` module provides a mechanism to adjust the balances (net positions) of specific areas within a power network to match target values. This is achieved through an iterative process involving scaling and load flow calculations.

## Overview

In power system modeling, it is often necessary to adjust the generation and load within certain areas so that the total power export/import of each area matches a predefined target. The balance adjustment feature automates this process.

Key concepts:
- **Balance Computation Area**: A defined part of the network (e.g., a country, a control area) where the balance needs to be adjusted.
- **Target Net Position**: The desired net power exchange for an area.
- **Scaling**: The process of increasing or decreasing generation and/or load to reach the target net position.

## Mechanism

The balance adjustment follows an iterative algorithm:

1.  **Scaling**: For each defined area, the current net position is compared to the target. The difference (mismatch) is used to scale the "scalable" elements within the area (typically generators and loads).
2.  **Load Flow**: A load flow calculation is performed to update the state of the network after scaling. This is important because losses in the network change when generation and load are modified, affecting the actual net position.
3.  **Mismatch Computation**: The new net position of each area is calculated from the load flow results and compared against the target.
4.  **Convergence Check**: If the total mismatch (either sum of squared mismatches or maximum absolute mismatch) is below a defined threshold, the process stops with success.
5.  **Iteration**: If not converged, the process repeats from step 1, using the remaining mismatch to further adjust the scaling.

The process also supports a mode where the load flow is skipped, in which case only one iteration of scaling is performed.

### Impact on Network Variants

The balance adjustment treatment is designed to be "non-destructive" to your existing network states unless a successful convergence is reached:

- **Temporary Variant**: At the beginning of the process, a temporary local variant is created by cloning the variant specified as `workingStateId`. 
- **Isolated Computation**: All iterative steps (scaling and load flow) are performed exclusively on this temporary variant. This ensures that the original network state remains untouched during the computation.
- **Result Application**: 
    - If the computation **succeeds** (converges), the state of the temporary variant is cloned back into the original `workingStateId` variant.
    - If the computation **fails** (e.g., reaches maximum iterations without converging or load flow fails), the original variant is left in its initial state.
- **Cleanup**: The temporary variant is automatically deleted at the end of the process, and the network's working variant is restored to what it was before the call.

No new permanent variants are created by this treatment; it only modifies the one provided as the `workingStateId` upon success.

### Sum of Net Positions

Users may wonder what happens if the sum of the target net positions of the provided areas is not zero.

In a physical power system, the global balance must be maintained: `Generation = Load + Losses + Net Export`. If the sum of the target net positions (Net Export) for all areas is not zero, the difference must be compensated by other parts of the network or by the slack mechanism during the load flow calculation.

- **Iterative Adjustment**: The balance adjustment will still attempt to reach the target net position for each specified area by scaling its internal generation/load.
- **Slack Bus Role**: If the targets don't sum to zero, the load flow's balancing mechanism will automatically adjust injections to maintain the global balance of the network. Depending on the load flow configuration, this can be handled by a single **slack bus** or **distributed** across multiple buses (e.g., proportional to load).
- **Convergence**: The computation converges only if each area reaches its individual target within the defined threshold. If the requested imbalance is too large or causes the slack mechanism to reach its limits, the process might fail to converge.
- **Omitted Areas**: Any part of the network not included in a `BalanceComputationArea` will not be scaled, but its actual net position will still be affected by the global load flow and the slack adjustment.

## Parameters

The behavior of the balance computation can be tuned using `BalanceComputationParameters`.

### Configuration Options

The following parameters are available:
- **Threshold Net Position**: The convergence criteria for the total mismatch.
- **Mismatch Mode**: How the total mismatch is calculated (`SQUARED` for sum of squares, or `MAX_ABS` for the maximum absolute value).
- **Max Number of Iterations**: The maximum number of scaling/load flow loops.
- **With Load Flow**: Whether to run a load flow after each scaling step.
- **Subtract Load Flow Balancing**: If `true`, the area net position used for mismatch computation is adjusted by subtracting the internal "load flow balance" (the difference between target and actual injections). This is useful to compensate for slack adjustments or network losses when using border-based areas.
- **Scaling Parameters**: Specific parameters for the scaling process (e.g., whether to scale only generation, only load, or both).
- **Load Flow Parameters**: Parameters passed to the load flow engine.

### Loading Parameters

Parameters can be defined programmatically using a fluent API, loaded from the platform configuration, or loaded from a JSON configuration file.

#### Programmatic Definition

You can create a `BalanceComputationParameters` object and set its values using the fluent API:

```java
BalanceComputationParameters parameters = new BalanceComputationParameters()
    .setThresholdNetPosition(1.0)
    .setMaxNumberIterations(5)
    .setWithLoadFlow(true);
```

#### Platform Configuration

Parameters can also be loaded from the PowSyBl platform configuration (by default `~/.itools/config.yml`):

```java
BalanceComputationParameters parameters = BalanceComputationParameters.load();
```

#### JSON Configuration

Alternatively, you can use `JsonBalanceComputationParameters` to read parameters from a JSON file:

```java
import com.powsybl.balances_adjustment.balance_computation.json_parameters.JsonBalanceComputationParameters;
import java.nio.file.Paths;

// ...
BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(Paths.get("parameters.json"));
```

Example `parameters.json`:

```json
{
  "version" : "1.3",
  "maxNumberIterations" : 10,
  "thresholdNetPosition" : 1.0,
  "mismatchMode" : "MAX_ABS",
  "withLoadFlow" : true,
  "subtractLoadFlowBalancing" : false,
  "load-flow-parameters" : {
    "voltageInitMode" : "DC_VALUES",
    "dc" : false
  },
  "scaling-parameters" : {
    "version" : "1.2",
    "scalingConvention" : "GENERATOR"
  }
}
```

## Usage Example

The following example shows how to create the necessary objects and run a balance computation.

```java
import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.network.area.CountryAreaFactory;
import com.powsybl.iidm.modification.scalable.GeneratorsScalable;

import java.util.Arrays;
import java.util.List;

// ...

// 1. Prepare the areas to be balanced
BalanceComputationArea areaFR = new BalanceComputationArea(
    "France",
    new CountryAreaFactory(Country.FR),
    Scalable.onGenerator("FR gen"),
    500.0
);

BalanceComputationArea areaBE = new BalanceComputationArea(
    "Belgium",
    new CountryAreaFactory(Country.BE),
    Scalable.stack("BE gen", "BE load"),
    -500.0
);

List<BalanceComputationArea> areas = Arrays.asList(areaFR, areaBE);

// 2. Configure parameters
BalanceComputationParameters parameters = new BalanceComputationParameters()
    .setThresholdNetPosition(1.0)  // 1 MW threshold
    .setMaxNumberIterations(5)
    .setWithLoadFlow(true);

// 3. Run the computation
BalanceComputation balanceComputation = new BalanceComputationFactoryImpl().create(
    areas,
    LoadFlow.find(),
    new LocalComputationManager()
);

balanceComputation.run(network, network.getVariantManager().getWorkingVariantId(), parameters)
    .thenAccept(result -> {
        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            System.out.println("Balance adjustment converged in " + result.getIterationCount() + " iterations.");
        } else {
            System.err.println("Balance adjustment failed to converge.");
        }
    }).join();
```
