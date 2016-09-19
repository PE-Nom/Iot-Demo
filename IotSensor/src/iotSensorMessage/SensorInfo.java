package iotSensorMessage;

public class SensorInfo {
	private String info;
	public int id;
	public String type;
	public String num;
	public String val;
	
	public SensorInfo(){}
	public SensorInfo(String info){this.info=info;}
	public SensorInfo(int id, String type,String num,String val){
		this.id =id;
		this.type = type;
		this.num = num;
		this.val = val;
	}

	public int getId(){return this.id;}
	public void setId(int id){this.id = id;}
	
	public String getType(){return this.type;}
	public void setType(String type){this.type = type;}

	public String getNum(){return this.num;}
	public void setNum(String num){this.num = num;}
	
	public String getVal(){return this.val;}
	public void setVal(String val){this.val = val;}
	
}
