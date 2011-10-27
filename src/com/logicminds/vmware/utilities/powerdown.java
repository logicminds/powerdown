package com.logicminds.vmware.utilities;


import java.net.URL;
import java.io.*;
import com.vmware.vim25.mo.*;

import com.vmware.vim25.mo.util.CommandLineParser;
import com.vmware.vim25.mo.util.OptionSpec;
import com.logicminds.utilities.*;

public class powerdown {

	/**
	 * @param args
	 */
	private String smtpserver="";
	private String username="";
	private String password="";
	private String msgbody="";
	private String defaultmsgsubject="";
	private String url="";
	private String from="";
	// wait time in seconds for all the vms to shutdown  before shutting down the host
	private Integer hostwaitperiod = new Integer(300);
	private boolean dryrun=true;
	private String email="";
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//CommandLineParser clp = new CommandLineParser(new OptionSpec[]{}, args);
	   	//String urlStr = clp.get_option("url");
  	    //String username = clp.get_option("username");
	    //String password = clp.get_option("password");
		powerdown pd = new powerdown();
		pd.readconfig(args[0]);
		ServiceInstance si = new ServiceInstance(new URL(pd.url), pd.username, pd.password, true);
		Folder rootFolder = si.getRootFolder();
		ManagedEntity[] mehosts = new InventoryNavigator(rootFolder).searchManagedEntities("ComputeResource");
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
		if(mes==null || mes.length ==0)
		{
			return;
		}
		if(mehosts==null || mehosts.length ==0)
		{
			return;
		}
		// This was used for when arguments were passed on the command line instead of a file
		//pd.getinput(mes);
		
		for (Object e : mes ){
			VirtualMachine vm = (VirtualMachine) e; 
			pd.shutdownvm(vm);
		
		}
		
		for (Object e : mehosts ){
			ComputeResource mehost = (ComputeResource) e; 
			HostSystem[] hosts = mehost.getHosts();
			for (HostSystem host : hosts){
				
				pd.shutdownhost(host);
			}
			
		
		}
		pd.msgbody += "\nThis is just a test, had this been real the entire virtual infrastructure in the testlab would have shutdown";
		Messenger message = new Messenger(pd.smtpserver);
		message.sendmsg(pd.email, pd.from, pd.defaultmsgsubject, pd.msgbody);
		si.getServerConnection().logout();
		
	}
	private void getinput(ManagedEntity[] mes){
		System.out.printf("\nYou are about to shutdown %d virtual machines\n\n", mes.length);
		for (int i=0; i < 10; i++){
			VirtualMachine vm = (VirtualMachine) mes[i];
			System.out.println(vm.getName());
		}
		System.out.print("...\n");
		
		
		//  open up standard input
	    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	    String response = "";
	    do  {
	    	System.out.print("\nAre you sure (y/n): ");
		    try {
		         response = br.readLine();
		        } 
		    catch (IOException ioe) {
		         System.out.println("IO error trying to read your answer!");
		        }
		} while ( !(response.equalsIgnoreCase("Y") || response.equalsIgnoreCase("N")) );
	    
	    if (response.equalsIgnoreCase("N") ){
	    	System.exit(1);
	    }
		
	}
	private void shutdownvm(VirtualMachine vm){
		
		if (!this.dryrun) {
			System.out.printf("\nShutting down %s", vm.getName());
			this.msgbody += "\nShutting down " + vm.getName();
			try {
				vm.shutdownGuest();
			}
			catch (Exception e){
				
			}
		}
		
	}
	/*TODO
	 * 1. Send email with status every few minutes
	 * 2. Send email when complete
	 * 3. 
	 * 
	 */
	private int checkpoweredvms(HostSystem host){
		/*
		 * Guest States
		 * Operation mode of guest operating system. One of:
			"running" - Guest is running normally.
			"shuttingdown" - Guest has a pending shutdown command.
			"resetting" - Guest has a pending reset command.
			"standby" - Guest has a pending standby command.
			"notrunning" - Guest is not running.
			"unknown" - Guest information is not available.
		 */
		VirtualMachine[] vms = null;
		int running = 0;
		int notrunning = 0;
		
		try{
			vms = host.getVms();
		}
		catch (Exception e){
			
		}
		for (VirtualMachine vm : vms){
			//System.out.println(vm.getName() + " is " + vm.getGuest().guestState);
			if (vm.getGuest().guestState.equalsIgnoreCase("running"))
				running++;
			
		}
		return running;
		
	}
	private void shutdownhost(HostSystem host){
		/*
		 * Need to wait a given amount of time before shutting down host
		 * We can also wait till the time and threshhold of vms shutdown reaches the given value
		 * 
		 */
		
		
		if (!this.dryrun){
			System.out.printf("\nShutting down %s", host.getName());
			this.msgbody += "\nShutting down " + host.getName();
			try{
				//host.powerDownHostToStandBy(60, false);
				host.shutdownHost_Task(true);
				
			}
			catch (Exception e){
				
			}
		}
		System.out.printf("\n%s Has %d vms running\n", host.getName(), this.checkpoweredvms(host));
		System.out.printf("%s is %s \n\n",host.getName(),host.getRuntime().powerState + "\n" );
		
		this.msgbody += "\n" + host.getName() + " has " + this.checkpoweredvms(host) + " vms running\n";
		this.msgbody += host.getName() + " is " + host.getRuntime().powerState + "\n";
		
	}
	private void readconfig(String filepath){
		
		try {
	        BufferedReader in = new BufferedReader(new FileReader(filepath));
	        String str;
	        while ((str = in.readLine()) != null) {
	            // Dont' process the comments
	        	if (str.startsWith("#"))
	            	continue;
	        	
	        	// We don't know what this is so read next line
	        	else if (!str.startsWith("--"))
	        		continue;
	        	
	        	else if (str.startsWith("--url")){
	        		this.url = str.split("=")[1].trim();
	        	    
	        	}
	        	else if (str.startsWith("--username")){
	        		this.username = str.split("=")[1].trim();
	        		
	        	}
	        	else if (str.startsWith("--password")){
	        		this.password = str.split("=")[1].trim();
	        		
	        	}
	        	else if (str.startsWith("--smtpserver")){
	        		this.smtpserver = str.split("=")[1].trim();
	        		
	        	}
	        	else if (str.startsWith("--msgsubject")){
	        		this.defaultmsgsubject = str.split("=")[1].trim();
	        		
	        	}
	        	else if (str.startsWith("--hostwaitperiod")){
	        		this.hostwaitperiod = Integer.parseInt(str.split("=")[1]);
	        	
	        	}
	        	else if (str.startsWith("--dryrun")){
	        		this.dryrun = Boolean.parseBoolean(str.split("=")[1]);
	        	}
	        	else if (str.startsWith("--email")){
	        		this.email = str.split("=")[1].trim();
	        		
	        	}
	        	else if (str.startsWith("--from")){
	        		this.from = str.split("=")[1].trim();
	        		
	        	}
	        	
	        	
	        }
	        
	        in.close();
	    } catch (IOException e) {
	    	
	    }


	}
	private void checkreqs(){
		// Check all the required variables in order for the program to run
	}
}
