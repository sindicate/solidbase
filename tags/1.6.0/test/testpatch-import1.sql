
--* // Copyright 2009 Ren� M. de Bloois

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

--* SECTION "Creating control tables"

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

--* SECTION.0 "Starting import"

IMPORT CSV INTO TEMP DATA
GO

IMPORT CSV INTO TEMP DATA
	"1", "2", "3"
GO

IMPORT CSV SEPARATED BY TAB INTO TEMP
DATA
"1"	"2"	"3"
"1"	"2"	"3"
"1"	"2"	"3"
GO

IMPORT CSV SEPARATED BY ; INTO TEMP DATA
"1"; "2"; "3"
"1"; "2"; "3"
"1"; "2"; "3"
GO

--* SECTION "Generating SQLException"

--* IGNORE SQL ERROR S0001

--* // A dot should still be printed when an error is ignored
CREATE TABLE TEMP ( TEMP1 VARCHAR, TEMP2 VARCHAR, TEMP3 VARCHAR )
GO

--* /IGNORE SQL ERROR

--* SECTION.1 "Importing with linenumber"

CREATE TABLE TEMP2 ( LINENUMBER INTEGER, TEMP1 VARCHAR, TEMP2 VARCHAR, TEMP3 VARCHAR )
GO

IMPORT CSV SEPARATED BY ; PREPEND LINENUMBER INTO TEMP2 DATA
"1"; "2"; "3"
"1"; "2"; "3"
"1"; "2"; "3"
GO

PRINT SELECT LINENUMBER FROM TEMP2
GO

--* SECTION.2 "Importing with column list"
--* SECTION.3 "And deeper"

CREATE TABLE TEMP3 ( TEMP1 INTEGER, TEMP2 VARCHAR, TEMP3 VARCHAR, TEMP4 VARCHAR )
GO

IMPORT CSV
SEPARATED BY |
INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )
VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-", TEST ''X' )
DATA
1|2
3|4
GO

PRINT SELECT TEMP1 || TEMP2 || TEMP3 FROM TEMP3
GO

--* /PATCH