# Contents #
## Using SolidBase ##
  * [SolidBase Ant tasks](SolidBaseAntTasks.md)
  * [SolidBase Maven plugin](SolidBaseMavenPlugin.md)
## File encoding & upgrade file definition ##
  * [File encoding](#File_encoding.md)
  * [Differences between upgrade files and SQL files](#Differences_between_upgrade_files_and_SQL_files.md)
  * [Upgrade file definition](#Upgrade_file_definition.md)
## Annotations ##
  * [IF HISTORY](#IF_HISTORY.md) (upgrade only)
  * [IGNORE SQL ERROR](#IGNORE_SQL_ERROR.md)
  * [JDBC ESCAPE PROCESSING](#JDBC_ESCAPE_PROCESSING.md)
  * [SECTION](#SECTION.md)
  * [SELECT CONNECTION](#SELECT_CONNECTION.md)
  * [SET DELIMITER](#SET_DELIMITER.md)
  * [SET USER](#SET_USER.md)
  * [SKIP](#SKIP.md)
  * [TRANSIENT](#TRANSIENT.md) (upgrade only)
## Commands ##
These are commands that are interpreted by SolidBase, not the database.
  * [ASSERT EMPTY](#ASSERT_EMPTY.md)
  * [ASSERT EXISTS](#ASSERT_EXISTS.md)
  * [IMPORT CSV](#IMPORT_CSV.md)
  * [PRINT SELECT](#PRINT_SELECT.md)


---

## File encoding ##
An upgrade file or SQL file should have the following first line:
```
--* ENCODING "<encoding>"
```
### Example ###
```
--* ENCODING "UTF-8"
```
This indicates to SolidBase what the character encoding of the input file is. The supported encodings vary between different implementations of the Java 2 platform, but every implementation is required to support US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE and UTF-16. See http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html.


---

## Differences between upgrade files and SQL files ##
There are a few differences between upgrade files and SQL files:
| DEFINITION | Obviously, you do not add a DEFINITION to an SQL file |
|:-----------|:------------------------------------------------------|
| COMMIT | In an SQL file you need to add COMMITs, in an upgrade file COMMIT is automatic |
| IF HISTORY | This annotation does not work in an SQL file |
| TRANSIENT | This annotation does not work in an SQL file |


---

## Upgrade file definition ##
Below I will give some examples with respect to versions and branches. In the examples I use version numbers with 3 digits. The first 2 digits represent a specific release of the database or system where the database is a part of. The 3 digits together represent the consecutive versions within one release.

An upgrade file needs to start with a definition (directly below --`*` ENCODING if present). Here is an initial definition:
```
--* DEFINITION
--*     SETUP "" --> "1.1"
--*     UPGRADE "" --> "1.0.1"
--* /DEFINITION
```
As you have learned from the GettingStarted, the setup to 1.1 is mandatory. It tells SolidBase what the structure of the control tables is.

Work in progress on the next version:
```
--* DEFINITION
--*     SETUP "" --> "1.1"
--*     UPGRADE "" --> "1.0.1"
--*     UPGRADE OPEN "1.0.1" --> "1.0.2"
--* /DEFINITION
```
This example shows that you can work on a specific version for a longer period of time. Changes can be added as development progresses and because SolidBase registers the number of statements that have been succesfully executed it knows which statements are added. When the version is complete and you need to finalize it, simply remove OPEN from the definition.

You do not HAVE to use this OPEN feature, you can also choose to give every individual change a new version number.

After some time, a release/stable branch is created for release 1.0 which is about to be released, and work for a future release 1.1 is done on the main development branch:
```
--* DEFINITION
--*     SETUP "" --> "1.1"
--*     UPGRADE "" --> "1.0.1"
--*     UPGRADE "1.0.1" --> "1.0.2"
--*         UPGRADE "1.0.2" --> "1.0.3" // Start of maintenance branch for release 1.0
--*         SWITCH "1.0.3" --> "1.0.2" // Return to the main development branch
--*     UPGRADE "1.0.2" --> "1.1.1" // The main development branch continues with the future release 1.1
--* /DEFINITION
```
From version 1.0.2 two upgrade paths are defined: one to version 1.0.3, and the other one to 1.1.1. You see a SWITCH from 1.0.3 back to 1.0.2. SolidBase will try to avoid following a path with a SWITCH in it as long as there are other paths available. In the SWITCH you can do whatever you need to get the database in the correct state for the main development branch.

Indentation is not mandatory, it is only used for clarity.

**Targeting specific versions:** Having more than one branch in the upgrade file implies that there must be a way to select which version you want to upgrade your database to. This is done with the 'target' parameter which you can specify on the commandline, in a properties file, Maven pom.xml or Ant build.xml. Examples: [http:source/browse/trunk/dist/build.xml build.xml], [http:source/browse/trunk/dist/pom.xml pom.xml] and [http:source/browse/trunk/dist/solidbase.properties solidbase.properties]. If your database evolution has branches, your sourcecode probably has too. In the sourcecode for the release branch you can configure the target database version as '1.0.`*`', while in the main development branch you can configure the target database version as '1.1.`*`'.

**Maintain a single upgrade file:** It is advisable to keep the upgrade files in each (live) source code branch synchronized or to use some technique to make sure that the same upgrade file is being used by each source code branch. With Subversion you could probably use the 'externals' feature for this purpose.

After some time, the release branch and main development branch have further progressed:
```
--* DEFINITION
--*     SETUP "" --> "1.1"
--*     UPGRADE "" --> "1.0.1"
--*     UPGRADE "1.0.1" --> "1.0.2"
--*         UPGRADE "1.0.2" --> "1.0.3" // Start of maintenance branch for release 1.0
--*         UPGRADE "1.0.3" --> "1.0.4"
--*         SWITCH "1.0.4" --> "1.1.1" // Return to the main development branch
--*     UPGRADE "1.0.2" --> "1.1.1"
--*     UPGRADE "1.1.1" --> "1.1.2"
--* /DEFINITION
```

And even more progress:
```
--* DEFINITION
--*     SETUP "" --> "1.1"
--*     UPGRADE "" --> "1.0.1"
--*     UPGRADE "1.0.1" --> "1.0.2"
--*         UPGRADE "1.0.2" --> "1.0.3" // Start of maintenance branch for release 1.0
--*         UPGRADE "1.0.3" --> "1.0.4"
--*         UPGRADE "1.0.4" --> "1.0.5"
--*         SWITCH "1.0.5" --> "1.1.2" // Return to the main development branch
--*     UPGRADE "1.0.2" --> "1.1.1"
--*     UPGRADE "1.1.1" --> "1.1.2"
--*     UPGRADE "1.1.2" --> "1.1.3"
--*     UPGRADE "1.1.3" --> "1.1.4"
--* /DEFINITION
```
The SWITCH should always start from the last version of the release branch and will go to ANY version of the main development branch. Use whatever makes sense in any given situation, but remember, a specific version of the database should have a structure that is independent from the path that has been followed through the upgrade file. Exceptions to this rule are inevitable, but these should only be temporary and should be carefully managed.

I will add examples of dealing with this later. A hint: you can use the [IF HISTORY](#IF_HISTORY.md) or the [IGNORE SQL ERROR](#IGNORE_SQL_ERROR.md) annotations to deal with this situation in the main development branch.


---

## ASSERT EMPTY ##
Asserts that a given query does not return results.
### Since ###
1.0
### Syntax ###
```
ASSERT EMPTY MESSAGE "<error message>" <select statement>
```
### Example ###
```
--* // To support an action that does not work when data is present in the table.
--* // For example adding a NOT NULL column.
ASSERT EMPTY MESSAGE "The TEMP_DATA table should not contain any data during the upgrade"
SELECT *
FROM TEMP_DATA;
```


---

## ASSERT EXISTS ##
Asserts that a given query returns results.
### Since ###
1.0
### Syntax ###
```
ASSERT EXISTS MESSAGE "<error message>" <select statement>
```
### Example ###
```
--* // Switching from another change control tool to SolidBase:
--* // We need to check that the database has the correct version in the version tables of the other tool.
ASSERT EXISTS MESSAGE "The upgrade expects version 1.34 of the database"
SELECT *
FROM OLD_STYLE_VERSION_TABLE
WHERE CURRENT_VERSION = '1.34';
```


---

## IF HISTORY ##
(Upgrade only)

Conditional execution of commands depending on the historical upgrade path.
This is useful when the upgrade path contains branches.
In that case, the main upgrade path needs to sometimes be able to change its behaviour depending on
the branch path being followed or not.
The complete history is stored in the database.
### Since ###
1.5.0
### Syntax ###
```
--* IF HISTORY [NOT] CONTAINS <version>
--* /IF
```
### Example ###
```
--* // If the upgrade history includes version 1.1.5 then the column has already been added to the table.
--* IF HISTORY NOT CONTAINS "1.1.5"
ALTER TABLE ATABLE ADD ACOLUMN INTEGER;
--* /IF
```


---

## IGNORE SQL ERROR ##
Enables ignoring of specific database errors.

Ignores can be nested. Ignores are reset at the beginning of each upgrade block.
### Since ###
1.0
### Syntax ###
```
--* IGNORE SQL ERROR <SQLState> [, <SQLState>, ...]
--* /IGNORE SQL ERROR
```
### Example ###
```
--* IGNORE SQL ERROR S0001
DROP VIEW NONEXISTING_VIEW;
--* /IGNORE SQL ERROR
```


---

## IMPORT CSV ##
Fast import of inline or external CSV data. Uses JDBC batch updates for maximum insert rate.
With NOBATCH however, JDBC batch updates will be disabled, which is useful when an error exists in the CSV data
and you are interested in the line number where this error occurs.
### Since ###
1.6.0
### Syntax ###
#### Syntax 1 ####
CSV data is part of the import statement and will be read into memory first.
```
IMPORT CSV
[ SKIP HEADER ]
[ SEPARATED BY (TAB|<character>) ]
[ IGNORE WHITESPACE ]
[ NOBATCH ]
INTO <tablename> 
[ ( <columnlist> ) ]
[ VALUES ( <valuelist> ) ]
DATA
... csv data ... <current delimiter>
```
#### Syntax 2 ####
CSV data comes after the import statement and is read directly from the source file.
```
IMPORT CSV
[ SKIP HEADER ]
[ SEPARATED BY (TAB|<character>) ]
[ IGNORE WHITESPACE ]
[ NOBATCH ]
INTO <tablename> 
[ ( <columnlist> ) ]
[ VALUES ( <valuelist> ) ] <current delimiter>
... csv data ...
<empty line>
```
#### Syntax 3 ####
CSV data is in another file.
```
IMPORT CSV
[ SKIP HEADER ]
[ SEPARATED BY (TAB|<character>) ]
[ IGNORE WHITESPACE ]
[ NOBATCH ]
INTO <tablename> 
[ ( <columnlist> ) ]
[ VALUES ( <valuelist> ) ]
FILE "<filename>" ENCODING "<encoding>" <current delimiter>
```
The supported encodings vary between different implementations of the Java 2 platform, but every implementation is required to support US-ASCII, ISO-8859-1, UTF-8, UTF-16BE, UTF-16LE and UTF-16. See http://docs.oracle.com/javase/6/docs/api/java/nio/charset/Charset.html.

### Examples ###
These are all syntax 1 examples, but they work with syntax 2 and 3 too.
```
--* // This import depends on the order of the columns in the table
IMPORT CSV INTO ATABLE DATA
1,2,3
4,5,6;

--* // This import explicitly specifies the columns, which is safer
IMPORT CSV INTO ATABLE ( COL1, COL2, COL3 ) DATA
1,2,3
4,5,6;

--* // Different separators
IMPORT CSV SEPARATED BY TAB INTO ATABLE DATA
1	2	3
4	5	6;
IMPORT CSV SEPARATED BY | INTO ATABLE DATA
1|2|3
4|5|6;

--* // Skipping the first line
IMPORT CSV SKIP HEADER SEPARATED BY TAB INTO ATABLE DATA
value1	value2	value3
1	2	3
4	5	6;

--* // You need double quotes when a comma or a newline is part of the value
--* // Double quotes in the value should be doubled
IMPORT CSV INTO ATABLE ( COL1, COL2, COL3 ) DATA
"here is a comma, which is ok",5,6
"newlines work
too, excellent",7,8
"""Life is good"", he said",5,6;

--* // Since 1.6.4, whitespace is part of the value, but you can ignore it with the IGNORE WHITESPACE option.
--* // However, whitespace between double quotes is always considered to be part of the value.
IMPORT CSV IGNORE WHITESPACE INTO ATABLE ( COL1, COL2, COL3 ) DATA
"  surrounding whitespace   ", 2 , 3 ;

--* // Values can be reordered, duplicated, ignored, transformed, and introduced
IMPORT CSV
INTO ATABLE ( ID, COL1, COL2, COL3 )
VALUES ( ASEQUENCE.NEXTVAL, TO_DATE( :2, 'YYYY-MM-DD' ), :1, :1 )
DATA
"here is a comma, which is ok",2003-01-01,6
"""Life is good"", he said",2004-01-01,6;
```


---

## JDBC ESCAPE PROCESSING ##
See [Getting Started with the JDBC API: SQL Escape Syntax in Statements](http://download.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/statement.html#1006519)

SolidBase disables JDBC escape processing by default. The purpose of JDBC escape processing is to make it easier to keep SQL database vendor independent. If you enable it however, you will not be able to send Java sources to the database because Java contains { and }.
### Since ###
1.6.5
### Syntax ###
```
--* JDBC ESCAPE PROCESSING ON | OFF;
```
### Example ###
```
--* JDBC ESCAPE PROCESSING ON;
INSERT INTO ATABLE ( ADATE, THEUSER ) VALUES ( {d '2011-07-25'}, {fn user()} );
```


---

## PRINT SELECT ##
Prints the result of the select statement.
### Since ###
1.6.0
### Syntax ###
```
PRINT SELECT ... normal select statement ...;
```
### Example ###
```
IMPORT CSV INTO ATABLE DATA
... csv data ...;
PRINT SELECT "Imported " || COUNT(*) || " records into ATABLE" FROM ATABLE;
```


---

## SECTION ##
Sets the next message to be displayed. The message will be displayed as soon as a command is executed.
### Since ###
1.6.0
### Syntax ###
```
--* SECTION "<message>"
```
### Example ###
```
--* SECTION "Creating indexes"
```
It is also possible to add an indentation level to the section.
### Syntax ###
```
--* SECTION.x "<message>"
```
### Example ###
```
--* SECTION.1 "Creating tables"
--* SECTION.2 "Audit tables"
```
### Output ###
```
Creating tables...
    Audit tables...
```


---

## SELECT CONNECTION ##
Selects another connection/database.

Each upgrade block starts with the default user and the default connection.
### Since ###
1.5.0
### Syntax ###
```
--* SELECT CONNECTION <connectionname>
```
### Example ###
```
--* SELECT CONNECTION QUEUES

--* // The primary connection is called 'DEFAULT'
--* SELECT CONNECTION DEFAULT
```
### See also ###
Configuring multiple connections (TODO)


---

## SET DELIMITER ##
Changes the current delimiter for commands. This a needed when stored procedures are part of the upgrade.
### Since ###
1.6.0
### Syntax ###
```
--* SET DELIMITER [ ISOLATED | TRAILING ] <delimiter1> [ OR [ ISOLATED |TRAILING ] <delimiter2> ]
```
### Examples ###
```
--* // Creating a stored procedure
--* SET DELIMITER ISOLATED GO
CREATE PROCEDURE APROCEDURE IS
BEGIN
	... a lot of semicolons are found here ...
END; -- In Oracle you need this semicolon after the END
GO
--* SET DELIMITER TRAILING ;

--* // Other examples
--* SET DELIMITER ;;
--* SET DELIMITER ISOLATED GO OR TRAILING ;;
```

To set the default delimiter, add the following to the upgrade file definition:

### Since ###
1.6.0
### Syntax ###
```
--* DELIMITER IS [ ISOLATED | TRAILING ] <delimiter1> [ OR [ ISOLATED |TRAILING ] <delimiter2> ]
```
### Example ###
```
--* DEFINITION
--*		DELIMITER IS ISOLATED GO
--*		...
--* /DEFINITION
```


---

## SET USER ##
Changes the current user.

Will ask for the password once.

Each upgrade block starts with the default user and the default connection.

Not supported when using the Ant task or Maven plugin, the secondary connections facility should be used instead when using Ant or Maven.
### Since ###
1.0
### Syntax ###
```
--* SET USER <username>
```
### Example ###
```
--* SET USER OTHERUSER
```


---

## SKIP ##
Skips commands.
### Since ###
1.6.2
### Syntax ###
```
--* SKIP
--* /SKIP
```
### Example ###
```
--* SKIP
-- In Oracle this is a command that you sometimes see in SQL files to 
-- prevent & from being recognized as a parameter to the SQL file.
-- It is a SQLPlus command, and should be skipped when executing this SQL file with SolidBase.
SET DEFINE OFF;
--* /SKIP
```


---

## TRANSIENT ##
(Upgrade only)

Marks commands as transient (not persisted).
During upgrade, persistent statements are counted (all commands that do not start with --`*`). This is needed to restart the upgrade after a failure.
After a restart, already executed persistent statements are skipped automatically.
Transient commands however, like the one in the example below, should not be counted and not be skipped after a restart.
### Since ###
1.6.1
### Syntax ###
```
--* TRANSIENT
--* /TRANSIENT
```
### Example ###
```
--* TRANSIENT
ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD';
--* /TRANSIENT
```