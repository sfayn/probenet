F:
cd F:\Documents\aub\fyp\one_1.3.0\reports
for /f %%a IN ('dir *.txt /B') do rename "%%~na.txt" "%%~na_1.txt"