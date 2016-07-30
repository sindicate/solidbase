
--* // Copyright 2010 Ren� M. de Bloois

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



--* SET TERMINATOR=;


--* SECTION "Creating table USERS"

CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR(40) NOT NULL,
	USER_PASSWORD VARCHAR(40) NOT NULL
);


--* SECTION "Inserting admin user"
 
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );	


--* SECTION "Inserting 3 users"
 
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' ); INSERT INTO USERS ( USER_USERNAME, 
USER_PASSWORD ) VALUES ( '3', 'z' );	


--* SKIP 
This is a skip test;
--* /SKIP


--* SET TERMINATOR = SEPARATE ; 	 


--* SECTION "Inserting 3 users"
 
-- Appearantly, some database allow multiple statements in one go without the BEGIN END (as Oracle does).
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' );	
;


--* RESET TERMINATOR


--* SECTION "Inserting 3 users"

--* // These are now also sent in one go
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' );	


--* SET TERMINATOR = TRAILING ; OR SEPARATE GO


--* SECTION "Inserting 3 users"
 
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' );
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' );
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' )	
	  		GO  	

--* // To test the reset() between 2 SQL files
--* SET TERMINATOR = SEPARATE GO

COMMIT
GO
ROLLBACK
GO
