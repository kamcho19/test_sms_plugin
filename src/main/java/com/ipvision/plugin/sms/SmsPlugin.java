package com.ipvision.plugin.sms;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import com.telepo.plugin.sms.AsyncSmsPlugin;
import com.telepo.plugin.sms.PluginAttribute;
import com.telepo.plugin.sms.SmsCallback;
import com.telepo.plugin.sms.SmsException;
import com.telepo.plugin.sms.SmsException.ReasonCode;
import com.telepo.plugin.sms.SmsOrigin;
import com.telepo.plugin.sms.SmsPluginCapability;

public class SmsPlugin implements AsyncSmsPlugin, SmsPluginCapability, BundleActivator  {
	
    private static ServiceRegistration serviceRegistration;
    private static org.osgi.framework.Version bundleVersion;
    private static final List<Locale> supportedLocales;
    private Map<String, String> settings;    
    
    private ZContext ctx;
    private static Socket client;
    private static String caseId;
    private static String code;
    private static PollItem[] items;
    private static int rc;
    private long startSendTime;
    private long startRepTime;

    //configuration
    private final static String MAX_LENGTH = "784";	
    private final static String MIN_LENGTH = "1";
    private final static String CUSTOMER_ID ="MITELEPO";
	private final static int POLL_TIMEOUT =86400000;//2500;
	private final static int SEND_TIMELIMIT = 10;//86400;	//1day second 1*24*60*60
	private final static int REPLY_TIMELIMIT = 10;//86400;	//1day second 1*24*60*60
	private final static int REQUEST_RETRIES = 3;
    private final static String SERVER_ENDPOINT = "tcp://10.0.9.64:9002"; //real
//	private final static String SERVER_ENDPOINT = "tcp://localhost:5555"; //test


    static {
        Locale[] locales = {
                Locale.ENGLISH,
                
                Locale.GERMAN,
                Locale.FRENCH,
                new Locale("sv"),

                new Locale("nl"),
                new Locale("nl_BE")};

        supportedLocales = Arrays.asList(locales);
    }

    public SmsPlugin() {
        // Public constructor used only when instantiating as plugin factory.
    }

    public final void openSocket()
    {	
		ctx = new ZContext();
    }

        
  
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceRegistration = bundleContext.registerService(
                SmsPlugin.class.getName(), new SmsPlugin(), null);
        bundleVersion = bundleContext.getBundle().getVersion();  
        
        openSocket();        
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {	
    	serviceRegistration.unregister();       
    }

    @Override
    public List<PluginAttribute> getAttributes(Locale locale) {
        List<PluginAttribute> attributeList = new LinkedList<>();
        attributeList.add(new PluginAttribute("First",
                "First attribute, only characters",
                "[-a-zA-Z]+",
                PluginAttribute.AttributeType.TEXT));
        attributeList.add(new PluginAttribute("Second",
                "Second attribute, password",
                "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]+",
                PluginAttribute.AttributeType.PASSWORD));
        attributeList.add(new PluginAttribute("Third",
                "Third attribute, only digits (values 0-7 will generate errors when sending messages",
                "[-0-9]+",
                PluginAttribute.AttributeType.TEXT));

        return attributeList;
    }

    @Override
    public String getDescription(Locale locale) {
        return "A plugin skeleton implementation";
    }

    @Override
    public String getName() {
        return "IPVISION SmsPlugin";
    }

    @Override
    public Version getVersion() {
        return new Version(bundleVersion.getMajor(), bundleVersion.getMinor(), bundleVersion.getMicro());
    }

    @Override
    public SmsPlugin instance(Map<String, String> attributeNameToValues) throws SmsException {
    	SmsPlugin plugin = new SmsPlugin();
        plugin.loadSettings(attributeNameToValues);
        return plugin;
    }

    private void loadSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    @Override
    public void send(String toNumber, String fromNr, String fromName, String text) throws SmsException {
        throw new SmsException("Synchronous sending is not supported");
    }

    @SuppressWarnings({"ThrowFromFinallyBlock", "ConstantConditions"})
    @Override
    public void send(String toNumber, String fromNr, String fromName, String text, SmsOrigin origin, SmsCallback callback) {
    	/* ================= make sms message ======================*/    	
        client = ctx.createSocket(ZMQ.REQ);
        assert (client != null); // true - countinue, false - stop and Assertion Error
        client.connect(SERVER_ENDPOINT);
        items =new PollItem[]{new PollItem(client, Poller.POLLIN)};    	
    	//
    	JSONObject smsData = new JSONObject();
    	JSONObject smsObj = new JSONObject();

    	JSONArray toArr = new JSONArray();
        toArr.add(toNumber);
        
        smsObj.put("to", toArr);        
        smsObj.put("to_cnt", toArr.size());        
    	smsObj.put("from", fromNr);
        smsObj.put("message", text);
        smsObj.put("customer_id", CUSTOMER_ID);
        smsObj.put("user_id", fromName);
        
        smsData.put("sms_send", smsObj);
        String msgData = smsData.toString();
        System.out.println("msgData =====> "+msgData);
        
        startSendTime = System.currentTimeMillis()/1000;    	        
        //
        int testCnt =0;
        long diffTime = (System.currentTimeMillis()/1000) - startSendTime;        
        try{
	        while(diffTime <SEND_TIMELIMIT && !Thread.currentThread().isInterrupted() )
	        {
	        	//
	        	testCnt ++;
	        	System.out.println("======== "+testCnt+" times retry ========");
	        	
	        	
	    	    Boolean msgResult = client.send(msgData);
	    	    
	    	    if(msgResult)
	    	    {
	    	    	//msgResult : true
	    	    	
		    	    int expect_reply = 1;
		    	    while(expect_reply >0)
		    	    {
		                rc = ZMQ.poll(items, POLL_TIMEOUT); 
		                
		                if(rc == 0 || rc == -1)
		                {
		                	// 0 : no connect
		                	// -1 : occur error 
		                	checkSmsException(ReasonCode.CONNECT_FAILED, "connect failed");
		                	diffTime = (System.currentTimeMillis()/1000) - startSendTime;
		                	break;
		                }
		                
		                if(items[0].isReadable())
		                {
		                	String sendRes = client.recvStr();
		                	if(sendRes == null){
		                		diffTime = (System.currentTimeMillis()/1000) - startSendTime;
		                		break;
		                	}
		                	
		                	//success
		                	//available reply               	 
		                	
		                	System.out.println("receive reply from server =====>"+sendRes.toString());
		                	startRepTime =System.currentTimeMillis()/1000;
		                	try{    	
		            	    	JSONParser jsonParser = new JSONParser();
		            	    	
		            	    	JSONObject jsonObj = (JSONObject)jsonParser.parse(sendRes);	            	    	
		            	    	JSONObject resObj = (JSONObject)jsonObj.get("sys_error");	            	    	
		            	    	System.out.println("resObj =====> "+resObj);
		            	    	if(resObj == null)
		            	    	{	
		            	    		resObj = (JSONObject)jsonObj.get("sms_send_res");
		            	    		caseId = resObj.get("case_id").toString();		            	    	
			            	    	code = resObj.get("code").toString();
			            	    	System.out.println("code ======> "+code);
			            	    	if(code.equals("1"))
			            	    	{
			            	    		// sending success
			            	    		System.out.println("======sending success======");	
			            	    		
			            	    		//result							
			            	    		/*{"sms_rep": {"case_id": "4ffb2a5d-6f93-40a9-9d0a-8176b16c58ae"}}*/
			            	    		smsData.clear();
			            	    		smsObj.clear();
			            	    		smsObj.put("case_id", caseId);
			            	    		smsData.put("sms_rep", smsObj);	            	    		
			            	    		
			            	    		String repData = smsData.toString();			            	    		
			            	    		
			            	    		System.out.println("repData ====>"+repData);
			            	    		
			            	    		
			            	    		int expect =1;			            	    		
			            	    		while(expect >0)
			            	    		{
				            	    		Boolean repResult =	client.send(repData);
				            	    		System.out.println("repResult =====>"+repResult);
				            	    		
				            	    		items =new PollItem[]{new PollItem(client, Poller.POLLIN)};
				            	    		rc = ZMQ.poll(items, POLL_TIMEOUT);
				            	    		
				            	    		if(rc ==0 ||rc ==-1)
				            	    		{
				            	    			//no connection
				            	    			break;
				            	    		}
				            	    		
				            	    		if(items[0].isReadable())
				            	    		{
				    		                	String repRes = client.recvStr();
				    		                	if(repRes == null)
				    		                	{
				    		                		//retry
				    		                		break;
				    		                	}
				    		                	
				    		                	try
				    		                	{
				    		                		jsonObj = (JSONObject)jsonParser.parse(repRes);	            	    	
				    		            	    	JSONObject repObj = (JSONObject)jsonObj.get("sys_error");	            	    	
				    		            	    	System.out.println("resObj =====> "+repObj);
				    		            	    	if(repObj == null)
				    		            	    	{	
				    		            	    		repObj = (JSONObject)jsonObj.get("sms_rep_res");
				    		            	    		caseId = repObj.get("case_id").toString();		            	    	
				    			            	    	code = repObj.get("code").toString();
				    			            	    	JSONArray arr = (JSONArray) repObj.get("results");
				    			            	    	System.out.println("##arr.toString ====> "+arr.toString());
				    			            	    	JSONObject temp = (JSONObject) arr.get(0);
				    			            	    	System.out.println("##on_going ===>"+ temp.get("on_going").toString());
				    			            	    	int onGoing = Integer.parseInt(temp.get("on_going").toString());
				    				                	diffTime =(System.currentTimeMillis()/1000)- startRepTime ;
				    				                	System.out.println("##diffTime =====> "+diffTime);
				    				                	if(diffTime > REPLY_TIMELIMIT)
				    				                	{
				    				                		//time out
				    				                		System.out.println("#######Time out#######");
				    				                		break;
				    				                	}
				    			            	    	
				    			            	    	if(onGoing == 0)
				    			            	    	{	
				    			            	    		//success 0
//				    			            	    		expect =0;
				    			            	    		System.out.println("#######success#######");
				    			            	    		break;
				    			            	    		
				    			            	    	}
				    		            	    	}
				    		                	}catch (Exception e)
				    		                	{
				    		                		
				    		                	}

				    		                	
				            	    		}
			            	    			
			            	    			
			            	    		}
			            	    		
			            	    	}else if(code.equals("0")){
			            	    		// sending fail
			            	    		System.out.println("======sending fail======");
			            	    		JSONObject errObj = (JSONObject) resObj.get("error_addr");
			            	    		String error = errObj.get("error").toString();
			            	    		System.out.println("error ======> "+error);
			            	    	}else{
			            	    		
			            	    	}	            	    		
		            	    	}else{
		            	    		//System Error
		            	    		
		            	    		/*
		            	    	    SMS_ERR_NONE    = 0,    ///< No error
		            	    	    SMS_ERR_UNKNOWN = 1,    ///< Unknown error.
		            	    	    SMS_ERR_TOO_MUCH_REQUEST = 2,   ///< too much requests.
		            	    	    SMS_ERR_INVALID_REQUEST  = 3,                            //!< SMS_ERR_INVALID_REQUEST
		            	    	    SMS_ERR_NO_MATCH_REQUEST  = 4,                           //!< SMS_ERR_NO_MATCH_REQUEST

		            	    	    SMS_ERR_SERVICE             = 10,   ///< Service error(Unknown)
		            	    	    SMS_ERR_INVALID_INPUT       = 11,   ///< Invalid input
		            	    	    SMS_ERR_NO_VALID_ADDRESS    = 12,   ///< No valid address
		            	    	    SMS_ERR_INVALID_GROUP       = 13,   ///< Invalid group
		            	    	    SMS_ERR_MESSAGE_TOO_LONG    = 14,   ///< Too long message.
		            	    	    SMS_ERR_DELIVERY_RECEIPT_NOTIFICATION_NOT_SUPPORT   = 15,//!< SMS_ERR_DELIVERY_RECEIPT_NOTIFICATION_NOT_SUPPORT
		            	    	    SMS_ERR_UNRECOGNIZED_DATA_FORMAT    = 16,                //!< SMS_ERR_UNRECOGNIZED_DATA_FORMAT
		            	    	    SMS_ERR_INVALID_CHARGING    = 17,   ///< Invalid Charging Information.

		            	    	    SMS_ERR_POLICY              = 20,   ///< Policy error(Unknown)
		            	    	    SMS_ERR_GROUPS_NOT_ALLOWED  = 21,   ///< Not allowed groups
		            	    	    SMS_ERR_NESTED_GROUPS_NOT_ALLOWED   = 22,   ///< Not allowed nested groups
		            	    	    SMS_ERR_CHARGING_NOT_ALLOWED        = 23,   ///< Not allowed charging

		            	    	    SMS_ERR_MESSAGE_TOO_SHORT   = 30,                        //!< SMS_ERR_MESSAGE_TOO_SHORT
		            	    	    SMS_ERR_NOT_MATCH_COUNT     = 31,                        //!< SMS_ERR_NOT_MATCH_COUNT
		            	    	    SMS_ERR_NO_CUSTOMER_ID      = 32,                        //!< Could not get correct customer_id.
		            	    		*/	            	    		
		            	    		
		            	    		
		            	    		int errCode= Integer.parseInt(resObj.get("code").toString());
		            	    		String sysErr = resObj.get("message").toString();
		            	    		ReasonCode reasonCode =ReasonCode.OTHER_ERROR;
		            	    		
		            	    		switch (errCode)
		            	    		{
			            	    		case 1:
			            	    		case 2:
			            	    			/*SMS_ERR_UNKNOWN = 1,    ///< Unknown error.*/
			            	    			/*SMS_ERR_TOO_MUCH_REQUEST = 2,   ///< too much requests.*/
			            	    			reasonCode = ReasonCode.OTHER_ERROR;
			            	    			break;		            	    			
			            	    		
		            	    		}
		            	    		
		            	    		checkSmsException(reasonCode, sysErr);
		            	    	}	            	    	
		                	}catch(Exception e){
		                		e.printStackTrace();
		                	}
		                	
		                	diffTime = SEND_TIMELIMIT;
		                	break;
		                }
		    	    }
	    	    }else{
	    	    	// msgResult : false
                	checkSmsException(ReasonCode.CONNECT_FAILED, "connect failed");
	    	    }
	        }
        }catch(Exception e){
        	e.printStackTrace();
        }finally{
//        	if(exception != null)        		
//        	callback.onFailure(exception);
        }  

        ctx.destroy();	
        callback.onSent();
        
    }

	@Override
	public boolean isLangSupported(Locale locale) {
		return supportedLocales.contains(locale);
	}

	@Override
	public String getMaxSMSLength() {
		return MAX_LENGTH;
	}
	
	public String getMinSMSLength() {
		return MIN_LENGTH;
	}
	
	public String getCaseId(){
		return caseId;
	}
	
	private void checkSmsException(ReasonCode rcd, String errMsg){
		SmsException exception = null;
		exception = new SmsException(rcd, errMsg);
		
		System.out.println(exception.getVendorMessage());
		System.out.println(exception.getReasonCode());
		System.err.println(exception.getReasonCode());
		
		exception.printStackTrace();		
	}


}
