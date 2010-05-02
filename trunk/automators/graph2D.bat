F:
cd F:\Documents\aub\fyp\one_1.3.0\automators

echo %* > vars_all
gawk -f keep_vars.awk vars_all >vars
FOR /F "delims=" %%i IN (vars) do set VAR= %%i

FOR %%r in (SW,AFS, AFSnW) do call per_router.bat %%r

gawk -f x-axis_interpreter.awk %VAR% vars  >x_axis
FOR /F "delims=" %%i IN (x_axis) DO  gnuplot -e "set term postscript eps enhanced; set output '../graphs/%1.eps';set xlabel '%%i'; set ylabel '%2';plot '../values/SW_latency' using 1:2 with lines"
REM ;plot 'FSnWnA_values' using 1:2 with lines
erase vars, vars_all, x_axis