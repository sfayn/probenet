erase temp_files, %1_file,temp,min
for /f %%a IN ('dir ..\reports\%1_*.txt /B') do echo "%%~na" >>temp_files
gawk -f name_decoder.awk %VAR% temp_files >> %1_file
FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes,latency,av_latency) do erase ..\values\%1_%%m
FOR /F "delims=" %%f IN (%1_file) DO FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes) do gawk -f average.awk metric="%%m" %%f >> ..\values\%1_%%m
FOR /F "delims=" %%f IN (%1_file) do gawk -f max_cum_prob.awk %%f >> temp
gawk -f get_min.awk temp >min
FOR /F "delims=" %%m IN (min) do set MIN_VALUE=%%m
FOR /F "delims=" %%f IN (%1_file) DO FOR %%m in (latency,av_latency) do gawk -f latency.awk metric="%%m" min=%MIN_VALUE% %%f >> ..\values\%1_%%m
erase temp_files, %1_file,temp,min
