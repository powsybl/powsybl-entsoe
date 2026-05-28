# Network Area

```{toctree}
:hidden:
network_area-definitions.md
```

## Introduction

In power system modeling, a **Network Area** defines a geographical or functional zone within a power network. It serves primarily as a **net position provider**, allowing for the calculation of the balance between energy production and consumption within that specific zone.

The `NetworkArea` interface in PowSyBl-ENTSO-E provides a consistent way to interact with these zones, regardless of how they are defined (by countries, voltage levels, etc.) or how their net position is calculated.

## Main Concepts

A Network Area is characterized by:
- A set of internal components (generators, loads, buses).
- A method to compute its **Net Position**.

### Net Position Convention

The net position sign convention used across all implementations is:
- **Positive (+)**: The area is exporting energy (flows are leaving the area).
- **Negative (-)**: The area is importing energy (flows are feeding the area).

The net position is expressed in MegaWatts (MW).

## Implementation Types

There are two main strategies for defining an area and computing its net position:

1.  **Border-based**: The net position is calculated by summing the physical flows on all branches crossing the area's perimeter.
2.  **Injection-based**: The net position is calculated as the sum of all internal productions minus the sum of all internal consumptions.

For each strategy, implementations exist to define areas based on:
- **Countries**: The area includes all elements belonging to one or more specified countries.
- **Voltage Levels**: The area includes all elements belonging to a list of specified voltage level IDs.
