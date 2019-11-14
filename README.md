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
- Project => Properties => Java Build Path => 
  - Add external JARs => /opt/ibm/ILOG/CPLEX_Studio129/cplex/lib/cplex.jar
  - cplex.jar => Native library location => Edit => /opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux
  - JRE System library => Native library location => Edit => /opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux

# Added functionalities to iFogSim
- Fix bugs
	- Creating 2 modules instead of 1
	- Memory allocation
	- MIs when multiple tuples are processed at the same time
	- MIs during migration process
	- MIs by cancelation of events
	- GUI
- Transform the hierarchical architecture into a directed graph
	- Added Communication between fog nodes at any level
	- Removed differentiations between parents/children
	- Removed all UP/DOWN directions and levels
	- Removed broadcast messages when sending tuples in the DOWN direction
- Improvement of GUI
	- Fixed bugs
	- Removed necessity to use a third party to define topologies
	- Added Add/Edit/Delete
		- Fog nodes
		- Sensors
		- Applications
		- Edges
		- Modules
		- Loops
		- Dependencies
	- Added read/write topologies using JSON files
	- Added random topology generator
	- Added settings
- Simulator
	- Created unique abstract resource provisioner class both for RAM and BW
	- Created overbooking variable inside RAM, BW, and PE provisioners and remove extra classes for that purpose
	- Removed any hard coded dependency with names (e.g., "cloud")
	- Removed Fog broker. It was only needed because of the ID. Now ID is the client (FogDevice) id
	- Cost now takes into account MIPS, STRG, RAM, BW usage
	- Energy consumption now takes into account MIPS, and Energy spent on mobile communications
	- Organized code to allow using only one main
	- Adapted original examples from iFogSim
	- Added several result analysis features
- Physical architecture
	- Added the Dijkstra method to find the network distance to between pairs of nodes
	- Added the functionality to deploy modules in any fog node
	- Added bandwidth to the links (price per bandwidth remains inside the fog node, bandwidth inside each node still exists but is not used)
- Applications
	- Removed MIPS from AppModule GUI (now its computed based on tuples CPU size and its frequency and probability)
	- BW from AppModule is unused (now Bw is defined between modules based on tuples NW size and its frequency and probability)
	- Now application are individual instead of being multiplayer
	- Routing table is made module to module rather than node to node
	- Clients can now have multiple applications which can be both single or multiplayer
	- Added possible positioning so that clients do not process applications from other clients
	- Added application modules features
		- Client (needs to be processed inside the client)
		- Global (needs to be processed outside any client)
		- Normal (can be processed in any fog node or even inside the client)
	- Added deadline to application loops (can have or not)
	- Added migration deadline to the application modules
- Algorithms
	- Removed iterative module placement
	- Added abstract class to allow the implementation of multiple optimization algorithms
	- Added multiple objective optimization algorithms for module placement
		- CPLEX from IBM
		- Random Search Algorithm
		- Genetic Algorithm
		- Brute Force Algorithm
	- Added Matlab-like plot feature to print algorithms iterations
	- Added output excel exporter
- Mobility
	- Added migration to the optimization algorithms
	- Added migration to the simulator (VM full migration => STRG + RAM)
	- Migration takes into account the network usage and routing, which was not in the MyiFogSim simulator
	- Added counter of dropped tuples during migrations to check the degradation of QoS
	- Added mobile communication model (both path loss and bandwidth)
	- Added a mobile communication model in which the transmitters always use max TX power (200mW = 23dBm) and bandwidth is variable based on distance and modelation used (this step uses both path loss and modelation models)
	- Mobile nodes are connected to one and only one static fog node (the closest one)
	- Added mobility patterns
		- Random
		- Rectangle
		- Static
