F:
cd F:\Documents\aub\fyp\one_1.3.0\automators
IF EXIST FS_file erase FS_file 
IF EXIST FS_values erase FS_values 
for /f %%a IN ('dir ..\reports /B FS_*.energy') do call filter_files.bat %%a %%~na FS_file %*
FOR /F %i IN (FS_file) DO call average.bat %i FS_values
gnuplot -e "set term postscript eps enhanced; set output '../graphs/c.eps';plot 'FS_values' using 1:2 with lines"