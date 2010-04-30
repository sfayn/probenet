F:
cd F:\Documents\aub\fyp\one_1.3.0\automators
erase FSnWnA_values, FSnWnA_file, SW_values, SW_file
echo %* > vars_all
gawk -f keep_vars.awk vars_all >vars
for /f %%a IN ('dir ..\reports\FSnWnA_*.txt /B') do call filter_files.bat "%%~na" FSnWnA_file %*
FOR /F "delims=" %%i IN (FSnWnA_file) DO gawk -f average.awk metric= %2 %%i >> FSnWnA_values

for /f %%a IN ('dir ..\reports\SW_*.txt /B') do call filter_files.bat "%%~na" SW_file %*
FOR /F "delims=" %%i IN (SW_file) DO gawk -f average.awk metric='%2' %%i >> SW_values

FOR /F "delims=" %%i IN (vars) DO gawk -f x-axis_interpreter.awk vars %%i >x_axis
FOR /F "delims=" %%i IN (x_axis) DO  gnuplot -e "set term postscript eps enhanced; set output '../graphs/%1.eps';set xlabel '%%i'; set ylabel '%2';plot 'SW_values' using 1:2 with lines"
REM ;plot 'FSnWnA_values' using 1:2 with lines
erase vars, vars_all,temp, x_axis
REM erase FSnWnA_values, FSnWnA_file, SW_values, SW_file