BEGIN {
	FS = "[,_-]";
	x_value=-1;
	error=0.01;
	#default values
	L=4
	N =60
	MS=55000
	S= 8.3
	B=1000000
	W= 60
	TR= 30
	TS=3000000
	CI =30
	F=4
	M=100000
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
	gsub(/k/, "000", N);
	gsub(/M/, "000000", N);	
	gsub(/k/, "000", MS);
	gsub(/M/, "000000", MS);	
	gsub(/k/, "000", CI);
	gsub(/M/, "000000", CI);	
	gsub(/\"/, "", $0);
	name=$0;
	gsub(/k_/, "000_", $0);
	gsub(/M_/, "000000_", $0);
	gsub(/k,/, "000,", $0);
	gsub(/M,/, "000000,", $0);
	type=$1;
	i=2;
	for (i=2;$i != "";i=i+2)
	{
		param=$i;
		first=$(i+1);
		second=$(i+2);
		if (param=="B" )
		{
			#printf("B");
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
			#printf("S");
			i++; ##because we have also comma
			S=S+0;
			average=(first+second)/2.0;
			if (S!=-1)
			{
				if (S>=average-error && S<=average+error)
					continue;
				else
					break;
			}
			else
				x_value=average;
		}
		else if (param=="W" )
		{
			#printf("W");
			i++; ##because we have also comma
			W=W+0;
			average=(first+second)/2.0;
			if (W!=-1)
			{
				if (W>=average-error && W<=average+error)
					continue;
				else
					break;
			}
			else
				x_value=average;
		}
		else if (param=="TR" )
		{
			#printf("TR");
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
			#printf("TS");
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
			#printf("L");
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
		else if (param=="N" )
		{
			#printf("N");
			if (N!=-1)
			{
				if (N==first)
					continue;
				else
					break;
			}
			else
				x_value=first;
		}
		else if (param=="MS" )
		{
			#printf("MS");
			i++; ##because we have also comma
			MS=MS+0;
			average=(first+second)/2.0;
			#printf("%g",average);
			if (MS!=-1)
			{
				if (MS>=average-error && MS<=average+error)
					continue;
				else
					break;
			}
			else
				x_value=average;
		}
		else if (param=="CI" )
		{
			#printf("CI");
			i++; ##because we have also comma
			CI=CI+0;
			average=(first+second)/2.0;
			if (CI!=-1)
			{
				if (CI>=average-error && CI<=average+error)
				{
					if (L==-1 && x_value==-1) ## needed in case L is varing and we are in AFS
						x_value=-2;
					continue;
				}
				else
					break;
			}
			else
				x_value=average;
		}
		else if (param=="F" )
		{
			#printf("F");
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
			#printf("M");
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
	#printf("\n");
	gsub(/ /, "", name);
	if ($i=="" && x_value==-2) # in case we are in AFS and L is varing
	{
		printf( "x=2 ..\\reports\\%s.txt\n",name);
		printf( "x=40 ..\\reports\\%s.txt\n",name);
	}		
	else if ($i == "" && x_value!=-1)
		printf( "x=%s ..\\reports\\%s.txt\n",x_value,name);
	
}

#######################################################################

END {
	
}