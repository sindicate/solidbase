
--* // Copyright 2009 René M. de Bloois

--* // Licensed under the Apache License, Version 2.0 (the "License");
--* // you may not use this file except in compliance with the License.
--* // You may obtain a copy of the License at

--* //     http://www.apache.org/licenses/LICENSE-2.0

--* // Unless required by applicable law or agreed to in writing, software
--* // distributed under the License is distributed on an "AS IS" BASIS,
--* // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--* // See the License for the specific language governing permissions and
--* // limitations under the License.

--* // ========================================================================

--*	PATCHES
--*		PATCH "" --> "1.0.1"
--*		PATCH "1.0.1" --> "1.0.2"
--*	/PATCHES







--* PATCH "" --> "1.0.1"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
)
GO

CREATE TABLE DBVERSIONLOG
(
	ID INTEGER IDENTITY,
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT VARCHAR NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
)
GO

--* /PATCH







--* PATCH "1.0.1" --> "1.0.2"

CREATE TABLE TEMP ( TEMP1 VARCHAR, TEMP2 VARCHAR, TEMP3 VARCHAR )
GO

--* SET MESSAGE "Starting import"

IMPORT CSV INTO TEMP
GO

IMPORT CSV INTO TEMP
	"1", "2", "3"
GO

IMPORT CSV SEPARATED BY TAB INTO TEMP
"1"	"2"	"3"
"1"	"2"	"3"
"1"	"2"	"3"
GO

IMPORT CSV SEPARATED BY ; INTO TEMP
"1"; "2"; "3"
"1"; "2"; "3"
"1"; "2"; "3"
GO

--* SET MESSAGE "Generating SQLException"

--* IGNORE SQL ERROR S0001

--* // A dot should still be printed when an error is ignored
CREATE TABLE TEMP ( TEMP1 VARCHAR, TEMP2 VARCHAR, TEMP3 VARCHAR )
GO

--* /IGNORE SQL ERROR

--* SET MESSAGE "Importing with linenumber"

CREATE TABLE TEMP2 ( LINENUMBER INTEGER, TEMP1 VARCHAR, TEMP2 VARCHAR, TEMP3 VARCHAR )
GO

IMPORT CSV SEPARATED BY ; INTO TEMP2 PREPENDING LINENUMBER
"1"; "2"; "3"
"1"; "2"; "3"
"1"; "2"; "3"
GO

PRINT SELECT LINENUMBER FROM TEMP2
GO

--* /PATCH
