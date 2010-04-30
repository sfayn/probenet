echo %1 > temp
FOR /F "delims=" %%i IN (vars) DO gawk -f name_decoder.awk name=%1 %%i temp >>%2