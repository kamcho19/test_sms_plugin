import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zeromq.ZMQ;

public class Hwclient {

//	private final static String SERVER_ENDPOINT = "tcp://10.0.9.64:9002";
	private final static String SERVER_ENDPOINT = "tcp://localhost:5555";
    public static void main(String[] args) {
    	
    	
        ZMQ.Context context = ZMQ.context(1);

        //  Socket to talk to server
        System.out.println("Connecting to hello world server…");

        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect(SERVER_ENDPOINT);
        //System.out.println("connect Complete : "+connectResult);
        
       
        
        
        JSONObject jsonData = new JSONObject();
        JSONObject jsonObj = new JSONObject();
        
        /*
        {"sms_send": 
        	{
        		"from": "Ipvision", 
        		"to_cnt": 1, 
        		"message": "Hello, world", 
        		"customer_id": "", 
        		"to": ["+4550606903"], 
        		"user_id": ""
        	}
        }
        */
        
        /*
        {
        	  "sms_send": {
        	    "to": [
        	      "4550606903"
        	    ],
        	    "from": "",
        	    "message": "tet",
        	    "to_cnt": 1,
        	    "user_id": "",
        	    "customer_id": "ipvstk_dev"
        	  }
        	}
        */
        
        
        jsonObj.put("from", "Ipvision");
        jsonObj.put("to_cnt", 1);
        jsonObj.put("message", "tet");
        jsonObj.put("customer_id", "ipvstk_dev");            
        
        JSONArray toArr = new JSONArray();
        toArr.add("4550606903");
        jsonObj.put("to", toArr );
        jsonObj.put("user_id", "");
    	
        jsonData.put("sms_send", jsonObj);
    	
        String s = jsonData.toString();
        System.out.println("Data : "+s);
        //send
        Boolean result = requester.send(s);            
        System.out.println("Send Result : "+result);
        
        byte[] reply = requester.recv(0);
        System.out.println("Received " + new String(reply));

        
        /*(for (int requestNbr = 0; requestNbr != 10; requestNbr++) {
            String request = "Hello";
            System.out.println("Sending Hello " + requestNbr);
            requester.send(request.getBytes(), 0);

            byte[] reply = requester.recv(0);
            System.out.println("Received " + new String(reply) + " " + requestNbr);
        }
        */
        requester.close();
        context.term();
    }
}