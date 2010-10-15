
--* // Copyright 2010 René M. de Bloois

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

--*	DEFINITION
--*		UPGRADE "" --> "1"
--*		UPGRADE "1" --> "2"
--*		UPGRADE "2" --> "3"
--*		UPGRADE "2" --> "4"
--*	/DEFINITION

--* UPGRADE "" --> "1"
--* SECTION "Creating control tables"
CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL 
);
CREATE TABLE DBVERSIONLOG
(
	ID INTEGER IDENTITY,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);
--* /UPGRADE

--* UPGRADE "1" --> "2"
CREATE TABLE TEMP ( TEMP1 VARCHAR(40), TEMP2 VARCHAR(40), TEMP3 VARCHAR(40) );
--* /UPGRADE

--* UPGRADE "2" --> "3"
--* // Missing " in the data
IMPORT CSV INTO TEMP DATA
1", "2", "3"
"1", "2", "3"
"1", "2", "3";
--* /UPGRADE

--* UPGRADE "2" --> "4"
--* // Missing " in the data
IMPORT CSV INTO TEMP DATA
"1", "2", "3"
1", "2", "3"
"1", "2", "3";
--* /UPGRADE
