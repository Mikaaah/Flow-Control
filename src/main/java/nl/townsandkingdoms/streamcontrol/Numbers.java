package nl.townsandkingdoms.streamcontrol;
public final class Numbers {
 public static int clamp(int n,int lo,int hi){return Math.max(lo,Math.min(hi,n));}
 public static double clamp(double n,double lo,double hi){return Math.max(lo,Math.min(hi,n));}
}
