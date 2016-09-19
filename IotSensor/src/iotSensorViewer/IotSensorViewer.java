package iotSensorViewer;

import java.util.Map;
import java.util.Iterator;
import java.awt.Color;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.axis.ValueAxis;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import iotSensor.*;
import iotSensorDataModel.*;

public class IotSensorViewer implements IotSensorDataModelListener, IotSensorListener{
	
	static Logger logger = Logger.getLogger(IotSensorViewer.class.getName());

	private int xscaleRange = 180;
	private int channel;
	private Shell shell;
	private IotSensorDataModel dataModel;
	private IotSensorDummy sensor;
	private ChartComposite chartComp;
	private Color[] colors = {Color.RED, Color.CYAN, Color.GREEN};
	private XYSeriesCollection data;
	private XYSeries sig;
	private Button sensorLoadBtn;
	private Button startSampBtn;
	private Button startMonitorBtn;
	private Text txt;
	private CLabel sensorLoadingStatuslbl;
	private CLabel samplingStatuslbl;
	private CLabel monitorStatuslbl;
	private boolean monitorStatus = false;
	
	private Map<String,Object> arrivalData;
	private XYPlot xyPlot;
	private XYItemRenderer renderer;
	private ValueAxis xscale;
	private ValueAxis yscale;
	
	public IotSensorViewer(IotSensorDummy sensor, Shell shell){
		this.sensor = sensor;
		this.dataModel = sensor.getDataModel();
		this.channel = this.dataModel.getChannel();
		this.shell = shell;
		this.data = new XYSeriesCollection();
	    createTimeSerieseChart();
	    createLoadSensorButton();
	    createSensorLoadingStatusLabel();
	    createStartSamplingButton();
	    createSamplingStatusLabel();
	    createStartMonitorButton();
	    createMonitoringStatusLabel();
	    createTextBox();
	    this.sensor.addListener(this);
	    startMonitor();
	}

	//�@���n��O���t�`���[�g
	private void createTimeSerieseChart(){

		// �[���Z���T�f�[�^�̐ݒ�
		this.sig = new XYSeries("Sensor Sig.");
		this.data.addSeries(sig);
		
		//�@�O���t�쐬
	    JFreeChart chart = 
	    		ChartFactory.createXYLineChart(
	    				"Pseudo Sensor"+this.channel+" Signal",
	    				"points",
	    				"value",
	    				data,
	    				PlotOrientation.VERTICAL, //* �O���t�`�����(HORIZONTAL)
	    				true,
	    				false,
	    				false);
	    
	    chart.setBackgroundPaint(Color.LIGHT_GRAY);

	    this.xyPlot = chart.getXYPlot();
	    this.xyPlot.setBackgroundPaint(Color.BLACK); // �w�i������
	    this.xyPlot.setDomainGridlinePaint(Color.white); //* �c��؂�� ...by awt
	    this.xyPlot.setRangeGridlinePaint(Color.white); //* ����؂�� ...by awt
		this.renderer = xyPlot.getRenderer();
		this.renderer.setSeriesPaint(0, colors[this.channel]); //* �O���t���̐F
		
		this.yscale = xyPlot.getRangeAxis();
		this.yscale.setRange(-1.1,1.1);
		this.xscale = xyPlot.getDomainAxis();
		this.xscale.setRange(0,xscaleRange);
	
	    GridData grid = new GridData();
	    grid.verticalSpan = 4;
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=1024;
	    
	    // �R���e�L�X�g���j���[�Ȃ��ō\�z
		this.chartComp = new ChartComposite(this.shell,SWT.NONE,chart,false,false,false,false,false);
		this.chartComp.setLayoutData(grid);
		// �}�E�X����ł̃Y�[���C���^�A�E�g�@disable
		this.chartComp.setDomainZoomable(false);
		this.chartComp.setRangeZoomable(false);
		
	}

	// SensorLoading�{�^�����������ꂽ���̃C�x���g����
	private class sensorLoadingBtnListener implements SelectionListener{
		public sensorLoadingBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.sensor.getLoadingStatus()){
				IotSensorViewer.this.sensor.unloading();
			}else{
				IotSensorViewer.this.sensor.loading();
			}
		}
	}
	
	/*
	 * IotSensorlListener �̃C�x���g���X�i�[
	 */
	public void loadingStatusChanged(boolean status){
		this.shell.getDisplay().asyncExec(new UpdateloadingStatusChangedChagedCmd());
	}
	
	private class UpdateloadingStatusChangedChagedCmd implements Runnable{
		public UpdateloadingStatusChangedChagedCmd(){
			
		}
		public void run(){
			if(IotSensorViewer.this.sensor.getLoadingStatus()){
				IotSensorViewer.this.sensorLoadBtn.setText("UNLOAD");
				IotSensorViewer.this.sensorLoadingStatuslbl.setText("Loaded!");
				IotSensorViewer.this.sensorLoadingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				IotSensorViewer.this.sensorLoadingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
			}else{
				IotSensorViewer.this.sensorLoadBtn.setText("LOAD");
				IotSensorViewer.this.sensorLoadingStatuslbl.setText("Unloaded!");
				IotSensorViewer.this.sensorLoadingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				IotSensorViewer.this.sensorLoadingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
	}

	//�@Monitor Start�{�^�����������ꂽ���̃C�x���g����
	private class startMonitoringBtnListener implements SelectionListener{
		public startMonitoringBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.monitorStatus){
				IotSensorViewer.this.stopMonitor();
			}else{
				IotSensorViewer.this.startMonitor();
			}
		}
	}

	private void startMonitor(){
		if(!this.monitorStatus){
			this.dataModel.addListener(this);
			this.monitorStatus = true;
			this.startMonitorBtn.setText("STOP MONITOR");
			this.monitorStatuslbl.setText("Now Monitoring!");
			this.monitorStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
			this.monitorStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		}
	}
	private void stopMonitor(){
		if( this.monitorStatus ){
			this.monitorStatus = false;
			this.dataModel.removeListener(this);
			this.startMonitorBtn.setText("START MONITOR");
			this.monitorStatuslbl.setText("Monitor Stopped!");
			this.monitorStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			this.monitorStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}
	
	// Sampling Start�{�^�����������ꂽ�Ƃ��̃C�x���g����
	//�@�v���g�^�C�v�ł́A�C���W�P�[�^�̈ʒu��ω�������B
	private class startSamplingBtnListener implements SelectionListener{
		public startSamplingBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.sensor.getSamplingStatus()){
				IotSensorViewer.this.stopSampling();
			}else{
				IotSensorViewer.this.startSampling();
			}
		}
	}
	private void startSampling(){
		this.sensor.startSampling();
	}
	private void stopSampling(){
		this.sensor.stopSampling();
	}

	// Window �I���{�^���̏���
	public void fin(){
		this.stopSampling();
		this.stopMonitor();
	}

	/*
	 * IotSensorDataModelListener �̃C�x���g���X�i�[
	 */
	public void updateSampleingData(Map<String,Object> arrivalData){
		this.arrivalData = arrivalData;
		// UI�X���b�h�֍X�V�������Ϗ�
		//�@UI�X���b�h�ȊO����͉�ʂ̍X�V�͂ł��Ȃ�
		this.shell.getDisplay().asyncExec(new UpdateDataCmd(this.arrivalData));
	}
	
	// UI�X���b�h�Ŏ��s�����X�V�����̎���
	private class UpdateDataCmd implements Runnable{
		Map<String,Object> addData;
		public UpdateDataCmd(Map<String,Object> addData){
			this.addData = addData;
		}
		public void run(){
			XYDataItem newData = new XYDataItem(Double.valueOf(this.addData.get("No.").toString()),
					 							Double.valueOf(this.addData.get("data").toString()));
			IotSensorViewer.this.sig.add(newData);
			if( IotSensorViewer.this.xscale.getUpperBound() < IotSensorViewer.this.sig.getMaxX() )
			{
				// �擪�f�[�^�̍폜
				if( xscaleRange < IotSensorViewer.this.sig.getItemCount()){
					IotSensorViewer.this.sig.remove(0);
				}
				IotSensorViewer.this.xscale.setRange(IotSensorViewer.this.sig.getMaxX() - xscaleRange,
													 IotSensorViewer.this.sig.getMaxX());
			}
			IotSensorViewer.this.txt.append("No."      + this.addData.get("No.").toString() + ", " +
										 	"data :" + this.addData.get("data").toString() + "\n");
		}
	}
	
	/*
	 * IotSensorlListener �̃C�x���g���X�i�[
	 */
	public void samplingStatusChanged(boolean status){
		this.shell.getDisplay().asyncExec(new UpdateSamplingStatusChagedCmd());
	}
	
	private class UpdateSamplingStatusChagedCmd implements Runnable{
		public UpdateSamplingStatusChagedCmd(){
			
		}
		public void run(){
			if(IotSensorViewer.this.sensor.getSamplingStatus()){
				IotSensorViewer.this.startSampBtn.setText("STOP SENSOR");
				IotSensorViewer.this.samplingStatuslbl.setText("Sensor Started!");
				IotSensorViewer.this.samplingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				IotSensorViewer.this.samplingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
			}else{
				IotSensorViewer.this.startSampBtn.setText("START SENSOR");
				IotSensorViewer.this.samplingStatuslbl.setText("Sensor Stopped!");
				IotSensorViewer.this.samplingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				IotSensorViewer.this.samplingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
	}

	// Sensor ���[�h�E�A�����[�h�{�^��
	private void createLoadSensorButton(){
	    /*
	     * Button Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.sensorLoadBtn = new Button(this.shell, SWT.BORDER);
		if(this.sensor.getLoadingStatus()){
			this.sensorLoadBtn.setText("UNLOAD");
		}else{
			this.sensorLoadBtn.setText("LOAD");
		}
	    this.sensorLoadBtn.setLayoutData(grid);
	    this.sensorLoadBtn.addSelectionListener(new sensorLoadingBtnListener());
	}
	// Sensor ���[�h�E�A�����[�h��ԕ\�����x��
	private void createSensorLoadingStatusLabel(){
	    /*
	     * Label Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.sensorLoadingStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER|SWT.SINGLE);
		if(this.sensor.getLoadingStatus()){
		    this.sensorLoadingStatuslbl.setText("Sensor loaded!");
		}else{
		    this.sensorLoadingStatuslbl.setText("Sensor unloaded!");
		}
	    this.sensorLoadingStatuslbl.setLayoutData(grid);
	}
	
	// �T���v�����O�J�n�E��~�{�^��
	private void createStartSamplingButton(){
	    /*
	     * Button Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.startSampBtn = new Button(this.shell, SWT.BORDER);
		if(this.sensor.getSamplingStatus()){
			this.startSampBtn.setText("STOP SENSOR");
		}else{
			this.startSampBtn.setText("START SENSOR");
		}
	    this.startSampBtn.setLayoutData(grid);
	    this.startSampBtn.addSelectionListener(new startSamplingBtnListener());
	}
	// �T���v�����O��ԕ\�����x��
	private void createSamplingStatusLabel(){
	    /*
	     * Label Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.samplingStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER|SWT.SINGLE);
		if(this.sensor.getSamplingStatus()){
		    this.samplingStatuslbl.setText("Sensor started!");
		}else{
		    this.samplingStatuslbl.setText("Sensor stopped!");
		}
	    this.samplingStatuslbl.setLayoutData(grid);
	}

	// ���j�^�J�n�E��~�{�^��
	private void createStartMonitorButton(){
	    /*
	     * Button Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.startMonitorBtn = new Button(this.shell, SWT.BORDER);
		this.startMonitorBtn.setText("START MONITOR");
	    this.startMonitorBtn.setLayoutData(grid);
	    this.startMonitorBtn.addSelectionListener(new startMonitoringBtnListener());
	}
	// ���j�^��ԕ\�����x��
	private void createMonitoringStatusLabel(){
	    /*
	     * Label Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.monitorStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER);
		if(this.monitorStatus){
		    this.monitorStatuslbl.setText("Monitor stopped!");
		}else{
		    this.monitorStatuslbl.setText("Now Monitoring!");
		}
	    this.monitorStatuslbl.setLayoutData(grid);
	}

	// ���j�^���O�\�����X�g
	private void createTextBox(){
	    /*
	     * TextBox Widget�̃Z�b�g
	     */
	    GridData grid = new GridData();
	    grid.horizontalSpan = 2;
	    grid.heightHint = 240;
	    grid.widthHint=240;

	    // �P��s�e�L�X�g�{�b�N�X���쐬
	    this.txt = new Text(shell,SWT.MULTI|SWT.BORDER|SWT.V_SCROLL);
	    this.txt.setLayoutData(grid);
	}
	
}
