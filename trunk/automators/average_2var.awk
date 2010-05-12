BEGIN {
	average=0;
	count=0;
}

#######################################################################
{
	if ($1==metric)
	{
		i=2;
		while ($i != "")
		{
			##printf("%d\n",$i);
			average+=$i
			i++;
			count++;
		}
	}
	
}

#######################################################################

END {
	if (count>0)
		printf( "%g\t%g\t%g\n",x,y,average/count);
	else
		printf("%g\t%g\t0\n",x,y);
}

