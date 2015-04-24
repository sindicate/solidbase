# Taskdef #

Here you have a minimal build.xml to make SolidBase work with Ant:

```
<project basedir="." name="sqltask-tests">

	<taskdef resource="solidbasetasks" classpath="solidbase.jar;derby-10.5.3.0.jar" />

	... targets come here ...

</project>
```

The classpath contains the solidbase.jar and the database driver jar. You can replace the Derby driver jar with the driver jar for your database. The taskdef will pick up the following Ant tasks from the solidbase.jar:

| **Task** | **Description** |
|:---------|:----------------|
| solidbase-upgrade or upgradedb | Upgrades a database |
| solidbase-sql or sb-sql | Executes one or more SQL files |

# solidbase-upgrade/dbupgrade #

This will start a database upgrade with the given database parameters and the given username:

```
<solidbase-upgrade driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:mem:testTask"
		user="sa" password=""
		upgradefile="testpatch-multiconnections.sql" target="1.1.*">
	<secondary name="queues" driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:mem:queues"
		username="sa" password="geheim" />
	<secondary name="user" username="sa" password="" />
</solidbase-upgrade>
```

The parameters in the task element represent the primary connection. The secondary elements specify extra connections available during the upgrade (See [ReferenceGuide#SELECT\_CONNECTION](ReferenceGuide#SELECT_CONNECTION.md)). In these secondary elements, the driver and url attributes are optional. If not specified they are inherited from the primary connection.

The target attribute is optional. If not specified, the default value of '`*`' will be used.

# solidbase-sql/sb-sql #

This will execute the specified single SQL file with the given database parameters and username:

```
<solidbase-sql driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:mem:testdb"
	username="sa" password="" sqlfile="test.sql" />
```

This will execute the specified SQL files with the given database parameters and username:

```
<sb-sql driver="org.hsqldb.jdbcDriver" url="jdbc:hsqldb:mem:testTask2"
		username="sa" password="">
	<secondary name="queues" url="jdbc:hsqldb:mem:queues" username="sa" password="geheim" />
	<secondary name="user" username="sa" password="" />
	<sqlfile src="testsql1.sql" />
	<sqlfile src="testsql2.sql" />
</sb-sql>
```

The parameters in the task element represent the primary connection. The secondary elements specify extra connections available during the upgrade (See [ReferenceGuide#SELECT\_CONNECTION](ReferenceGuide#SELECT_CONNECTION.md)). In these secondary elements, the driver and url attributes are optional. If not specified they are inherited from the primary connection.