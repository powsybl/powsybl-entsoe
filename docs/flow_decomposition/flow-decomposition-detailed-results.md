# Flow decomposition computation detailed results

It is possible to get intermediate detailed results of a flow decomposition computation by attaching observers to the FlowDecompositionComputer.
Observers are notified of the following events:
* when the computation starts
* when the base case starts to be computed
* when a contingency computation starts
* when GLSK are computed
* when net positions are computed (for base case computation)
* when the nodal injection matrix is computed (for base case or contingency)
* when the PTDF matrix is computed (for base case or contingency)
* when the PSDF matrix is computed (for base case or contingency)
* when the AC loadflow is computed (for base case or contingency)
* when the DC loadflow is computed (for base case or contingency)
* when the flow decomposition results before rescaling are being built (for base case or contingency)
* when the computation is done

After AC and DC loadflows the observer has access to the network at that stage, as well as the loadflow result.
In this manner, it is possible to retrieve any information of the network after loadflow 
calculations. Additionally, the observer has access to the decomposed flows prior to the 
rescaling step, enabling more effective testing and analysis of the available rescaling algorithms.

Note that these observers are meant to be used for testing purposes only.
Using observers impacts calculation performance and therefore are not suitable in production environment.

Note also that PTDFs and PSDFs respect the [flow sign convention](../flow_decomposition/flow-decomposition-outputs.md#flow-sign-conventions).
