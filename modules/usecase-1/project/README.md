# Fuse Use Case Project

## Maven Modules

* parent : Contain dependencies, properties definitions and common maven plugins used by the modules
* routing : Apache Camel routes. Unit test.
* features : Apache Karaf XML features file

## Build project

```
mvn clean install
```

## Deploy it on JBoss Fuse

- Download and Install JBoss Fuse 6.2.1
- Open a terminal and move to the home directory of the distribution
- Launch JBoss Fuse 

```
./bin/fuse
```

- When the Console appears, then deploy the XML features file and install the use case

```
addurl mvn:org.fuse.usecase1/features/1.0/xml/features
features:install usecase-camel-bindy-json
```

- Copy/paste the `src/data/inbox/customers.csv` file to the `src/data/inbox` folder created under the home directory of the JBoss Fuse server
  and check that 3 files have been created under `src/data/outbox` and one under `src/data/error`
  
## Use Fabric
  
- Install Fabric within JBoss Fuse
 
```
fabric:create
```

- Execute this maven command from the `routing` folder 

```
mvn fabric8:deploy
```

- Create a container and deploy the use case profile

```
fabric:container-create-child root usecase1
fabric:container-add-profile usecase1 fuse-usecase1
```

- Copy/paste the `src/data/inbox/customers.csv` file to the `src/data/inbox` folder created under the usecase1 folder of the instance created into the 
  `instances` directory and check that 3 files have been created under `src/data/outbox` and one under `src/data/error`
