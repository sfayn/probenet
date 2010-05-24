erase temp_files, %1_file,temp
REM for /f %%a IN ('dir ..\reports\%1_*.txt /B') do echo "%%~na" >>temp_files
dir ..\reports\%1_*.txt /B > temp
gawk -f remove_file_extension.awk temp > temp_files
gawk -f name_decoder_L.awk %VAR% temp_files >> %1_file
FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes,latency,av_latency) do erase ..\values\%1_%%m_1, ..\values\%1_%%m
FOR /F "delims=" %%f IN (%1_file) DO FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes) do gawk -f average.awk metric="%%m" %%f >> ..\values\%1_%%m_1
FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes) do gawk -f merge_equal_results.awk  ..\values\%1_%%m_1 > ..\values\%1_%%m
FOR %%m in (delivery_prob,overhead_ratio, average_energy,dead_nodes) do erase  ..\values\%1_%%m_1
erase temp_files,temp