
--*	PATCHES

--*		INIT			""				-->	"INIT"			// De init patch creeert tabellen voor de patchtool zelf

--*		// hier beginnen de echte patches
--*		PATCH 		"INIT"	-->	"2.0.1"
--*		PATCH OPEN	"2.0.1"	-->	"2.0.2"
--*		PATCH		"2.0.2"	-->	"2.0.3"
--*		PATCH		"2.0.3"	-->	"2.0.4"
--*		BRANCH		"2.0.4"	-->	"2.0.5"
--*			PATCH	"2.0.5"	-->	"2.0.6"
--*			PATCH	"2.0.6"	-->	"2.0.7"
--*			PATCH	"2.0.7"	-->	"2.0.8"
--*			PATCH	"2.0.8"	-->	"2.0.9"
--*			PATCH	"2.0.9"	-->	"2.0.10"
--*			PATCH	"2.0.10"	-->	"2.0.11"
--*			PATCH	"2.0.11"	-->	"2.0.12"
--*		RETURN		"2.0.12"	-->	"3.0.1"
--*		PATCH		"2.0.4"	-->	"3.0.1"
--*		PATCH		"3.0.1"	-->	"3.0.2"
--*		PATCH		"3.0.2"	-->	"3.0.3"

--*	/PATCHES







--* INIT "" --> "INIT"

--* // Hier komt de check in de oude version_control
ASSERT EXISTS MESSAGE "Assertion failed"
SELECT * 
FROM SYS.SYSTABLES
WHERE TABLENAME = 'SYSTABLES'
--*

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







--* PATCH "2.0.1" --> "2.0.2"

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

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

--* /PATCH







--* PATCH "INIT" --> "2.0.1"

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

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

CREATE TABLE TEST
(
	TEST VARCHAR(10)
)
--*

DROP TABLE TEST
--*

--* /PATCH
