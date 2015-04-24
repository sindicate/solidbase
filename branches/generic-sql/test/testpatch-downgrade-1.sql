
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

--*	DEFINITION
--*		INIT "" --> "1.1"
--*		UPGRADE "" --> "1.0.1"
--*		UPGRADE "1.0.1" --> "1.0.2"
--*			UPGRADE "1.0.2" --> "1.0.3"
--*			SWITCH "1.0.3" --> "1.1.0"
--*		UPGRADE "1.0.2" --> "1.1.0"
--*		UPGRADE "1.1.0" --> "1.1.1"
--*		DOWNGRADE "1.1.1" --> "1.0.2"
--*	/DEFINITION

--* INIT "" --> "1.1"
CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL,
	SPEC VARCHAR(5) NOT NULL
);
CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR(1) NOT NULL,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);
CREATE INDEX DBVERSIONLOG_INDEX1 ON DBVERSIONLOG ( TYPE, TARGET );
--* /INIT

--* UPGRADE "" --> "1.0.1"
CREATE TABLE TEST1 ( TEST VARCHAR(20) );
--* /UPGRADE

--* UPGRADE "1.0.1" --> "1.0.2"
CREATE TABLE TEST2 ( TEST VARCHAR(20) );
--* /UPGRADE

--* UPGRADE "1.0.2" --> "1.0.3"
CREATE TABLE TEST3 ( TEST VARCHAR(20) );
--* /UPGRADE

--* SWITCH "1.0.3" --> "1.1.0"
--* /SWITCH

--* UPGRADE "1.0.2" --> "1.1.0"
CREATE TABLE TEST3 ( TEST VARCHAR(20) );
--* /UPGRADE

--* UPGRADE "1.1.0" --> "1.1.1"
CREATE TABLE TEST4 ( TEST VARCHAR(20) );
--* /UPGRADE

--* DOWNGRADE "1.1.1" --> "1.0.2"
DROP TABLE TEST4;
DROP TABLE TEST3;
--* /DOWNGRADE
