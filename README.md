# QoS-Analysis-of-OTA-Update-Approaches
I have analyzed OTA-Update approaches on their Quality of Service Trade-Offs and wrote my Bachelor's Thesis about it. I mainly covered the use-case of updating car fleets. I implemented a few approaches in two prototypes that I used for load testing. In this repo you can find the paper as well as the code used for the comparison.

* __ota-tester-polling-intervals:__ Version of the OTA-Update prototype without a broker.
* __ota-tester-with-broker:__ Version of the OTA-Update prototype with a broker.
* __doc:__ Containing the architectural decisions covered in the Thesis.
* __gatling-gradle:__ Gatling load tests used for performance testing.
* __kubefiles:__ YAML files that were used for deployments on the cluster. Not maintained! Still, they should give a good idea on how the prototypes were deployed.
* __Schwendinger.pdf:__ The thesis document.

