--* // ========================================================================

--*	PATCHES
--*		PATCH "" --> "1.0.1"
--*	/PATCHES







--* // ========================================================================
--* PATCH "" --> "1.0.1"
--* // ========================================================================

--* SET MESSAGE "Creating table DBVERSION"
CREATE TABLE DBVERSION
( 
	VERSION VARCHAR, 
	TARGET VARCHAR, 
	STATEMENTS INTEGER NOT NULL 
)
GO

UNTERMINATED COMMAND

--* /PATCH

--* // ========================================================================
