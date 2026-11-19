@echo off
chcp 65001 >nul
echo =========================================================================
echo   SCRIPT TU DONG CHAY THI NGHIEM MERGESORT CUTOFF (DSA PROJECT)
echo =========================================================================

set "JAVAC=javac"
set "JAVA=java"
if exist "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe" (
    set "JAVAC=C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
    set "JAVA=C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
)

echo.
echo [1/4] Bien dich ma nguon Java trong thu muc src/...
if not exist bin mkdir bin
"%JAVAC%" -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo [LOI] Bien dich that bai!
    exit /b 1
)
echo       -> Bien dich thanh cong!

echo.
echo [2/4] Sinh bo du lieu thi nghiem (datasets/)...
"%JAVA%" -cp bin DatasetGenerator

echo.
echo [3/4] Chay thi nghiem do thoi gian va thong ke Cost Model...
"%JAVA%" -da -cp bin CutoffExperiment

echo.
echo [4/4] Ve do thi thong ke ket qua thi nghiem (results/*.png)...
"%JAVA%" -cp bin ChartGenerator

echo.
echo =========================================================================
echo   HOAN TAT! Kiem tra ket qua va bieu do trong thu muc results\
echo =========================================================================
