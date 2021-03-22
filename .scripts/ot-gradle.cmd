@echo off
setlocal

pushd %~dp0
set SCRIPT_ROOT=%CD%
popd

pushd %~dp0..
set PROJECT_ROOT=%CD%\otel

pushd %PROJECT_ROOT%

set DEFAULT_OPTIONS=--info --stacktrace --warning-mode=all
set GRADLE_CMD=gradlew.bat %DEFAULT_OPTIONS% %*
echo Running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
call %GRADLE_CMD%
if errorlevel 1 (
    echo Error running '%GRADLE_CMD%' in '%PROJECT_ROOT%'
    exit /b 1
)
popd
popd
endlocal