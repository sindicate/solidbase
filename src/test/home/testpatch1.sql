
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

--*	DEFINITION
--*		UPGRADE "" --> "1.0.1"
--*		UPGRADE "1.0.1" --> "1.0.2"
--*	END DEFINITION







--* // ========================================================================
--* UPGRADE "" --> "1.0.1"
--* // ========================================================================

--* SECTION "Creating table DBVERSION"
CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL 
);

--* // The patch tool expects to be able to use the DBVERSION table after the *first* sql statement

--* SECTION "Creating table DBVERSIONLOG"
CREATE TABLE DBVERSIONLOG
(
	ID INTEGER IDENTITY, -- An index might be needed here to let the identity perform
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
);

--* // The existence of DBVERSIONLOG will automatically be detected at the end of this patch

--* END UPGRADE







--* // ========================================================================
--* UPGRADE "1.0.1" --> "1.0.2"
--* // ========================================================================

--* // We need at least one sql without a message. This is a test too.
CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR(40) NOT NULL,
	USER_PASSWORD VARCHAR(40) NOT NULL
);	

--* SECTION "Inserting admin user" 
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );

--* END UPGRADE

--* // ========================================================================

