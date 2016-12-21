// Maurice Ribble
// 6-28-2009
// http://www.glacialwanderer.com/hobbyrobotics

// This takes data off the serial port and graphs it.
// There is an option to log this data to a file.

// I wrote an arduino app that sends data in the format expected by this app.
// The arduino app sends accelerometer and gyroscope data.
import processing.serial.*;
import java.io.*;
import static javax.swing.JOptionPane.*;
import java.awt.Toolkit;
import ddf.minim.*;

Minim minim;
AudioPlayer hyah;
AudioPlayer huff;

String lable="LEFT_validate";
int counter=1;
int insNum=16;         //length of sliding window
int ins=1;         //
boolean trainFile=true;         // generates training dataset
boolean logFile=false;         // logs sensordata
boolean record = false;
long stamp=0;

int deltaThreshold = 50;

String portName="";
PrintWriter file,file2;
float minGyro=-15000;
float maxGyro=5000;
float minAccel=-200;
float maxAccel=1000;
// Globals
static final int g_winW=820;           // Window Width
static final int g_winH=600;           // Window Height
boolean g_dumpToFile=true;          // Dumps data to c:\\output.txt in a comma seperated format (easy to import into Excel)
boolean g_enableFilter=true;          // Enables simple filter to help smooth out data.
final boolean debug=false;
//Serial myPort;        // The serial port

cDataArray g_xAccel=new cDataArray(200);
cDataArray g_yAccel=new cDataArray(200);
cDataArray g_zAccel=new cDataArray(200);
cDataArray g_vRef=new cDataArray(200);
cDataArray g_xRate=new cDataArray(200);
cDataArray g_yRate=new cDataArray(200);
cGraph g_graph=new cGraph(10,190,800,400);
Serial g_serial;
PFont g_font;

void setup()
{
        size(820,600,P2D);
        minim = new Minim(this);
        hyah = minim.loadFile("hyah.wav");
        huff = minim.loadFile("huff.wav");
        
        if(trainFile) {
                file=createWriter(lable+"_train.csv"); //bool tells to append
                file2=createWriter(lable+"_train_plot.csv");
                file.print("Lable,"); //write the file header
                for(int i=1; i<=insNum; i=i+1) {
                        file.print("AccX"+i+",AccY"+i+",AccZ"+i+",GyrX"+i+",GyrY"+i+",GyrZ"+i+","); //write the file header
                }
                file2.print("#,AccX,AccY,AccZ,GyrX,GyrY,GyrZ\r\n"); //write the file header
                file.flush();
                file2.flush();
        }
        if(logFile) {
                file=createWriter("countingTime.csv"); //bool tells to append
                file.print("#,AccX,AccY,AccZ,GyrX,GyrY,GyrZ\r\n"); //write the file header
                file.flush();

                file2=createWriter("log_filtered.csv"); //bool tells to append
                file2.print("#,AccX,AccY,AccZ,GyrX,GyrY,GyrZ\r\n"); //write the file header
                file2.flush();
        }

        String mypp=port();
        println(Serial.list());
        g_serial=new Serial(this,mypp,9600,'N',8,1.0);
        g_font=loadFont("ArialMT-20.vlw");
        textFont(g_font,20);

        // This draws the graph key info
        strokeWeight(1.5);
        stroke(255,0,0);
        line(20,420,35,420);
        stroke(0,255,0);
        line(20,440,35,440);
        stroke(0,0,255);
        line(20,460,35,460);
        stroke(255,255,0);
        line(20,480,35,480);
        stroke(255,0,255);
        line(20,500,35,500);
        stroke(0,255,255);
        line(20,520,35,520);
        fill(0,0,0);
        text("xAccel",40,430);
        text("yAccel",40,450);
        text("zAccel",40,470);
        text("xGyro",40,490);
        text("yGyro",40,510);
        text("zGyro",40,530);

}

void draw()
{
        // We need to read in all the avilable data so graphing doesn't lag behind
        while(g_serial.available()>=18)
        {
                processSerialData();
        }

        strokeWeight(1);
        fill(255,255,255);
        g_graph.drawGraphBox();

        strokeWeight(1.5);
        stroke(255,0,0);
        g_graph.drawLine(g_xAccel,minAccel,maxAccel);
        stroke(0,255,0);
        g_graph.drawLine(g_yAccel,minAccel,maxAccel);
        stroke(0,0,255);
        g_graph.drawLine(g_zAccel,minAccel,maxAccel);
        stroke(0,0,0);
        g_graph.drawLine(g_vRef,minGyro,maxGyro);
        stroke(255,0,255);
        g_graph.drawLine(g_xRate,minGyro,maxGyro);
        stroke(0,255,255);
        g_graph.drawLine(g_yRate,minGyro,maxGyro);
}

// This reads in one set of the data from the serial port
void processSerialData(){
        if(g_serial.available()>=18) {
                String inputString=g_serial.readStringUntil('\n');
                //print(inputString);
                if(inputString!=null&&inputString.length()>0) {
                        String[] inputStringArr=split(inputString,",");
                        if(inputStringArr.length>=7&&inputStringArr[0].equals("h")) { //  we have 7 elements
                                int xAccel=int(inputStringArr[1]);
                                int yAccel=int(inputStringArr[2]);
                                int zAccel=int(inputStringArr[3]);
                                int vRef=int(inputStringArr[4]);
                                int xRate=int(inputStringArr[5]);
                                int yRate=int(inputStringArr[6]);

                                g_xAccel.addVal(xAccel);
                                g_yAccel.addVal(yAccel);
                                g_zAccel.addVal(zAccel);
                                g_vRef.addVal(vRef);
                                g_xRate.addVal(xRate);
                                g_yRate.addVal(yRate);
                                
                                if(logFile) {
                                        file.print(stamp+","+xAccel+","+yAccel+","+zAccel+","+vRef+","+xRate+","+yRate+"\r\n");
                                        file.flush();

                                        file2.print(stamp+","+g_xAccel.delta+","+g_yAccel.delta+","+g_zAccel.delta+","+g_vRef.delta+","+g_xRate.delta+","+g_yRate.delta+"\r\n");
                                        file2.flush();
                                        stamp++;
                                }
                                if(trainFile && record) {
                                  
                                  if(counter < insNum) {
                                    counter++;
                                    
                                  } else {
                                    /*
                                    int accel_max = Math.max(Math.max(g_xAccel.record_max, g_yAccel.record_max), g_zAccel.record_max);
                                    int accel_min = Math.min(Math.min(g_xAccel.record_min, g_yAccel.record_min), g_zAccel.record_min);
                                    
                                    int gyro_max = Math.max(Math.max(g_vRef.record_max, g_xRate.record_max), g_yRate.record_max);
                                    int gyro_min = Math.min(Math.min(g_vRef.record_min, g_xRate.record_min), g_yRate.record_min);
                                    
                                    float[] xAccelNorm = normalize(accel_min, accel_max, g_xAccel.getRecording());
                                    float[] yAccelNorm = normalize(accel_min, accel_max, g_yAccel.getRecording());
                                    float[] zAccelNorm = normalize(accel_min, accel_max, g_zAccel.getRecording());
                                    
                                    float[] vRefNorm = normalize(gyro_min, gyro_max, g_vRef.getRecording());
                                    float[] xRateNorm = normalize(gyro_min, gyro_max, g_xRate.getRecording());
                                    float[] yRateNorm = normalize(gyro_min, gyro_max, g_yRate.getRecording());
                                    */

                                    float[] rec_xAcc = g_xAccel.getRecording();
                                    float[] rec_yAcc = g_yAccel.getRecording();
                                    float[] rec_zAcc = g_zAccel.getRecording();

                                    float[] rec_vRef = g_vRef.getRecording();
                                    float[] rec_xRate = g_xRate.getRecording();
                                    float[] rec_yRate = g_yRate.getRecording();
                                    
                                    StringBuilder sb = new StringBuilder();
                                    StringBuilder sb2 = new StringBuilder();
                                    
                                    sb.append("\r\n" + lable);
                                    
                                    for(int i = 0; i < insNum; i++) {
                                      sb.append("," + rec_xAcc[i] + "," + rec_yAcc[i] + "," + rec_zAcc[i] + 
                                                 "," + rec_vRef[i] + "," + rec_xRate[i] + "," + rec_yRate[i]);
                                                 
                                      sb2.append(stamp + "," + rec_xAcc[i] + "," + rec_yAcc[i] + "," + rec_zAcc[i] + 
                                                 "," + rec_vRef[i] + "," + rec_xRate[i] + "," + rec_yRate[i] + "\r\n");
                                      stamp++;
                                    }
                                    counter=0;
                                    record= false;
                                    fill(0,0,0);
                                    background(102);
                                    text("Gesture Type:"+lable+",  Gesture Number:"+ins,400,530);
                                    file.print(sb);
                                    file.flush();
                                    file2.print(sb2);
                                    file2.flush();
                                    ins++;
                                    
                                    huff.rewind();
                                    huff.play();
                                  }
                                }
                                
                                  if(!record &&(Math.abs(g_yAccel.delta) > deltaThreshold ||
                                    		Math.abs(g_xAccel.delta) > deltaThreshold ||
                                    		Math.abs(g_zAccel.delta) > deltaThreshold)) {
                                      
                                      g_xAccel.startRecording();
                                      g_yAccel.startRecording();
                                      g_zAccel.startRecording();
                                      g_vRef.startRecording();
                                      g_xRate.startRecording();
                                      g_yRate.startRecording();
                                      
                                      record = true;
                                      hyah.rewind();
                                      hyah.play();
                                   }
                        }
                }
        }
}

float[] normalize(int offset, int max, float[] data) {
  int span = max - offset;
  float[] returnVals = new float[data.length];
  
  for(int i = 0; i < data.length; i++) {
    returnVals[i] = (float) (data[i] - offset) / (float) span;
  }
  return returnVals;
}

// This class helps mangage the arrays of data I need to keep around for graphing.
class cDataArray {
float[] m_data;
float[] prev_data;
float[] record_data;
int record_index = 0;
int m_maxSize;
int m_startIndex = 0;
int m_endIndex = 0;
int m_curSize;
int windowSize = 3;
int fv = 0;        //latest filtered value
int pfv = 0;       //previous filtered value 
int delta = 0;

int record_max = Integer.MIN_VALUE;
int record_min = Integer.MAX_VALUE;

boolean record = false;

cDataArray(int maxSize) {
        m_maxSize = maxSize;    //200
        m_data = new float[maxSize];
        prev_data = new float[windowSize];
        record_data = new float[insNum];
}

void addVal(float val) {

        if (g_enableFilter && (m_curSize != 0)) {
                for (int i = windowSize - 1; i >= 1; i--) {
                        prev_data[i] = prev_data[i - 1];
                }
                prev_data[0] = val;

                float sum = 0;

                for (int j = 0; j < windowSize; j++) {
                        sum += prev_data[j];
                }
                
                float filteredVal = (sum / windowSize);
                pfv = fv;
                fv = (int) filteredVal;
                delta = fv - pfv;
                m_data[m_endIndex] = delta;
        } else {
        		pfv = fv;
        		fv = (int)val;
        		delta = fv - pfv;
                m_data[m_endIndex] = delta;
        }
        if(this.record) {
          if(record_index < insNum){
            record_max = Math.max(record_max, (int)m_data[m_endIndex]);
            record_min = Math.min(record_min, (int)m_data[m_endIndex]);
            record_data[record_index] = m_data[m_endIndex];
            record_index++;
          } else {
            this.record = false;
          }
        }
        m_endIndex = (m_endIndex + 1) % m_maxSize;
        if (m_curSize == m_maxSize) {
                m_startIndex = (m_startIndex + 1) % m_maxSize;
        } else {
                m_curSize++;
        }
}

float getVal(int index) {
        return m_data[(m_startIndex + index) % m_maxSize];
}

int getCurSize() {
        return m_curSize;
}

int getMaxSize() {
        return m_maxSize;
}

void startRecording(){
  record_index = 0;
  record_max = Integer.MIN_VALUE;
  record_min = Integer.MAX_VALUE;
  this.record = true;
}

float[] getRecording() {
  return record_data;
}
}

// This class takes the data and helps graph it
class cGraph {
float m_gWidth, m_gHeight;
float m_gLeft, m_gBottom, m_gRight, m_gTop;

cGraph(float x, float y, float w, float h) {
        m_gWidth = w;
        m_gHeight = h;
        m_gLeft = x;
        m_gBottom = g_winH - y;
        m_gRight = x + w;
        m_gTop = g_winH - y - h;
}

void drawGraphBox() {
        stroke(0, 0, 0);
        rectMode(CORNERS);
        rect(m_gLeft, m_gBottom, m_gRight, m_gTop);
}

void drawLine(cDataArray data, float minRange, float maxRange) {
        float graphMultX = m_gWidth / data.getMaxSize();
        float graphMultY = m_gHeight / (maxRange - minRange);

        for (int i = 0; i < data.getCurSize() - 1; ++i) {
                float x0 = i * graphMultX + m_gLeft;
                float y0 = m_gBottom - ((data.getVal(i) - minRange) * graphMultY);
                float x1 = (i + 1) * graphMultX + m_gLeft;
                float y1 = m_gBottom - ((data.getVal(i + 1) - minRange) * graphMultY);
                line(x0, y0, x1, y1);
        }
}
}

String port() {
        String COMx, COMlist = "";
        /*
           Other setup code goes here - I put this at
           the end because of the try/catch structure.
         */
        try {
                if (debug) printArray(Serial.list());
                int i = Serial.list().length;
                if (i != 0) {
                        if (i >= 2) {
                                // need to check which port the inst uses -
                                // for now we'll just let the user decide
                                for (int j = 0; j < i; ) {
                                        COMlist += char(j + 'a') + " = " + Serial.list()[j];
                                        if (++j < i) COMlist += "\n";
                                }
                                COMx = showInputDialog("Which COM port is correct? (a,b,..):\n" + COMlist);
                                if (COMx == null) exit();
                                if (COMx.isEmpty()) exit();
                                i = int(COMx.toLowerCase().charAt(0) - 'a') + 1;
                        }
                        portName = Serial.list()[i - 1];
                        if (debug) println(portName);
                        return portName;
                } else {
                        showMessageDialog(frame, "Device is not connected to the PC");
                        exit();
                }
        } catch (Exception e) { //Print the type of error
                showMessageDialog(frame, "COM port is not available (may\nbe in use by another program)");
                println("Error:", e);
                exit();
        }
        return portName;
}

void mousePressed() {
        if(file != null) file.close();
        if(file2 != null) file2.close();
        Runtime rt = Runtime.getRuntime();
        try {
                rt.exec("gnome-terminal --working-directory=IOTAP-android/data_processing/imu_lab --execute python plot_log_filtered.py " + lable + "_train_plot.csv");
        } catch(Exception s) {s.printStackTrace();}
        exit();
        
}
