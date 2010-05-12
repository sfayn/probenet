BEGIN {
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
{

}
END {
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
	if (B==-2)
		printf("Buffer Size in bits\n");
	else if (S==-2)
		printf("Average Speed in m/s\n");
	else if (W==-2)
		printf("Average Wait Time in seconds\n");
	else if (TR==-2)
		printf("Transmit Range in meters\n");
	else if (TS==-2)
		printf("Transmit Speed in bps\n");
	else if (L==-2)
		printf("Number of messages disseminated(L)\n");
	else if (N==-2)
		printf("Number of nodes in network\n");
	else if (MS==-2)
		printf("Average Message size in bits\n");
	else if (CI==-2)
		printf("Average Message Creation Interval in seconds\n");
	else if (F==-2)
		printf("Maximum Number of FTC\n");
	else if (M==-2)
		printf("Maximum Message Size\n");
}

