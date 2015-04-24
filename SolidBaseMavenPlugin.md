# pom.xml #

Here you have a minimal pom.xml that enables both a database upgrade and the execution of an SQL file with the SolidBase Maven plugin:

```
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

	<modelVersion>4.0.0</modelVersion>
	
	<groupId>solidbase</groupId>
	<artifactId>mavenplugintest</artifactId>
	<version>1.0</version>
	<packaging>pom</packaging>

	<pluginRepositories>
		<pluginRepository>
			<id>solidbase</id>
			<name>SolidBase Repository</name>
			<layout>default</layout>
			<url>http://solidbase.googlecode.com/svn/repository</url>
			<releases><enabled>true</enabled></releases>
			<snapshots><enabled>false</enabled></snapshots>
		</pluginRepository>
	</pluginRepositories>

	<build>
		<plugins>
			<plugin>
				<groupId>solidbase</groupId>
				<artifactId>solidbase</artifactId>
				<version>1.6.5</version>
				<dependencies>
					<dependency>
						<groupId>hsqldb</groupId>
						<artifactId>hsqldb</artifactId>
						<version>1.8.0.7</version>
					</dependency>
				</dependencies>
				<configuration>
					<driver>org.hsqldb.jdbcDriver</driver>
					<url>jdbc:hsqldb:mem:test</url>
					<username>sa</username>
					<password></password>
					<upgradefile>upgrade-hsqldb-example.sql</upgradefile>
					<target>1.0.*</target>
					<sqlfile>hsqldb-test.sql</sqlfile>
					<connections>
						<secondary>
							<name>queues</name>
							<url>jdbc:hsqldb:mem:queues</url>
							<username>sa</username>
							<password></password>
						</secondary>
					</connections>
				</configuration>
			</plugin>
		</plugins>
	</build>
  
</project>
```

This does not actually upgrade the database. You have to enter the following command to upgrade the database:

```
mvn solidbase:upgrade
```

This will start a database upgrade with the given database parameters and the given username. The parameters in the configuration element represent the primary connection. The secondary elements specify extra connections available during the upgrade (See [ReferenceGuide#SELECT\_CONNECTION](ReferenceGuide#SELECT_CONNECTION.md)). In these secondary elements, the driver and url attributes are optional. If not specified they are inherited from the primary connection.

The target parameter is optional. If not specified, the default value of '`*`' will be used.

Both an upgrade file and an SQL file are configured. It depends on the commands you give which one is used. You can execute the configured SQL file with the following command:

```
mvn solidbase:sql
```

This will execute the given SQL file with the given database parameters and username. The parameters in the configuration element represent the primary connection. The secondary elements specify extra connections available during the upgrade (See [ReferenceGuide#SELECT\_CONNECTION](ReferenceGuide#SELECT_CONNECTION.md)). In these secondary elements, the driver and url attributes are optional. If not specified they are inherited from the primary connection.

# Binding SolidBase to a Maven build phase #

By adding the following XML below the 'configuration' element:

```
<executions>
	<execution>
		<id>solidbase-upgrade</id>
		<phase>pre-integration-test</phase>
		<goals><goal>upgrade</goal></goals>
	</execution>
</executions>
```

the database will be upgraded automatically during the 'pre-integration-test' phase. Thus, the following command:

```
mvn install
```

will automatically upgrade the database because the 'pre-integration-test' phase is part of the Maven 'install' command.