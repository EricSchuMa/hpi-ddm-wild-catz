# Discovering INDs in Databases with Spark
## Build the project using maven packaging system (maven version 3.1):
- Scala version 2.11.11
- Java JDK version 1.8
- In "INDdiscovery" - directory run mvn clean package

## Files:
- dependencyDiscovery.scala: given a folder containing csv-files prints
the INDs among all columns in all tables in a lexicographic order
/ uses ";" delimiter