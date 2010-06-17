
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
--*		INIT "1.0" --> "1.1"
--*		PATCH "1.0.2" --> "1.0.3"
--*	/PATCHES







--* INIT "1.0" --> "1.1"

ALTER TABLE DBVERSION ADD SPEC VARCHAR;
ALTER TABLE DBVERSIONLOG ADD TYPE VARCHAR;

UPGRADE;

ALTER TABLE DBVERSION ALTER COLUMN SPEC SET NOT NULL;
ALTER TABLE DBVERSIONLOG ALTER COLUMN TYPE SET NOT NULL;

--* /INIT







--* PATCH "1.0.2" --> "1.0.3"

--* IF HISTORY CONTAINS "1.0.3"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
);

--* IF HISTORY CONTAINS "1.0.1"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
);

--* /IF

--* /IF

--* IF HISTORY NOT CONTAINS "1.0.1"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
);

--* /IF

--* IF HISTORY CONTAINS "1.0.1"

CREATE TABLE TEST ( TEST VARCHAR );

--* /IF

--* /PATCH
