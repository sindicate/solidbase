--*	PATCHES
--*		INIT source="" target="INIT" description="INIT"
--*		PATCH OPEN source="INIT" target="DHL TTS 2.0.1" description="TTS 2.0.1 2006-03-27"
--*		PATCH source="DHL TTS 2.0.1" target="DHL TTS 2.0.2" description="TTS 2.0.2 2006-03-27"
--*		PATCH source="DHL TTS 2.0.2" target="DHL TTS 2.0.3" description="TTS 2.0.3 2006-03-27"
--*		PATCH source="DHL TTS 2.0.3" target="DHL TTS 2.0.4" description="TTS 2.0.4 2006-03-27"
--*			BRANCH source="DHL TTS 2.0.4" target="DHL TTS 2.0.5" description="TTS 2.0.5 2006-03-27"
--*				PATCH source="DHL TTS 2.0.5" target="DHL TTS 2.0.6" description="TTS 2.0.6 2006-03-27"
--*				PATCH source="DHL TTS 2.0.6" target="DHL TTS 2.0.7" description="TTS 2.0.7 2006-03-27"
--*				PATCH source="DHL TTS 2.0.7" target="DHL TTS 2.0.8" description="TTS 2.0.8 2006-03-27"
--*				PATCH source="DHL TTS 2.0.8" target="DHL TTS 2.0.9" description="TTS 2.0.9 2006-03-27"
--*				PATCH source="DHL TTS 2.0.9" target="DHL TTS 2.0.10" description="TTS 2.0.10 2006-03-27"
--*				PATCH source="DHL TTS 2.0.10" target="DHL TTS 2.0.11" description="TTS 2.0.11 2006-03-27"
--*				PATCH source="DHL TTS 2.0.11" target="DHL TTS 2.0.12" description="TTS 2.0.12 2006-03-27"
--*			RETURN source="DHL TTS 2.0.12" target="DHL TTS 3.0.1" description="TTS 3.0.1 2006-03-27"
--*		PATCH source="DHL TTS 2.0.4" target="DHL TTS 3.0.1" description="TTS 3.0.1 2006-03-27"
--*		PATCH source="DHL TTS 3.0.1" target="DHL TTS 3.0.2" description="TTS 3.0.1 2006-03-27"
--*		PATCH source="DHL TTS 3.0.2" target="DHL TTS 3.0.3" description="TTS 3.0.1 2006-03-27"
--*	/PATCHES







--* INIT source="" target="INIT"

CREATE TABLE DBVERSION
( 
	VERSION VARCHAR(20) NOT NULL, 
	TARGET VARCHAR(20), 
	STATEMENTS INTEGER NOT NULL 
)
--*

CREATE TABLE DBVERSIONLOG
( 
	VERSION VARCHAR(20) NOT NULL, 
	TARGET VARCHAR(20) NOT NULL, 
	STATEMENT INTEGER NOT NULL, 
	TIMESTAMP TIMESTAMP NOT NULL, 
	SQLSOURCE LONG VARCHAR NOT NULL, 
	RESULT LONG VARCHAR
)
--*

INSERT INTO DBVERSION ( VERSION, TARGET, STATEMENTS )
VALUES ( 'INIT', NULL, 3  )
--*

--* /INIT







--* PATCH source="INIT" target="DHL TTS 2.0.1"

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

--* // Dit is patch tool commentaar

-- Dit is sql commentaar

DROP TABLE TEST
--*

--*

--*

--* /PATCH
