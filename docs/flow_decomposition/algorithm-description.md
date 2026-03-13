# Flow decomposition algorithms

This module provides algorithms for **power flow decomposition** in transmission networks.

Two independent flow partitioning implementations are available:

- **Power Flow Colouring (PFC)**: implementation based on nodal injection
  decomposition and sensitivity analysis and described in [CORE decision by ACER](https://eepublicdownloads.entsoe.eu/clean-documents/nc-tasks/CORE%20-%2074%20-%20ACER%20decision%20-%20Annex%201.pdf). 
- **Full Line Decomposition (FLD)**: alternative implementation based on a direct
  line-oriented decomposition, described in the article [“The full line decomposition method - a further development for causation-based cost sharing”](https://www.e-cigre.org/publications/detail/cse009-cse-009.html)
  by M. Pavesi, J. Casteren and S. A. Graaff, CIGRE Science and Engineering, pp. 27–43, Oct. 2017.

The following sections describe the flow decomposition algorithm in detail.

## Algorithm hypothesis

The flow decomposition algorithm is based on the DC approximation, in which the losses in the network branches
are neglected, and that allows to rely on the superposition principle to assess which is the impact of any injection
on any branch flow by simple sensitivity analysis.

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

## Power Flow Colouring

Below is the concrete description of the algorithm implemented in PowSyBl.

![Flow decomposition algorithm chart](/_static/img/flow_decomposition/flowDecompositionAlgorithmChart.svg)

### Net positions computation

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

### Nodal Injections partitioning

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

### Sensitivity analysis

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

### Flow partitioning

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

## Full Line Decomposition

### Implementation status

The Full Line Decomposition (FLD) method is currently in **experimental** status. Some features are not yet implemented,
including (but not limited to):
- **Three Windings Transformer** handling,
- **HVDC** handling,

Moreover, implementation and interface may change depending on the feedbacks.

### Principle

The **Full Line Decomposition (FLD)** approach provides an alternative formulation
of flow partitioning in which the decomposition is performed **directly at branch
level**, instead of starting from a nodal injection partition.

The method is based on a DC representation of the network and on the superposition
principle, similarly to Power Flow Colouring, but it does not rely on a prior
decomposition of nodal injections using GLSKs.

### High-level algorithm description

At a high level, the algorithm proceeds as follows:

1. A DC power flow is computed, providing a reference active power flow on each branch.
2. For each branch, the physical flow is decomposed into elementary contributions
   associated with zones and inter-zonal exchanges.
3. These contributions are propagated consistently through the network topology,
   ensuring that Kirchhoff’s laws are respected.
4. PST flows are calculated exactly as in PFC methodology
5. For each branch, the aggregation of all contributions exactly reconstructs
   the reference DC flow.

The methodology leads to a complete and unambiguous decomposition of branch flows.

### Downstream distribution matrix $A_d$

In the Full Line Decomposition framework, the **downstream distribution matrix**
$A_d$ models how power flowing through each node is distributed among its downstream
neighbours, according to the **proportional sharing principle**.

Let $F_{i \rightarrow j}$ denote the active power flow from node $i$ to node $j$
in the DC reference state, and let $P_j$ be the total incoming power at node $j$.
The elements of the downstream distribution matrix are defined as:

$$
(A_d)_{ij} =
\begin{cases}
\dfrac{F_{i \rightarrow j}}{P_j} & \text{if } F_{i \rightarrow j} > 0 \\
0 & \text{otherwise}
\end{cases}
$$

Each column of $A_d$ represents how the power flowing through a node is split among
its upstream neighbours. By construction, $A_d$ depends only on the network topology
and on the reference branch flow directions.

When the network is connected and free of pathological flow patterns, the matrix
$A_d$ is invertible. Its inverse $A_d^{-1}$ describes how an injection at one node
propagates downstream through the network and is a key ingredient for computing
the **Power Exchange (PEX) matrix**.

### Role of the PEX matrix

The **Power Exchange (PEX) matrix** is central in the methodology.
The PEX matrix represents the power exchanges between pairs of nodes (or, after aggregation,
between pairs of zones) and is constructed on the **full network**.

At nodal level, the PEX matrix can be expressed in a simplified form as:

$$
\mathrm{PEX}_{ij} =
\frac{P_{G,i}}{P_i} \cdot (A_d^{-1})_{ij} \cdot P_{D,j}
$$

where:
- $P_{G,i}$ is the generation at node $i$,
- $P_{D,j}$ is the demand at node $j$,
- $P_i$ is the total nodal power flowing through node $i$,
- $A_d^{-1}$ is the inverse downstream distribution matrix derived from the network topology
  and branch power flows.

The term $(A_d^{-1})_{ij}$ represents the share of power injected at node $i$ that reaches
node $j$ according to the **proportional sharing principle**.

The nodal PEX matrix can be aggregated at zonal level by summing all nodal exchanges between
nodes belonging to the corresponding zones. Combined with node-to-node or zonal PTDFs,
the PEX matrix allows computing the contribution of each zone-to-zone exchange to the
flow on any given branch.

XNodes are integrated in PEX matrix on specific nodes, with dedicated edge associated to zone $X$ in the final decomposition,
that allows independent handling afterwards.

$$
\begin{array}{l}
\mathrm{F}_\mathrm{AF}[l] = \sum_{i \in B, j \in C, B \neq C} \mathrm{PTDF}_{l,i2j} \cdot \mathrm{PEX}_{ij} \\
\mathrm{F}_\mathrm{LF}[l,B] = \sum_{i,j \in B, B\neq A} \mathrm{PTDF}_{l,i2j} \cdot \mathrm{PEX}_{ij} \\
\mathrm{F}_\mathrm{IF}[l] = \sum_{i,j \in A} \mathrm{PTDF}_{l,i2j} \cdot \mathrm{PEX}_{ij} \\
\mathrm{F}_\mathrm{XF}[l] = \sum_{i \in X, j \in * \textrm{ or } i \in *, j \in X} \mathrm{PTDF}_{l,i2j} \cdot \mathrm{PEX}_{ij} \\
\mathrm{F}_\mathrm{PST} = \mathrm{PSDF} \cdot \mathrm{\Delta}_\mathrm{PST} \\
\end{array}
$$

where:
- $\mathrm{F}_\mathrm{AF}[l]$ is the network element $l$ allocated flow,
- $\mathrm{F}_\mathrm{LF}[l,A]$ is the network element $l$ loop flow for zone $B$,
- $\mathrm{F}_\mathrm{IF}[l]$ is the network element $l$ internal flow,
- $\mathrm{F}_\mathrm{XF}[l]$ is the network element $l$ xNode flow,
- $\mathrm{F}_\mathrm{PST}$ is the vector of the network element PST (phase shift transformer) flow,
- $\mathrm{\Delta}_\mathrm{PST}$ is the phase shift transformers angle vector. The neutral tap position of each PST is used to compute this difference.

### Properties

- The decomposition is performed **per branch**, without explicit nodal GLSK-based scaling.
- The sum of all flow components equals the reference DC flow.
- The result is less sensitive to modelling choices related to nodal injection allocation.
- Two implementations are available, one fast that directly computes the flow parts as a direct sensitivity calculation,
  and one slow that uses the full sensitivity matrix to compute the flow parts.

An important limitation exists today on this implementation: xNode flows are not calculated yet.

## Flow parts rescaling

Due to superposition principle, the sum of all the flow parts calculated previously is equal to the flow that was
calculated by the DC power flow.

However, the flow reference is the one calculated using AC power flow which is different. The final step of the algorithm
is though to rescale the different flow parts in order to ensure that the sum of the parts is equal to the initially calculated AC flow.

By default, no rescaling to the AC flow is done on the flow decomposition results.

Available rescaling modes are defined here below.

### ACER methodology-based rescaling
The difference between reference AC flow and the sum of the parts of the decomposition is redispatched on the different
parts proportionally to their rectified linear unit ($\mathrm{ReLU}(x) = \mathrm{max}(x, 0)$).

### Proportional rescaling
Each flow is rescaled by a proportional coefficient. The coefficient is defined by $\alpha_{\text{rescale}} = \frac{max(|AC p1|, |AC p2|)}{|DC p1|}$.
In this way, the DC flow will have the same magnitude as the AC flow.
Since we divide by the DC flow to calculate the coefficient, lines with a too small DC flow are not rescaled.

### Max current overload rescaling
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

## Comparison of flow partitioning implementations

The two flow partitioning implementations differ mainly in their conceptual approach.

Power Flow Colouring is well suited for zonal analyses where injection allocation
plays a central role.  
Full Line Decomposition provides a more direct interpretation of physical flows on
network elements and reduces the dependence on nodal allocation assumptions.

There is no reference methodology as physical interpretation of the associated results is difficult, and the
choice between one or another methodology depends on the assumptions done and the level of dependency on the
inputs.
