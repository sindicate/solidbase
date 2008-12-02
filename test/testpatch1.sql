--* // ========================================================================

--*	PATCHES
--*		PATCH "" --> "1.0.1"
--*		PATCH "1.0.1" --> "1.0.2"
--*	/PATCHES

--* // ========================================================================
--* PATCH "" --> "1.0.1"
--* // ========================================================================

--* MESSAGE START 'Creating table DBVERSION'
CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20), 
	TARGET VARCHAR(20), 
	STATEMENTS DECIMAL(4) NOT NULL 
)
GO

--* // The patch tool expects to be able to use the DBVERSION table after the *first* sql statement

--* MESSAGE START 'Creating table DBVERSIONLOG'
CREATE TABLE DBVERSIONLOG
(
	ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- An index might be needed here to let the identity perform
	SOURCE VARCHAR(20),
	TARGET VARCHAR(20) NOT NULL,
	STATEMENT DECIMAL(4) NOT NULL,
	STAMP TIMESTAMP NOT NULL,
	COMMAND VARCHAR(4000),
	RESULT VARCHAR(4000)
)
GO

--* // The existence of DBVERSIONLOG will automatically be detected at the end of this patch

--* /PATCH

--* // ========================================================================
--* PATCH "1.0.1" --> "1.0.2"
--* // ========================================================================

--* MESSAGE START 'Creating table USERS'
CREATE TABLE USERS
(
	USER_ID INT NOT NULL GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
	USER_USERNAME VARCHAR(26) NOT NULL,
	USER_PASSWORD VARCHAR(30) NOT NULL
)
GO

--* MESSAGE START 'Inserting admin user'
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' )
GO

--* /PATCH

--* // ========================================================================
