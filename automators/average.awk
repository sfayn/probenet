BEGIN {
	average=0;
	count=0;
}

#######################################################################
{
	if ($1="average:")
	{
		i=2;
		while ($i != "")
		{
			average+=$i
			i++;
			count++;
		}
	}
	
}

#######################################################################

END {
	printf( "1\t%d\n",average/count);
}

