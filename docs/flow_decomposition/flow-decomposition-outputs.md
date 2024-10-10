# Flow decomposition outputs

## Network element parts

For each network element of interest, flow decomposition outputs contain the following elements:
- Reference flow : active power flow that is considered as the reference for the decomposition. It is actually equal
  to the sum of all the flow parts calculated by the algorithm. to the sum of all the flow parts calculated by the algorithm.
  Reference AC flows are available on terminal 1 and 2.
- Allocated flow : allocated flow part of the network element's flow.
- Internal flow : internal flow part of the network element's flow. It is calculated as the loop flow from the country
  which network element is part of (interconnections are considered as part of no specific country, so will always have an internal flow to 0).
- Loop flows : map of the loop flow part of the network element's flow for each zone.
- PST flow : PST flow part of the network element's flow.
- Xnode flow : flow part due to all unmerged interconnections and HVDC connections modelled as dangling lines in IIDM.

## Flow sign conventions

On one hand, the reference flows are oriented from side 1 to side 2 of the associated IIDM branch. A positive reference flow implies
a flow from side 1 to side 2, while a negative one means a flow from side 2 to side 1.
For coherence and simplicity purposes, the entire algorithm (except some [rescaling methods](../flow_decomposition/algorithm-description.md#flow-parts-rescaling)) is based on the side 1.

On the other hand, all flow parts (allocated flow, internal flow, loop flows and PST flow) are oriented in the branch
flow convention. A positive flow part tends to increase the absolute flow on the branch (i.e. a burdening flow), while a
negative one tends to decrease the absolute flow on the branch (i.e. a relieving flow).
