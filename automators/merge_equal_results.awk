# sort numbers in ascending order
function sort(ARRAY,ARRAY2, ELEMENTS,   temp, i, j) {
	for (i = 2; i <= ELEMENTS; ++i) {
				for (j = i; ARRAY[j-1] > ARRAY[j]; --j) {
	         temp = ARRAY[j]
	         ARRAY[j] = ARRAY[j-1]
	         ARRAY[j-1] = temp
						temp = ARRAY2[j]
						ARRAY2[j] = ARRAY2[j-1]
	         ARRAY2[j-1] = temp
	 }
	}
	return
}
BEGIN{
	i=1
}
{
	x[i]=$1+0.0
	y[i]=$2+0.0
	#printf("%s\t%s\n",x[i],y[i]);
	i++
}
END {
	array_count=i
	sort(x,y,array_count-1)   
	#printf("----------\n");
	
	count=1
	total=y[1]
	for (i = 2; i <= array_count-1; i++) {
		if (x[i]==x[i-1])
		{
			count++;
			total+=y[i]
		}
		else
		{
			printf("%g\t%g\n",x[i-1],total/count);
			count=1;
			total=y[i];
		}
	}
	printf("%g\t%g\n",x[i-1],total/count);

}