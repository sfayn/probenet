BEGIN {
	FS = "[_-]";
	x_value=-1;
}

#######################################################################
{
	type=$1;
	printf("%s-%s----\n",B,S);
	i=2;
	for (i=2;$i != "";i=i+2)
	{
		printf("%s-%s\n",$i,$(i+1));
		if ($i="B" )
		{
			if (B!=-1)
			{
				if (B=$(i+1))
					continue;
				else
					break;
			}
			else
				x_value=$(i+1);
		}
		if ($i="S" )
		{
			if (S!=-1)
			{
				if (S=$(i+1))
					continue;
				else
					break;
			}
			else
				x_value=$(i+1);
		}
		if (type="FS")
		{
		}
	}
	if ($i == "" && x_value!=-1)
		printf( "x=%d ..\\reports\\%s\n",x_value,name+".energry");
	
}

#######################################################################

END {
	
}

