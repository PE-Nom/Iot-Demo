package iotDeviceDemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotException;

import iotDemoCredentialSample.IotDemoCredentials;
import iotDeviceServer.IotDeviceServer;
import iotDemoMqttClient.MqttClient;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

public class IotDeviceDemo {
	
	static Logger logger = Logger.getLogger(IotDeviceDemo.class.getName());

	/* ----------------------------------------
	 *  main
	 */
	public static void main( String...args ) {

		String identity_pool_id = null;
		String clientEndpoint = null;
//		String clientId		  = "UUID";		// ユニークな文字列であればOK。最終的にはUUIDで生成する。

		DOMConfigurator.configure(".\\log4j.xml");
		BasicConfigurator.configure();

		Properties properties = new Properties();
		try{
			InputStream fileInputStream = new FileInputStream(new File("credential.properties"));
			properties.load(fileInputStream);
			identity_pool_id = properties.getProperty("identity_pool_id");
			clientEndpoint = properties.getProperty("client_entpoint");
			logger.info("identity_pool_id ="+identity_pool_id);
			logger.info("clientEnfpoint = "+clientEndpoint);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		MqttClient mqttClient = new MqttClient(identity_pool_id, clientEndpoint);
		
       	try{
       		mqttClient.connect();
       		IotDeviceServer server = new IotDeviceServer("127.0.0.1", 3575, mqttClient);
//			 	IotDeviceServer server = new IotDeviceServer("127.0.0.1", 3575);
			logger.info("start");
			while(true);
        }catch(AWSIotException e){
        	logger.info(String.format("AWSIotException @ main : %s"+e.getErrorCode()));
		}catch(IOException e){
			logger.info("Exception @ main" + e.toString());
		}
	}
}
