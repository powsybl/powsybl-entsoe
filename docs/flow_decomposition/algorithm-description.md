# Algorithm description

The flow decomposition algorithm is based on the DC approximation, in which the losses in the network branches
are neglected, and that allows to rely on the superposition principle to assess which is the impact of any injection
on any branch flow by simple sensitivity analysis.

Below is the concrete description of the algorithm implemented in PowSyBl.

![Flow decomposition algorithm chart](/_static/img/flow_decomposition/flowDecompositionAlgorithmChart.svg)

## Net positions computation

Countries' net position computation is done once for all on base case using AC loadflow in the initial network, before any other alteration of the input.

The net position of a country is calculated as the sum of the mean leaving flow of all AC and HVDC line interconnections
(losses are shared equally between both countries).

Unpaired half lines contribute only to the net position of its physical terminal.

For paired half lines, losses are not shared equally, they are split with respect to the boundary.

The sum of net positions is equal to the opposite of the sum of the power on the boundary side of each unpaired half line.

$$\sum_{\text{zone }z} \mathrm{NP}(z) = -\sum_{\text{unpaired half line }h} P_{\text{boundary side}}(h)$$

where:
- $\mathrm{NP}(\text{zone})$ is the net position of the zone $z$.
- $P_{\text{boundary side}}(h)$ is the power on the boundary side of the unpaired half line $h$

> **_NOTE:_** If all half lines are merged, the sum of net positions is zero.

## Losses compensation

In order to mitigate the impact of DC approximation in the flow decomposition process, a dedicated step of losses compensation is implemented.

Instead of using standard power flow compensation methodology, a standard full AC power flow is run on the input network
that allows to calculate the losses on each network element.

These losses are then compensated on the sending side of each network element.

![Losses compensation on lines](/_static/img/flow_decomposition/lossesCompensationOnLine.svg)

A special treatment is done on tie lines, where instead of compensating the losses on the sending terminal, losses are
compensated at both sides. Losses are calculated for each half line individually by adding its terminal and boundary flows.
If the half lines have no shunt susceptance, this corresponds to split tie line losses proportionally to the resistance of each half line.

![Losses compensation on tie lines](/_static/img/flow_decomposition/lossesCompensationOnTieLine.svg)

## Nodal Injections partitioning

> **_NOTE:_** In PowSyBl terminology, nodal injections are injection connectables that 
> - are not paired dangling lines, 
> - are connected to the network,
> - are in the main synchronous component (for sensitivity computation reasons),
> - are not a bus bar section (because of a lack of reference injection),
> - are not shunt compensator or static var compensator (for sensitivity computation reasons).

> **_NOTE:_** In PowSyBl terminology, xnodes are unpaired dangling lines connected to the network and in the main synchronous component. 

In order to distinguish internal/loop flows and allocated flows, the nodal injections in each zone must de decomposed in two parts:
- Nodal injections for allocated flows
- Nodal injections for loop flows and internal flows
- Nodal injections for xnode flows

This decomposition is based on GLSK (Generation and Load Shift Keys). It is an input of the process that provides,
for each zone of the study a list of injections and associated factor to be used to scale the zone to a given net position.

By default, the algorithm uses so-called "Country GSK", which is an automatic GLSK that scales on all generators
proportionally to their target power setpoint.

Nodal injection decomposition is done as follows:

$$
\begin{array}{l}
\mathrm{NI}_\mathrm{AF} = \mathrm{GLSK} \cdot \mathrm{NP} \\
\mathrm{NI}_\mathrm{LIF} = \mathrm{NI} - \mathrm{NI}_\mathrm{AF} - \mathrm{NI}_\mathrm{X}
\end{array}
$$

where:
- $\mathrm{NI}$ is the vector of the network injections,
- $\mathrm{NI}_\mathrm{X}$ is the vector of the network injections from dangling lines,
- $\mathrm{NI}_\mathrm{AF}$ is the vector of allocated flow part of the network injections,
- $\mathrm{NI}_\mathrm{LIF}$ is the vector of loop flow and internal flow part of the network injections,
- $\mathrm{NP}$ is the vector of the zones' net position,
- $\mathrm{GLSK}$ is the matrix of the GLSK factors for each injection in each zone,

## Sensitivity analysis

> **_NOTE:_** In PowSyBl terminology, only two windings transformers are considered. PSTs must:
> - be connected to the network,
> - have a phase tap changer,
> - have a neutral step on the phase tap changer,
> - have a bus at each terminal,
> - be connected to the main synchronous component.
> Therefore, three windings transformers are not supported. 

> **_NOTE:_** Each element of the sensitivity computation must be connected to the main synchronous component (variables (injections) and functions (branches)). 

In order to assess the linear impact (implied by the DC approximation) of each nodal injection and phase shift transformer
on the network elements' flow, a sensitivity analysis is run.

The following matrices are calculated using [sensitivity analysis](inv:powsyblcore:std:doc#simulation/sensitivity/index) API:
- $\mathrm{PTDF}$ is the matrix of the sensitivity of the network element flow to each network injection shift,
- $\mathrm{PSDF}$ is the matrix of the sensitivity of the network element flow to each phase shift transformer tap angle change,

> **_NOTE:_** When *DIRECT_SENSITIVITY_BASED* flow partitioner is used, those matrices aren't calculated explicitly, and both sensitivity analysis and flow partitioning is done "at once".
> As a result, PTDF and PSDF matrices cannot be reported anymore.

## Flow partitioning

Based on previously calculated elements, flow partitioning can now be calculated as follows:

$$
\begin{array}{l}
\mathrm{F}_\mathrm{AF} = \mathrm{PTDF} \cdot \mathrm{NI}_\mathrm{AF} \\
\mathrm{F}_\mathrm{LIF} = \mathrm{PTDF} \cdot \mathrm{diag}(\mathrm{NI}_\mathrm{LIF}) \cdot \mathrm{AM} \\
\mathrm{F}_\mathrm{PST} = \mathrm{PSDF} \cdot \mathrm{\Delta}_\mathrm{PST} \\
\mathrm{F}_\mathrm{X} = \mathrm{PTDF} \cdot \mathrm{NI}_\mathrm{X} \\
\end{array}
$$

where:
- $\mathrm{F}_\mathrm{AF}$ is the vector of the network element allocated flow,
- $\mathrm{F}_\mathrm{LIF}$ is the matrix of the network element loop flow or internal flow for each zone,
- $\mathrm{F}_\mathrm{PST}$ is the vector of the network element PST (phase shift transformer) flow,
- $\mathrm{F}_\mathrm{X}$ is the vector of the network element xnode flow,
- $\mathrm{AM}$ is the allocation matrix, which associates each injection to its zone. $\mathrm{AM}_{ij}$ = 1 if node i is in zone j, 0 otherwise,
- $\mathrm{\Delta}_\mathrm{PST}$ is the phase shift transformers angle vector. The neutral tap position of each PST is used to compute this difference.

## Flow parts rescaling

Due to superposition principle, the sum of all the flow parts calculated previously is equal to the flow that was
calculated by the DC power flow.

However, the flow reference is the one calculated using AC power flow which is different. The final step of the algorithm
is though to rescale the different flow parts in order to ensure that the sum of the parts is equal to the initially calculated AC flow.

By default, no rescaling to the AC flow is done on the flow decomposition results.

Available rescaling modes are defined here below.

#### ACER methodology-based rescaling
The difference between reference AC flow and the sum of the parts of the decomposition is redispatched on the different
parts proportionally to their rectified linear unit ($\mathrm{ReLU}(x) = \mathrm{max}(x, 0)$).

#### Proportional rescaling
Each flow is rescaled by a proportional coefficient. The coefficient is defined by $\alpha_{\text{rescale}} = \frac{max(|AC p1|, |AC p2|)}{|DC p1|}$.
In this way, the DC flow will have the same magnitude as the AC flow.
Since we divide by the DC flow to calculate the coefficient, lines with a too small DC flow are not rescaled.

#### Max current overload rescaling
Each flow is rescaled by the same coefficient. The goal is to rescale DC flows in such a way that we find the same level of current overload as in the AC case - the maximum of the two terminals.
Therefore, we first compare AC current overloads to find which terminal (1 or 2) has the highest current overload. Then, we get the associated active power to rescale DC flows.
In case there are missing limits, we just compare AC currents instead of overloads.
Hence, the coefficient is defined as
1) With current limits:
- if $\frac{AC I1}{I_{max}1} >= \frac{AC I2}{I_{max}2}$, then $\alpha_{\text{rescale}} = \frac{\sqrt{3} \cdot AC I1 \cdot \frac{V1_{nominal}}{1000}}{|DC p1|}$
- else, $\alpha_{\text{rescale}} = \frac{\sqrt{3} \cdot AC I2 \cdot \frac{V2_{nominal}}{1000}}{|DC p1|}$
2) Without current limits:
- if $AC I1 >= AC I2$, then $\alpha_{\text{rescale}} = \frac{\sqrt{3} \cdot AC I1 \cdot \frac{V1_{nominal}}{1000}}{|DC p1|}$
- else, $\alpha_{\text{rescale}} = \frac{\sqrt{3} \cdot AC I2 \cdot \frac{V2_{nominal}}{1000}}{|DC p1|}$

Since we divide by the DC flow to calculate the coefficient, lines with a too small DC flow are not rescaled.
