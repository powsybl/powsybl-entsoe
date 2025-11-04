# Configuration

## Dedicated parameters

| Name                                     | Type    | Default value | Description                                                                                                                                                                                                                                                                                                                                                                  |
|------------------------------------------|---------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enable-losses-compensation               | boolean | false         | When set to true, adds losses compensation step of the algorithm. Otherwise, all losses will be compensated using chosen power flow compensation strategy.                                                                                                                                                                                                                   |
| losses-compensation-epsilon              | double  | 1e-5          | Threshold used in losses compensation step of the algorihm. If actual losses are below the given threshold on a branch, no injection is created in the network to compensate these losses. Used to avoid creating too many injections in the network. May have an impact in overall algorithm performance and memory usage.                                                  |
| sensitivity-epsilon                      | double  | 1e-5          | Threshold used when filling PTDF and PSDF matrices. If a sensitivity is below the given threshold, it is set to zero. Used to keep sparse matrices in the algorithm. May have an impact in overall algorithm performance and memory usage.                                                                                                                                   |
| rescale-mode                             | enum    | NONE          | Use NONE if you don't want to rescale flow decomposition results. Use ACER_METHODOLOGY for the ACER methodology rescaling strategy. Use PROPORTIONAL for a proportional rescaling. Use MAX_CURRENT_OVERLOAD for a rescaling based on AC current overloads. See [Flow parts rescaling](../flow_decomposition/algorithm-description.md#flow-parts-rescaling) for more details. |
| proportional-rescaler-min-flow-tolerance | double  | 1e-6          | Option used from rescale modes PROPORTIONAL and MAX_CURRENT_OVERLOAD. Defines the minimum DC flow required in MW for the rescaling to happen.                                                                                                                                                                                                                                |    
| dc-fallback-enabled-after-ac-divergence  | boolean | true          | Defines the fallback behavior after an AC divergence Use True to run DC loadflow if an AC loadflow diverges (default). Use False to throw an exception if an AC loadflow diverges.                                                                                                                                                                                           |
| sensitivity-variable-batch-size          | int     | 15000         | When set to a lower value, this parameter will reduce memory usage, but it might increase computation time.                                                                                                                                                                                                                                                                  |
| flow-partitioner                         | enum    | MATRIX_BASED  | Use DIRECT_SENSITIVITY_BASED for better performance. However, nodal PTDF aren't explicitely calculated anymore and won't be reported. Use MATRIX_BASED if all detailed node PTDF needs to be reported.                                                                                                                                                                       |

## Impact of existing parameters

Any implementation of load flow provider and sensitivity analysis provider can be used, as the entire algorithm only
relies on common loadflow API and sensitivity analysis API.

Thus, the flow decomposition algorithm relies on [load flow parameters](inv:powsyblcore:*:*#loadflow-generic-parameters)
and [sensitivity analysis parameters](inv:powsyblcore:*:*#sensitivity-generic-parameter).

## Open load flow parameters

Some open load flow parameters have been modified in the tests. They are listed below.

| Name                              | Default value in tests       | Reason                                                                                                    |
|-----------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------|
| balanceType                       | PROPORTIONAL_TO_GENERATION_P | A lot of tests are based on UCTE format. The PMax values are wrong. It is better to balance on P instead. |
| slackBusPMaxMismatch              | 1e-3                         | We need precision in our tests. The OLF default value favors performance.                                 |
| newtonRaphsonStoppingCriteriaType | PER_EQUATION_TYPE_CRITERIA   | We need precision in our tests. The OLF default value favors performance.                                 |
| maxActivePowerMismatch            | 1e-3                         | We need precision in our tests. The OLF default value favors performance.                                 |
