Having a hard time juggling sql files to keep all of your databases up-to-date and in-sync?

**SolidBase is a database change management and version control tool that uses annotated SQL.**


---


## Strengths & Features ##

  * Database changes are written in SQL, see [examples](#Examples.md);
  * Keeps developer, test AND production databases identical;
  * Existing data can be maintained and transformed as needed;
  * Imports CSV data fast, see the example [output](#Example_output_from_CSV_import.md);
  * Real version control, not a bunch of changes;
  * Version information is stored in the database;
  * Multiple upgrade paths can be defined to support the code branching strategies used during systems development. Switching from one path to another is also supported;
  * Database vendor agnostic, only JDBC connections needed;
  * Supports controlling multiple databases and/or schemas with one version number;
  * Supports custom commands through plugins;
  * Ant tasks and Maven plugins included in the jar;
  * Also operates from the command prompt with an optional properties file.

In addition to upgrading databases, SolidBase also executes SQL files using the extended features of SolidBase (for example, fast import of inline CSV data, sending statements to different target databases, or ignoring specific SQL errors).

## News ##

  * **2012-03-05: Released [SolidBase 1.6.7](http://code.google.com/p/solidbase/downloads/detail?name=solidbase-1.6.7.jar).** IMPORT CSV can now import into tables with schema names.

> The complete change log is [here](http://solidbase.googlecode.com/svn/branches/releases/1.6/CHANGELOG.TXT).

  * **2011-10-03: Released [SolidBase 1.6.6](http://code.google.com/p/solidbase/downloads/detail?name=solidbase-1.6.6.jar).** Problem with PostgreSQL fixed.

  * **2011-07-25: Added a new Getting Started**: [using SolidBase with a solidbase.properties file](GettingStartedCommandLineWithProperties.md).

  * **2011-01-14: Released [SolidBase 1.6.5](http://code.google.com/p/solidbase/downloads/detail?name=solidbase-1.6.5.jar).** Fixed minor issue that came up when we tried to compile a Java procedure on Oracle.

  * **2010-11-11: Released SolidBase 1.6.4.** Further IMPORT CSV improvements and external CSV files can now be imported too. Removed an accidental dependency on java 6, so that SolidBase will run on java 5 again.

  * **2010-10-11: Released SolidBase 1.6.3.** Autocommit is now off during the execution of SQL files (for better performance), so you need to explicitly add commit statements to your SQL files. Upgrade files however are always processed with autocommit on.

  * **2010-10-04: Released SolidBase 1.6.2.**

  * **2010-08-15: Released SolidBase 1.6.1.** The default delimiter is changed to a trailing ; . Introduced new annotations SETUP and TRANSIENT, the old INIT and SESSIONCONFIG will be removed later.

  * **2010-06-06: Created the [SolidBase User](http://groups.google.com/group/solidbase-user) mailing list.** You can post questions and comments there.

  * **2010-06-05: Updated the Getting Started pages.**

  * **2010-05-31: Released SolidBase 1.6.0.** Many enhancements, for example the ability to execute annotated SQL files, the ability to import inline CSV data, and a configurable SQL delimiter.

  * **2010-03-07: Released SolidBase 1.5.3.** One small bug fix.

  * **2010-02-18: Added a new Getting Started**: [the SolidBase Maven plugin](GettingStartedMaven.md).

  * **2010-02-17: Added a new Getting Started**: [the SolidBase Ant task](GettingStartedAnt.md).

  * **2010-02-15: Added the first Getting Started**: [SolidBase on the command-line](GettingStartedCommandLineWithArguments.md).

  * **2010-02-15: Released SolidBase 1.5.2.** This release has some commandline modifications that were needed for the first Getting Started.

  * **2010-01-31: Released SolidBase 1.5.1.** This release adds the secondary connections capability which the Maven plugin lacked.

> Downgrade is still experimental, which means that the implementation and its use are subject to change.


---


## Getting Started ##

Here are a couple of Getting Started pages:
  * [Getting Started with SolidBase on the command-line](GettingStartedCommandLineWithArguments.md);
  * [Getting Started with SolidBase using a solidbase.properties file](GettingStartedCommandLineWithProperties.md);
  * [Getting Started with the SolidBase Ant task](GettingStartedAnt.md);
  * [Getting Started with the SolidBase Maven plugin](GettingStartedMaven.md).

I'm currently working on a [Reference Guide](ReferenceGuide.md). If you have any questions or comments you can post them on the [SolidBase User mailing list](http://groups.google.com/group/solidbase-user). Feature requests or bug reports can be found [http:issues/list here].

## Examples ##

  * An example Ant [build.xml](http://code.google.com/p/solidbase/source/browse/branches/releases/1.6/dist/build.xml).
  * An example Maven [pom.xml](http://code.google.com/p/solidbase/source/browse/branches/releases/1.6/dist/pom.xml).
  * An example [solidbase.properties](http://code.google.com/p/solidbase/source/browse/branches/releases/1.6/dist/solidbase.properties) for stand-alone execution.

Below an example of a database change. The complete example file can be found [here](http://code.google.com/p/solidbase/source/browse/branches/releases/1.6/dist/upgrade-hsqldb-example.sql).

You can find example upgrade scripts for Oracle, MySQL, Derby/JavaDB and HSQLDB/HyperSQL [here](http://code.google.com/p/solidbase/source/browse/branches/releases/1.6#1.6/dist).

```
--* UPGRADE "" --> "1.0.1"

--* SECTION "Creating table USERS"
CREATE TABLE USERS
(
        USER_ID INT IDENTITY,
        USER_USERNAME VARCHAR NOT NULL,
        USER_PASSWORD VARCHAR NOT NULL
);

--* SECTION "Inserting admin user"
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '*****' );

--* SECTION "Inserting user"
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'rené', '*****' );

--* /UPGRADE
```

And here is the output from the example upgrade file:

```
G:\PROJECTS\BUILDS\solidbase\dist>ant upgrade
Buildfile: G:\PROJECTS\BUILDS\solidbase\dist\build.xml

upgrade:
[upgradedb] SolidBase v1.6.2-rev491 (http://solidbase.org)
[upgradedb]
[upgradedb] Opening file 'G:\PROJECTS\BUILDS\solidbase\dist\upgrade-hsqldb-example.sql'
[upgradedb]     Encoding is 'ISO-8859-1'
[upgradedb] Connecting to database...
[upgradedb] The database is unmanaged.
[upgradedb] Setting up control tables to "1.1"
[upgradedb] Upgrading to "1.0.1"
[upgradedb]     Creating table USERS...
[upgradedb]     Inserting admin user...
[upgradedb]     Inserting user...
[upgradedb] Upgrading "1.0.1" to "1.0.2"
[upgradedb]     Creating queue...
[upgradedb] The database is upgraded.
[upgradedb]
[upgradedb] Current database version is "1.0.2".

BUILD SUCCESSFUL
Total time: 0 seconds
G:\PROJECTS\BUILDS\solidbase\dist>
```

## Example output from CSV import ##

It takes only 23 seconds to import 1 million records.

```
G:\PROJECTS\bigpatch>ant testdata2
Buildfile: G:\PROJECTS\bigpatch\build.xml

testdata2:
   [sb-sql] SolidBase v1.6.4-rev584 (http://solidbase.org)
   [sb-sql]
   [sb-sql] Opening file 'G:\PROJECTS\bigpatch\folder\insertACNPCE2.sql'
   [sb-sql]     Encoding is 'ISO-8859-1'
   [sb-sql] Connecting to database...
   [sb-sql] Importing...
   [sb-sql] Inserted 1000000 records
   [sb-sql] Execution complete.
   [sb-sql]

BUILD SUCCESSFUL
Total time: 23 seconds
G:\PROJECTS\bigpatch>
```

Oracle XE runs on Ubuntu 10 on VirtualBox 3.2.8 on Windows 7 on an AMD Phenom(tm) 8650 Triple-Core Processor (3x 2300 Mhz). SolidBase runs on the same Windows 7 machine.

The CSV data contains 12 columns, 1 million lines and is 87 MBytes in size.

The database table has no indexes, no constraints and no foreign keys.
These would considerably slow down the insert rate, and I wanted to show the maximum achievable.


---


&lt;wiki:gadget url="http://www.ohloh.net/p/solidbase/widgets/project\_basic\_stats.xml" height="220" border="1" /&gt;