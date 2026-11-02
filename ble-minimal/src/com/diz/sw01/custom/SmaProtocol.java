package com.diz.sw01.custom;
import java.util.*;
/** Wire layout recovered from the original MessageFactory, BleParser and sensor entities. */
public final class SmaProtocol {
 public static final int IDENTITY=0x0301,SESSION=0x0302,HEART_LIVE=0x0224,MEASUREMENT=0x0236,HEART_HISTORY=0x0503,ACTIVITY=0x0502,POWER=0x0203;
 public static final int READ=0x10,UPDATE=0,CREATE=0x20;
 public static final long EPOCH=946684800L;
 public static int u(byte b){return b&255;}
 public static int be16(byte[] a,int p){return (u(a[p])<<8)|u(a[p+1]);}
 public static long be32(byte[] a,int p){return ((long)be16(a,p)<<16)|be16(a,p+2);}
 public static byte[] int32(long n){return new byte[]{(byte)(n>>24),(byte)(n>>16),(byte)(n>>8),(byte)n};}
 // The original BleReadable.timeToMillis treats the wire time as local wall-clock seconds.
 public static long watchMillis(long seconds){Calendar wall=Calendar.getInstance(TimeZone.getTimeZone("UTC"));wall.setTimeInMillis((seconds+EPOCH)*1000);Calendar local=Calendar.getInstance();local.clear();local.set(wall.get(Calendar.YEAR),wall.get(Calendar.MONTH),wall.get(Calendar.DAY_OF_MONTH),wall.get(Calendar.HOUR_OF_DAY),wall.get(Calendar.MINUTE),wall.get(Calendar.SECOND));return local.getTimeInMillis();}
 public static int crc(byte[] a,int start){int c=0;for(int i=start;i<a.length;i++){c^=u(a[i]);for(int b=0;b<8;b++)c=(c&1)!=0?(c>>>1)^0xa001:c>>>1;}return c;}
 public static byte[] frame(int key,int flag,byte[] payload,boolean reply){int n=payload==null?0:payload.length;if(n>65532)throw new IllegalArgumentException("payload too long");byte[] a=new byte[n+9];a[0]=(byte)0xab;a[1]=(byte)(reply?0x11:1);a[2]=(byte)((n+3)>>8);a[3]=(byte)(n+3);a[6]=(byte)(key>>8);a[7]=(byte)key;a[8]=(byte)flag;if(n>0)System.arraycopy(payload,0,a,9,n);int c=crc(a,6);a[4]=(byte)(c>>8);a[5]=(byte)c;return a;}
 public static final class Packet{public final int key,flag;public final boolean reply,nack;public final byte[] data;Packet(byte[] a){key=be16(a,6);flag=u(a[8]);reply=(a[1]&16)!=0;nack=(a[1]&32)!=0;data=Arrays.copyOfRange(a,9,a.length);}}
 public static final class Decoder{
  private byte[] pending=new byte[0];public int rejected;
  public void reset(){pending=new byte[0];rejected=0;}
  public List<Packet> feed(byte[] chunk){List<Packet> out=new ArrayList<>();if(chunk==null||chunk.length==0)return out;if(pending.length+chunk.length>131082){reset();rejected++;return out;}byte[] all=Arrays.copyOf(pending,pending.length+chunk.length);System.arraycopy(chunk,0,all,pending.length,chunk.length);int p=0;
   while(p<all.length){if(u(all[p])!=0xab){p++;rejected++;continue;}if(all.length-p<6)break;int payload=be16(all,p+2),len=payload+6;if(payload<3||(all[p+1]&15)!=1){p++;rejected++;continue;}if(all.length-p<len)break;byte[] a=Arrays.copyOfRange(all,p,p+len);if(crc(a,6)!=be16(a,4)){p++;rejected++;continue;}out.add(new Packet(a));p+=len;}pending=Arrays.copyOfRange(all,p,all.length);return out;
  }
 }
 public static final class Heart{public final long millis;public final int bpm,type;Heart(byte[] d,int p){millis=watchMillis(be32(d,p));bpm=u(d[p+4]);type=u(d[p+5]);}}
 public static List<Heart> hearts(byte[] d){if(d.length%6!=0)throw new IllegalArgumentException("heart record length");List<Heart> out=new ArrayList<>();for(int p=0;p<d.length;p+=6)out.add(new Heart(d,p));return out;}
 public static final class Activity{public final long millis,steps;public final int mode,state;Activity(byte[] d,int p){millis=watchMillis(be32(d,p));mode=u(d[p+4])>>>3;state=u(d[p+4])&7;steps=((long)u(d[p+5])<<16)|be16(d,p+6);}public String id(){return millis+":"+mode+":"+state;}}
 public static List<Activity> activities(byte[] d){if(d.length%16!=0)throw new IllegalArgumentException("activity record length");List<Activity> out=new ArrayList<>();for(int p=0;p<d.length;p+=16)out.add(new Activity(d,p));return out;}
}
