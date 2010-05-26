let z=1
for i in VL L M H VH
do 
	for j in VL L M H VH
	do
		for k in low medium high
		do
			echo 1
			echo RULE $z : IF timeDiff IS $k AND newTH IS $i AND oldTH is $j >>generated.fcl
            echo    THEN estimatedTH IS $i; >>generated.fcl
				let z=$z+1
		done
	done
done