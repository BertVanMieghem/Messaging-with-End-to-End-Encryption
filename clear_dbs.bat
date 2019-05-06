@echo off
copy NUL SccServer\SccServer.sqlite /Y

for /r %%i in (SccClient\db\*) do echo %%i
	copy NUL %%i /Y