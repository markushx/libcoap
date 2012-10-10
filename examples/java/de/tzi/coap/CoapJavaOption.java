package de.tzi.coap08;
public class CoapJavaOption
{
  private short optionType;
  private String optionValue;
  private int optionLength;
  
    public CoapJavaOption(int ot, String val, int l) {
        this.optionType = (short) ot;
        this.optionValue = val;
        this.optionLength = l;
    }

    public short getType(){
      return optionType;
    }

    public String getValue(){
      return optionValue;
    }

    public int getLength(){
      return optionLength;
    }
}
