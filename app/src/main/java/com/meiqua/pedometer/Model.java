package com.meiqua.pedometer;

/**
 * Created by Administrator on 2015/11/29.
 */
public class Model {
   private int aX;
    private int aY;
    private int aZ;
    private int counter;

    @Override
    public String toString() {
        return
                "aZ=" + aZ
                ;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }



    public Model() {
    }

    public void parseData(String str,int counter){
        if(str.contains("aworld"))
        {

            try {
                this.counter=counter;
                int Xstart=str.indexOf("x");
                int Xend=str.lastIndexOf("x");
                String Xstring=str.substring(Xstart+1,Xend);
                aX=Integer.parseInt(Xstring);

                int Ystart=str.indexOf("y");
                int Yend=str.lastIndexOf("y");
                String Ystring=str.substring(Ystart+1,Yend);
                aY=Integer.parseInt(Ystring);
                int Zstart=str.indexOf("z");
                int Zend=str.lastIndexOf("z");
                String Zstring=str.substring(Zstart+1,Zend);
                aZ=Integer.parseInt(Zstring);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public int getaX() {
        return aX;
    }

    public void setaX(int aX) {
        this.aX = aX;
    }

    public int getaY() {
        return aY;
    }

    public void setaY(int aY) {
        this.aY = aY;
    }

    public int getaZ() {
        return aZ;
    }

    public void setaZ(int aZ) {
        this.aZ = aZ;
    }
}
