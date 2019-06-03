echo "Clear DBs"

::sqlite3 SccClient\db\SccClient_Bert.sqlite < delete.sql
sqlite3 SccClient\db\SccClient_Debug.sqlite < delete.sql
sqlite3 SccClient\db\SccClient_Sonneveld.sqlite < delete.sql

sqlite3 SccServer\SccServer.sqlite < delete.sql
