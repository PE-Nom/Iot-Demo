package iotSensorDemo;

import java.io.IOException;
import java.util.Iterator;

import iotSensor.IotSensorContainer;
import iotSensorDataModel.IotSensorDataModelContainer;
import iotSensorDataModel.IotSensorDataModel;
import iotSensorViewer.IotSensorViewerContainer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

import iotSensor.*;
import iotSensorClient.*;

public class IotSensorDemo {

	static Logger logger = Logger.getLogger(IotSensorDemo.class.getName());

	public IotSensorDemo(){
		
	}

	public static void main(String args[]){

		DOMConfigurator.configure(".\\log4j.xml");
		BasicConfigurator.configure();

		IotSensorDataModelContainer dataModelContainer = new IotSensorDataModelContainer();
		IotSensorContainer sensorContainer = new IotSensorContainer(dataModelContainer);
		IotSensorViewerContainer chartContainer = new IotSensorViewerContainer(sensorContainer);

		Iterator<IotSensorDummy> sensorit = sensorContainer.getSensors().iterator();
		while(sensorit.hasNext()){
			IotSensorDummy sensor = sensorit.next();
			try{
				IotSensorDataModel model = sensor.getDataModel();
				IotSensorClient client = new IotSensorClient("127.0.0.1", 3575, sensor);
				sensor.addListener(client);
				model.addListener(client);
			}catch(IOException e){
				logger.info("Exception @ IoSensorDemo main");
			}
		}
        
		logger.info("channels : " + dataModelContainer.getChannels());

		chartContainer.dispatch();
	
		logger.info("Close Buttom Pressed!");
	}

}
