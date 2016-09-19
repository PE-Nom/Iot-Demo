package iotSensor;

import java.util.Iterator;
import java.util.Vector;

import iotSensorDataModel.*;

public class IotSensorContainer {

	private IotSensorDataModelContainer dataModels;
	private Vector<IotSensorDummy> sensors = new Vector<IotSensorDummy>();
	
	public IotSensorContainer(IotSensorDataModelContainer dataModels){
		this.dataModels = dataModels;
		Vector<IotSensorDataModel> models = this.dataModels.getDataModels();
		Iterator<IotSensorDataModel> it = models.iterator();
		while(it.hasNext()){
			IotSensorDataModel model = it.next();
			IotSensorDummy sensor = new IotSensorDummy(model);
			sensors.add(sensor);
		}
	}

	public IotSensorDataModelContainer getDataModelContainer(){
		return this.dataModels;
	}
	
	public Vector<IotSensorDummy> getSensors(){
		return this.sensors;
	}
}
