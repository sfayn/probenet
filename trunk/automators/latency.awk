BEGIN {
	index_cum=-1;
}
######################################################################
{
	
	if ($1=="cum_prob")
	{
		i=2;
		one=1;
		while ($i != "")
		{
			if ($i>min)
			{
				index_cum =i;
				one=0;
				break;
			}
			else if ($i==min)
			{
				index_cum =i;
				one=1;
				break;
			}
			i++;
		}
		if (index_cum==-1 && i>2)
		{
			index_cum=(i-1);
			one=0;
		}
	}
	else if ($1==metric)
	{
		if (index_cum!=-1)
		{
			if ( one==1)
				printf( "%g\t%g\n",x,$index_cum);
			else
				printf( "%g\t%g\n",x,($index_cum+$(index_cum-1))/2);
		}
		else
			printf("%g\t0\n",x);
	}
	
}


