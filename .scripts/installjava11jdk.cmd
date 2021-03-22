@echo off

set JDK_VERSION=11
set JDK_NAME=zulu-11-azure-jdk_11.45.27-11.0.10-win_x64
set JDK_ZIP_URL=https://repos.azul.com/azure-only/zulu/packages/zulu-11/11.0.10/zulu-11-azure-jdk_11.45.27-11.0.10-win_x64.zip

echo "Installing Java %JDK_VERSION% (%JDK_NAME%)..."
powershell -NoProfile -ExecutionPolicy Unrestricted -File "%~dp0install-java.ps1" -Url "%JDK_ZIP_URL%" -Destination "C:\Program Files\Java" -EnvVar "JAVA_HOME" -UpdatePath -Checksum "0bc2ba3066a23e217f0c9a590c3d8a7e17244e849034866606571395877ee7fc" -CleanOnFinish || exit /B 1

echo "C:\Program Files\Java\%JDK_NAME%"
dir "C:\Program Files\Java\%JDK_NAME%"

echo "JAVA_HOME=%JAVA_HOME%"

echo "Installed Java %JDK_VERSION% Successfully."
