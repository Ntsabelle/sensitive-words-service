#!/bin/bash
set -e

for i in $(seq 1 30); do
  /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" -Q "SELECT 1" >/dev/null 2>&1 && break
  sleep 2
done

/opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" -Q "IF DB_ID('${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}];"

/opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" -Q "
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = '${DB_USER}')
BEGIN
    CREATE LOGIN [${DB_USER}] WITH PASSWORD = '${DB_PASSWORD}', CHECK_POLICY = OFF;
END
"

/opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" -d "${DB_NAME}" -Q "
IF DATABASE_PRINCIPAL_ID('${DB_USER}') IS NULL
BEGIN
    CREATE USER [${DB_USER}] FOR LOGIN [${DB_USER}] WITH DEFAULT_SCHEMA = dbo;
END
ALTER ROLE db_owner ADD MEMBER [${DB_USER}];
"

echo "Database ${DB_NAME} and user ${DB_USER} are ready for Flyway."