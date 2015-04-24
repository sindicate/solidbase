--* // ========================================================================

--*	PATCHES
--*		PATCH "" --> "1.0.1"
--*		PATCH "1.0.1" --> "1.0.2"
--*	/PATCHES







--* // ========================================================================
--* PATCH "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE "Creating table DBVERSION"

--* // Create version control table
CREATE TABLE DBVERSION
(
	VERSION VARCHAR(20),
	TARGET VARCHAR(20),
	STATEMENTS INTEGER NOT NULL
)
GO

--* // The patch tool expects to be able to use the DBVERSION table after the *first* sql statement

--* SET MESSAGE "Creating table DBVERSIONLOG"

--* // Create version control log table
CREATE TABLE DBVERSIONLOG
(
	ID INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT INTEGER NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
)
GO

--* // The existence of DBVERSIONLOG will automatically be detected at the end of this patch

--* /PATCH

--* // ========================================================================







--* // ========================================================================
--* PATCH "1.0.1" --> "2.0.2"
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
