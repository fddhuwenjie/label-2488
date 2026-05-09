@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.1
@REM
@REM Optional ENV vars:
@REM   JAVA_HOME - location of a JDK home dir, required when JAVA is not on PATH
@REM   MVNW_REPOURL - repo to use for downloading wrapper jar (default: https://repo.maven.apache.org/maven2)
@REM   MVNW_USERNAME / MVNW_PASSWORD - credentials for downloading wrapper
@REM   MVNW_VERBOSE - true: output additional info on wrapper execution
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_BASEDIR%"=="" GOTO endDetectBaseDir
@SET EXEC_DIR=%CD%
@SET WDIR=%EXEC_DIR%
:findBaseDir
@IF EXIST "%WDIR%\.mvn" GOTO baseDirFound
@CD ..
@IF "%WDIR%"=="%CD%" GOTO baseDirNotFound
@SET WDIR=%CD%
@GOTO findBaseDir
:baseDirFound
@SET MAVEN_PROJECTBASEDIR=%WDIR%
@CD "%EXEC_DIR%"
@GOTO endDetectBaseDir
:baseDirNotFound
@SET MAVEN_PROJECTBASEDIR=%EXEC_DIR%
:endDetectBaseDir
@IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
    @SET MVNW_VERBOSE=true
)

@SET JAVA_EXE=%JAVA_HOME%/bin/java.exe
@IF NOT EXIST "%JAVA_EXE%" (
    SET "JAVA_EXE=java.exe"
)

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.1/maven-wrapper-3.3.1.jar"

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="wrapperUrl" SET DOWNLOAD_URL=%%B
)

@IF EXIST %WRAPPER_JAR% (
    @IF "%MVNW_VERBOSE%"=="true" (
        @echo Found %WRAPPER_JAR%
    )
) ELSE (
    @IF NOT "%MVNW_REPOURL%"=="" (
        @SET DOWNLOAD_URL="%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.1/maven-wrapper-3.3.1.jar"
    )
    @IF "%MVNW_VERBOSE%"=="true" (
        @echo Downloading from: %DOWNLOAD_URL%
    )
    @powershell -Command "&{"^
        "$webclient = new-object System.Net.WebClient;"^
        "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
        "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
        "}"^
        "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"^
    "}"
    @IF "%ERRORLEVEL%"=="0" ( GOTO :downloadSuccess )
    @DEL "%WRAPPER_JAR%"
    @echo ERROR: Could not download %WRAPPER_JAR% >&2
    @echo Please check your internet connection and try again. >&2
    @EXIT /B 1
    :downloadSuccess
)

@IF EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" (
    @SET /P JVM_CONFIG_MAVEN_PROPS=<"%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config"
)

@"%JAVA_EXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %*
