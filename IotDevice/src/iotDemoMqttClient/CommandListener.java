package iotDemoMqttClient;

import org.apache.log4j.Logger;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.fasterxml.jackson.databind.ObjectMapper;

import iotSensorMessage.SensorInfo;
import iotDeviceServer.IotDeviceSensorCommunicator;

public class CommandListener extends AWSIotTopic {

	static Logger logger = Logger.getLogger(CommandListener.class.getName());

	private IotDeviceSensorCommunicator communicator;
	
    public CommandListener(String topic, AWSIotQos qos, IotDeviceSensorCommunicator communicator) {
        super(topic, qos);
        CommandListener.logger.info("topic :" + topic);
        this.communicator  = communicator;
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        CommandListener.logger.info("CommandListener " + System.currentTimeMillis() + ": <<< " + message.getStringPayload());
        ObjectMapper mapper = new ObjectMapper();
        String m = message.getStringPayload();
		try{
			SensorInfo info = mapper.readValue(m,SensorInfo.class);
	        this.communicator.Write(info.type, info.num, info.val);
		}catch(Exception e){
			CommandListener.logger.info("Exception occured @ mapper.readValue : "+e.toString());
		}
    }
}
