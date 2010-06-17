
--* // Copyright 2006 René M. de Bloois

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
--*		INIT "" --> "1.1"
--*		INIT "1.0" --> "1.1"
--*		INIT "1.1" --> "1.1.1"
--*		PATCH "" --> "1.0.1"
--*		PATCH "1.0.1" --> "1.0.2"
--*	/PATCHES







--* INIT "" --> "1.1"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL,
	SPEC VARCHAR NOT NULL
);

CREATE TABLE DBVERSIONLOG
(
	TYPE VARCHAR NOT NULL,
	SOURCE VARCHAR,
	TARGET VARCHAR NOT NULL,
	STATEMENT VARCHAR NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR,
	RESULT VARCHAR
);

--* /INIT







--* INIT "1.0" --> "1.1"

ALTER TABLE DBVERSION ADD SPEC VARCHAR;
ALTER TABLE DBVERSIONLOG ADD TYPE VARCHAR;

UPGRADE;

ALTER TABLE DBVERSION ALTER COLUMN SPEC SET NOT NULL;
ALTER TABLE DBVERSIONLOG ALTER COLUMN TYPE SET NOT NULL;

--* /INIT







--* INIT "1.1" --> "1.1.1"

CREATE INDEX DBVERSIONLOG_TYPE_INDEX ON DBVERSIONLOG ( TYPE, TARGET );

--* /INIT







--* PATCH "" --> "1.0.1"

CREATE TABLE TEST1 ( TEST VARCHAR );

--* /PATCH







--* PATCH "1.0.1" --> "1.0.2"

CREATE TABLE TEST2 ( TEST VARCHAR );

--* /PATCH







