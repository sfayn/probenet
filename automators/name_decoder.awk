BEGIN {
	FS = "[,_-]";
	x_value=-1;
}

#######################################################################
{
	gsub(/k/, "000", B);
	gsub(/M/, "000000", B);
	gsub(/k/, "000", M);
	gsub(/M/, "000000", M);
	gsub(/k/, "000", TS);
	gsub(/M/, "000000", TS);
	gsub(/k/, "000", TR);
	gsub(/M/, "000000", TR);
	gsub(/k/, "000", W);
	gsub(/M/, "000000", W);
	gsub(/k/, "000", S);
	gsub(/M/, "000000", S);
	gsub(/k/, "000", L);
	gsub(/M/, "000000", L);
	gsub(/k/, "000", F);
	gsub(/M/, "000000", F);	
	gsub(/k_/, "000-", $0);
	gsub(/M_/, "000000-", $0);
	gsub(/\"/, "", $0);
	
	type=$1;
	i=2;
	for (i=2;$i != "";i=i+2)
	{
		param=$i;
		first=$(i+1);
		second=$(i+2);
		if (param=="B" )
		{
			if (B!=-1)
			{
				if (B==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="S" )
		{
			i++; ##because we have also comma
			S=S+0;
			if (S!=-1)
			{
				if (S>=first && S<=second)
					continue;
				else
					break;
			}
			else
				x_value=(first+second)/2;
		}
		else if (param=="W" )
		{
			i++; ##because we have aslo comma
			W=W+0;
			if (W!=-1)
			{
				if (W>=first && W<=second)
					continue;
				else
					break;
			}
			else
				x_value=(first+seccond)/2;
		}
		else if (param=="TR" )
		{
			if (TR!=-1)
			{
				if (TR==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="TS" )
		{
			if (TS!=-1)
			{
				if (TS==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="L" )
		{
			if (L!=-1)
			{
				if (L==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="F" )
		{
			if (F!=-1)
			{
				if (F==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="M" )
		{
			if (M!=-1)
			{
				if (M==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
	}
	gsub(/\"/, "", name);
	if ($i == "" && x_value!=-1)
		printf( "x=%s ..\\reports\\%s.txt\n",x_value,name);
	
}

#######################################################################

END {
	
}

