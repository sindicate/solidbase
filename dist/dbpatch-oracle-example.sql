
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
--*		PATCH "" --> "1.0.1"
--*		PATCH "1.0.1" --> "1.0.2"
--*	/PATCHES







--* // ========================================================================
--* PATCH "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE 'Creating table DBVERSION'

--* // Create version control table
CREATE TABLE DBVERSION
(
	VERSION VARCHAR2(20),
	TARGET VARCHAR2(20),
	STATEMENTS INTEGER NOT NULL
)
GO

--* // The patch tool expects to be able to use the DBVERSION table after the *first* sql statement

--* SET MESSAGE 'Creating table DBVERSIONLOG'

--* // Create version control log table sequence generator
CREATE SEQUENCE DBVERSIONLOG_SEQUENCE
GO

--* // Create version control log table
CREATE TABLE DBVERSIONLOG
(
	ID INTEGER NOT NULL,
	SOURCE VARCHAR2(20),
	TARGET VARCHAR2(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP DATE NOT NULL,
	COMMAND VARCHAR2(4000),
	RESULT VARCHAR2(4000)
)
GO

--* // Create version control log table autonumber trigger
CREATE OR REPLACE TRIGGER DBVERSIONLOG_AUTONUMBER BEFORE INSERT ON DBVERSIONLOG
REFERENCING NEW AS NEWROW
FOR EACH ROW
BEGIN
	IF :NEWROW.ID IS NULL THEN
		SELECT DBVERSIONLOG_SEQUENCE.NEXTVAL INTO :NEWROW.ID FROM DUAL;
	END IF;
END;
GO

--* // The existence of DBVERSIONLOG will automatically be detected at the end of this patch

--* /PATCH







--* // ========================================================================
--* PATCH "1.0.1" --> "1.0.2"
--* // ========================================================================

--* SET MESSAGE 'Creating EP datamodel'

CREATE TABLE EP_ORGANISATION(
		ORGANISATIONID                 		VARCHAR2(32) NOT NULL,
		NAME                           		VARCHAR2(80) NOT NULL,
		COORDINATOR                    		VARCHAR2(32) NOT NULL,
		ADDRESS                        		VARCHAR2(80),
		TELEPHONE                      		VARCHAR2(20),
		FAX                            		VARCHAR2(20),
		ENDDATE                        		TIMESTAMP,
		CONSTRAINT EP_ORGANISATION_PK PRIMARY KEY (ORGANISATIONID))
GO

--* /PATCH

--* // ========================================================================
