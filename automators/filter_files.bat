echo %1 > temp
echo %* > vars_all
gawk -f keep_vars.awk vars_all >vars
FOR /F "delims=" %%i IN (vars) DO gawk -f name_decoder.awk name=%1 %%i temp >>%2
erase vars, vars_all,temp