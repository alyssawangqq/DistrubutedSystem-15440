/* Sample code for basic Server */

public class Server {
    public static void main ( String args[] ) throws Exception {
	if (args.length != 2) throw new Exception("Need 2 args: <cloud_ip> <cloud_port>");
	ServerLib SL = new ServerLib( args[0], Integer.parseInt(args[1]) );

	// register with load balancer so requests are sent to this server
	SL.register_frontend();

	//if(SL.getStatusVM(4) == Cloud.CloudOps.VMStatus.NonExistent) SL.startVM();
	// main loop
	// 6 AM 1 EX VM
	while (true) {
	    System.out.println(SL.getTime());
	    if(SL.getTime() <= 6 ) {
		if(SL.getStatusVM(2) == Cloud.CloudOps.VMStatus.NonExistent) {
		    SL.startVM();
		}else {
		    //clean up
		    int i = 2;
		    while (SL.getStatusVM(i) != Cloud.CloudOps.VMStatus.NonExistent){
			SL.endVM(i);
		    }
		}
	    }

	    else if((SL.getTime() <= 17  || SL.getTime() >= 22 )&& SL.getStatusVM(3) == Cloud.CloudOps.VMStatus.NonExistent) {
		SL.startVM();
	    }

	    else {
		if(SL.getStatusVM(4) == Cloud.CloudOps.VMStatus.NonExistent) {
		    SL.startVM();
		}
	    }

	    Cloud.FrontEndOps.Request r = SL.getNextRequest();
	    SL.processRequest( r );
	}
    }
}

