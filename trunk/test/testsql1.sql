
--* // Copyright 2010 René M. de Bloois

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



--* DELIMITER IS ;


--* SET MESSAGE "    Creating table USERS"

CREATE TABLE USERS
(
	USER_ID INT IDENTITY,
	USER_USERNAME VARCHAR NOT NULL,
	USER_PASSWORD VARCHAR NOT NULL
);


--* SET MESSAGE "    Inserting admin user"
 
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( 'admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=' );	


--* SET MESSAGE "    Inserting 3 users"
 
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' ); INSERT INTO USERS ( USER_USERNAME, 
USER_PASSWORD ) VALUES ( '3', 'z' );	


--* SET DELIMITER ISOLATED ; 	 


--* SET MESSAGE "    Inserting 3 users"
 
-- Appearantly, some database allow multiple statements in one go without the BEGIN END (as Oracle does).
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' );
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' );	
;


--* SET DELIMITER TRAILING ;


--* SET MESSAGE "    Inserting 3 users"

--* // These are now also sent in one go
INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' ); INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' );	


--* SET DELIMITER TRAILING ; OR ISOLATED GO


--* SET MESSAGE "    Inserting 3 users"
 
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '1', 'x' );
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '2', 'y' );
			INSERT INTO USERS ( USER_USERNAME, USER_PASSWORD ) VALUES ( '3', 'z' )	
	  		GO  	

