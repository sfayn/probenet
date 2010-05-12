F:
cd F:\Documents\aub\fyp\one_1.3.0\automators
echo %* > vars_all
gawk -f keep_vars.awk vars_all >vars
FOR /F "delims=" %%i IN (vars) do set VAR= %%i
FOR %%r in (AFSnW) do call per_router_2var.bat %%r
erase min,temp
FOR %%r in (AFSnW) do FOR /F "delims=" %%f IN (%%r_file) do gawk -f max_cum_prob.awk %%f >> temp
gawk -f get_min.awk temp >min
FOR /F "delims=" %%m IN (min) do set MIN_VALUE=%%m
FOR %%r in (AFSnW) do FOR /F "delims=" %%f IN (%%r_file) DO FOR %%m in (latency,av_latency) do gawk -f latency_2var.awk metric="%%m" min=%MIN_VALUE% %%f >> ..\values\%%r_%%m
REM FOR %%r in (AFSnW) do FOR %%m in (latency,av_latency) do gawk -f sort_2var.awk  ..\values\%%r_%%m_1 > ..\values\%%r_%%m
FOR %%r in (AFSnW) do FOR %%m in (latency,av_latency) do erase  ..\values\%%r_%%m_1, %%r_file
erase min,temp

gawk -f x-axis_interpreter.awk %VAR% vars  >x_axis
FOR /F "delims=" %%i IN (x_axis) DO set X_label= %%i
gawk -f y-axis_interpreter.awk %VAR% vars  >y_axis
FOR /F "delims=" %%i IN (y_axis) DO set Y_label= %%i
FOR %%z in (delivery_prob,overhead_ratio, average_energy,dead_nodes,latency,av_latency) do gnuplot -e "set term png size 1280, 800;set output '../graphs/%1_3D_%%z.png'  ;set zlabel '%%z';set ylabel '%Y_label%';set xlabel '%X_label%';set dgrid3d 30,30;set hidden3d;splot '../values/AFSnW_%%z' u 1:2:3 title 'AFSnW' with lines";
erase vars, vars_all, x_axis,y_axis
REM set logscale x;
REM , '../values/AFS_%%z' u 1:2:3 title 'AFS' with lines, '../values/SW_%%z' u 1:2:3 title 'SW' with lines