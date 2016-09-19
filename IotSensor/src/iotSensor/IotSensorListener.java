package iotSensor;

public interface IotSensorListener {
	public void samplingStatusChanged(boolean status);
	public void loadingStatusChanged(boolean status);
}
