# FogComputingSim
Fog Computing Simulator

# Relevant Links
https://github.com/JoseCVieira/Thesis---Fog-and-Cloud-Computing-Optimization-in-Mobile-IoT-Environments

# Abstract
With the surge of ubiquitous demand on high-complexity and quality of mobile IoT services, new computing paradigms have emerged. Motivated by the long and unpredictable end-to-end communication latency experienced in cloud computing as well as the rapid growth of mobile traffic, fog computing emerges as the most comprehensive and natural paradigm to support real-time applications and to get more efficient with the data sent to the cloud. From the performed analysis, the lack of research in dynamic environments regarding both the client and fog nodes is noticeable. Moreover, most of the existing proposed schemes only consider few specific objectives for the system optimization as well as static computing resources. In the present work, a novel fog computing architecture has been designed and evaluated with the purpose of finding a solution to the aforementioned issues. A novel optimization problem formulation is also proposed in order to match the characteristics of the proposed architecture. A set of optimization algorithms were developed to solve the formulated problem. Moreover, the proposed architecture has been successfully implemented in a suitable developed simulation toolkit. The performance of the optimization algorithms was assessed in different static and mobile scenarios using the QoS offered to the clients and the system provider objectives as the main metrics. It is observed that the proposed architecture effectively helps to improve the offered QoS to its users in mobile and static environments while meeting the system provider objectives.

# Architecture
![alt text](https://github.com/JoseCVieira/Thesis/blob/master/fog_architecture.png)

# CPLEX
- Download IBM student version
- chmod +x cplex_studioXXX.linux-x86.bin
- ./cplex_studioXXX.linux-x86.bin
- /opt/ibm/ILOG/CPLEX_Studio129/cplex/lib => cplex.jar
- Project => Properties => Java Build Path => Add external JARs => /opt/ibm/ILOG/CPLEX_Studio129/cplex/lib/cplex.jar
- Project => Properties => Java Build Path => cplex.jar => Native library location => Edit => /opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux
- Project => Properties => Java Build Path => JRE System library => Native library location => Edit => /opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux

# Added functionalities to iFogSim
- 
