package iotSensorDataModel;

import java.util.Map;

public interface IotSensorDataModelListener {
	public void updateSampleingData(Map<String,Object> mes);
}
