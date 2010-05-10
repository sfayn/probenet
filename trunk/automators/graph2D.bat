F:
cd F:\Documents\aub\fyp\one_1.3.0\automators
echo %* > vars_all
gawk -f keep_vars.awk vars_all >vars
FOR /F "delims=" %%i IN (vars) do set VAR= %%i
FOR %%r in (SW,AFS, AFSnW) do call per_router.bat %%r
erase min,temp
FOR %%r in (SW,AFS, AFSnW) do FOR /F "delims=" %%f IN (%%r_file) do gawk -f max_cum_prob.awk %%f >> temp
gawk -f get_min.awk temp >min
FOR /F "delims=" %%m IN (min) do set MIN_VALUE=%%m
FOR %%r in (SW,AFS, AFSnW) do FOR /F "delims=" %%f IN (%%r_file) DO FOR %%m in (latency,av_latency) do gawk -f latency.awk metric="%%m" min=%MIN_VALUE% %%f >> ..\values\%%r_%%m_1
FOR %%r in (SW,AFS, AFSnW) do FOR %%m in (latency,av_latency) do gawk -f sort.awk  ..\values\%%r_%%m_1 > ..\values\%%r_%%m
FOR %%r in (SW,AFS, AFSnW) do FOR %%m in (latency,av_latency) do erase  ..\values\%%r_%%m_1, %%r_file
erase min,temp

gawk -f x-axis_interpreter.awk %VAR% vars  >x_axis
FOR /F "delims=" %%i IN (x_axis) DO set X_label= %%i
FOR %%y in (delivery_prob,overhead_ratio, average_energy,dead_nodes,latency,av_latency) do gnuplot -e "set term png size 1280, 800;set output '../graphs/%1_%%y.png'  ; set logscale x;set ylabel '%%y';set xlabel '%X_label%';plot '../values/AFSnW_%%y' using 1:2 title 'AFSnW' with lines, '../values/AFS_%%y' using 1:2 title 'AFS' with lines, '../values/SW_%%y' using 1:2 title 'SW' with lines";
rem set term postscript eps enhanced; set output '../graphs/%1_%%y.eps';set xlabel '%X_label%'; set ylabel '%%y';plot '../values/AFSnW_%%y' using 1:2 with lines";

rem plot '../values/SW_%%y' using 1:2 with lines;
rem plot '../values/AFSnW_%%y' using 1:2 with lines"
REM ;plot 'FSnWnA_values' using 1:2 with lines
 erase vars, vars_all, x_axis
 
 