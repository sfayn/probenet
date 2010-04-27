echo %2 > temp
gawk -F'_-' -f name_decoder.awk %4 %5 %6 %7 %8 %9 temp >>%3
REM erase temp