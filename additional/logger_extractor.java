/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package additional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

/**
 *
 * @author Jad Makhlouta
 */
public class logger_extractor {

    /**
     * @param args the command line arguments
     */
	private static class message_info
	{
		public int hop_count=0;
		public double starting_time=-1;
		public double finishing_time=-1;
		LinkedList<Double> priorities=new LinkedList<Double>();
		message_info(double start)
		{
			starting_time=start;
		}
		public double average_priority()
		{
			double sum=0;
			for(int i=0;i< priorities.size();i++)
				sum+=priorities.get(i);
			return sum/priorities.size();
		}
		public double latency()
		{
			return (reached()?finishing_time-starting_time:-1);
		}
		public boolean reached()
		{
			return (finishing_time>starting_time);
		}
		@Override
		public String toString()
		{
			return "hop_count: "+hop_count+ (reached()?" latency: "+latency():" did_not_reach")+ " average_priority: "+average_priority();
		}

	};
    public static void main(String[] args) throws IOException {
        if (args.length<2)
			return;
		File file= new File(args[0]);
		if (!file.exists())
			return;
		file.setReadable(true);
		FileReader r =new FileReader(file);
		BufferedReader reader=new BufferedReader(r);
		String s;
		Vector<message_info> messages=new Vector<message_info>(1000);
		messages.add(0,null);
		int num_of_nodes=0;
		double sum_message_count=0;
		while (true)
		{
			s=reader.readLine();
			if (s==null)
				break;
			String [] words=s.split(" ");
			if (words.length<3)
				break;
			if (words[0].equals("Message") && words[2].equals("created_at"))
			{
				message_info info=new message_info(Double.parseDouble(words[5]));
				messages.add(Integer.parseInt(words[1].substring(1)),info);
			}
			else if (words[0].equals("Message") && words[2].equals("relayed_to"))
			{
				int i=Integer.parseInt(words[1].substring(1));
				message_info info=messages.get(i);
				info.hop_count++;
				info.priorities.add(Double.parseDouble(words[7]));
				messages.set(i,info);
			}
			else if (words[0].equals("Message") && words[2].equals("reached"))
			{
				int i=Integer.parseInt(words[1].substring(1));
				message_info info=messages.get(Integer.parseInt(words[1].substring(1)));
				info.priorities.add(Double.parseDouble(words[7]));
				info.finishing_time=Double.parseDouble(words[5]);
				messages.set(i,info);
			}
			else if (words[0].equals("number_of_messages_at"))
			{
				sum_message_count+=Integer.parseInt(words[2]);
				num_of_nodes++;
			}
			else
				break;
		}
		File out= new File(args[1]);
		if (!out.exists())
			out.createNewFile();
		else
		{
			out.delete();
			out.createNewFile();
		}
		out.setWritable(true);
		FileWriter logger =new FileWriter(out);
		logger.write("average number of messages per node="+(sum_message_count/(double)num_of_nodes)+"\n");
		logger.flush();
		double [] sum_average_latency=new double[10];
		int [] num=new int[10];
		int [] not_reach=new int[10];
		int len=sum_average_latency.length;
		for (int j=0;j<len;j++)
		{
			sum_average_latency[j]=0;
			num[j]=0;
			not_reach[j]=0;
		}
		for (int i=1;i<messages.size();i++)
		{
			message_info m=messages.get(i);
			for (int j=0;j<len;j++)
			{
				double av=m.average_priority();
				if (av>j/(double)len && av<=(j+1)/(double)len)
					if (m.reached())
					{
						sum_average_latency[j]+=m.latency();
						num[j]++;
						break;
					}
					else
						not_reach[j]++;
			}
			/*logger.write(i+" "+messages.get(i).toString()+"\n");
			logger.flush();*/

		}
		for (int j=0;j<len;j++)
		{
			logger.write(" for priority =["+(j/(double)len) + "," +((j+1)/(double)len)+"], average latency="+(sum_average_latency[j]/(double)num[j])+", not reached = "+not_reach[j]+" from "+(not_reach[j]+num[j])+"\n");
			logger.flush();
		}
		logger.write("--------------------------------------\n");
		logger.flush();
		for (int i=1;i<messages.size();i++)
		{
			logger.write(i+" "+messages.get(i).toString()+"\n");
			logger.flush();
		}
    }

}
