import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zeromq.ZMQ;

public class Hwserver {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	  ZMQ.Context context = ZMQ.context(1);

          //  Socket to talk to clients
          ZMQ.Socket responder = context.socket(ZMQ.REP);
          responder.bind("tcp://*:5555");

          
          

          // Send reply back to client
          String reply = "";
          
          JSONObject jsonData = new JSONObject();
          JSONObject jsonObj = new JSONObject();
          JSONArray errorArr = new JSONArray();
          
          jsonObj.put("code", 0);
          jsonObj.put("case_id", "6d20db26-15c6-4901-a874-5542ba7ad8bc");
      	  jsonObj.put("error_addr", errorArr);
      	  jsonData.put("sms_send_res", jsonObj);
          
//          while (!Thread.currentThread().isInterrupted()) {
              // Wait for next request from the client
              String request = responder.recvStr(0);
              System.out.println("request data : "+request);              

              // Do some 'work'
              Thread.sleep(1000);
              reply = jsonData.toString();
              responder.send(reply.getBytes(), 0);
//          }
          responder.close();
          context.term();
    }
}