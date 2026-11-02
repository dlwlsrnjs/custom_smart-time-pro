package com.diz.sw01.custom;
import java.util.*;
public final class ProtocolTest {
 static void check(boolean v,String name){if(!v)throw new AssertionError(name);}
 static byte[] hex(String s){s=s.replace(" ","");byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
 public static void main(String[] args)throws Exception{
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  check(SmaProtocol.crc("123456789".getBytes("US-ASCII"),0)==0xbb3d,"CRC16 ARC known check value");
  // Sensor entity fixtures use big endian 32-bit seconds since 2000, followed by fields.
  byte[] h=hex("00000001 48 02");SmaProtocol.Heart heart=SmaProtocol.hearts(h).get(0);
  check(heart.bpm==72&&heart.type==2&&heart.millis==946684801000L,"heart layout epoch and units");
  byte[] a=hex("00000002 3A 010203 00000000 00000000");SmaProtocol.Activity act=SmaProtocol.activities(a).get(0);
  check(act.steps==66051&&act.mode==7&&act.state==2&&act.millis==946684802000L,"activity unsigned24 and bit fields");
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));check(SmaProtocol.watchMillis(1)==946652401000L,"Korean local watch clock must not add nine hours");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  byte[] frame=SmaProtocol.frame(0x0503,0x10,h,true);
  for(int cut=0;cut<=frame.length;cut++){SmaProtocol.Decoder d=new SmaProtocol.Decoder();List<SmaProtocol.Packet> packets=new ArrayList<>();packets.addAll(d.feed(Arrays.copyOfRange(frame,0,cut)));packets.addAll(d.feed(Arrays.copyOfRange(frame,cut,frame.length)));check(packets.size()==1&&packets.get(0).key==0x0503&&packets.get(0).reply&&Arrays.equals(packets.get(0).data,h),"all fragment boundaries "+cut);}
  SmaProtocol.Decoder d=new SmaProtocol.Decoder();byte[] joined=new byte[frame.length*2];System.arraycopy(frame,0,joined,0,frame.length);System.arraycopy(frame,0,joined,frame.length,frame.length);check(d.feed(joined).size()==2,"coalesced packets");
  byte[] bad=frame.clone();bad[10]^=1;check(new SmaProtocol.Decoder().feed(bad).isEmpty(),"CRC corruption never displayed");
  byte[] noise=new byte[frame.length+3];noise[0]=7;noise[1]=8;noise[2]=9;System.arraycopy(frame,0,noise,3,frame.length);check(new SmaProtocol.Decoder().feed(noise).size()==1,"resync noise");
  boolean rejected=false;try{SmaProtocol.hearts(new byte[5]);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"reject incomplete heart record");
  rejected=false;try{SmaProtocol.activities(new byte[15]);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"reject incomplete activity record");
  System.out.println("PASS CRC check vector, sensor layouts, every fragment split, coalescing, corruption and malformed lengths");
 }
}
