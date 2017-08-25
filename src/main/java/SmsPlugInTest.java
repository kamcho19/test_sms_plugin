import java.io.IOException;

import com.ipvision.plugin.sms.SmsPlugin;
import com.telepo.plugin.sms.SmsCallback;
import com.telepo.plugin.sms.SmsException;
import com.telepo.plugin.sms.SmsOrigin;

class TestSmsCallback implements SmsCallback{	
	
	public void TestSmsCallback(){};
	
	@Override
	public void onSent(){
		System.out.println("interface call");
		
	}	
	@Override
	public void onFailure(SmsException e){
		
	}

}


public class SmsPlugInTest {	
	

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception  {
		
		
		// Test code
		System.out.println("Hello SMS plugin Test");
		
        SmsPlugin sp = new SmsPlugin();
        
        
        sp.openSocket();
//normal
        sp.send("+4550221422", "Ipvision", "Ipvision", "Hello1, world", SmsOrigin.TEST, new TestSmsCallback());
//blank message
//        sp.send("+4550221422", "Ipvision", "Ipvision", "", SmsOrigin.TEST, new TestSmsCallback());
//null message
//        sp.send("+4550221422", "Ipvision", "Ipvision",  null, SmsOrigin.TEST, new TestSmsCallback());
//error destination
//        sp.send("", "Ipvision", "Ipvision", "Hello1, world", SmsOrigin.TEST, new TestSmsCallback());
//      sp.send(toNumber, fromNr, fromName, text, origin, callback);

		sp.closeSocket();

	}

}
