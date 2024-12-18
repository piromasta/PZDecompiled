package zombie.popman;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.util.list.PZArrayUtil;

public final class ZombieStateFlags {
   private int m_flags = 0;

   public ZombieStateFlags() {
   }

   public ZombieStateFlags(int var1) {
      this.m_flags = var1;
   }

   public ZombieStateFlags(ZombieStateFlag... var1) {
      ZombieStateFlag[] var2 = var1;
      int var3 = var1.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ZombieStateFlag var5 = var2[var4];
         this.setFlag(var5);
      }

   }

   public static ZombieStateFlags fromInt(int var0) {
      return new ZombieStateFlags(var0);
   }

   public static int intFromZombie(IsoZombie var0) {
      int var1 = 0;
      var1 = setFlag(var1, ZombieStateFlag.Initialized);
      var1 = setFlag(var1, ZombieStateFlag.Crawling, var0.isCrawling());
      var1 = setFlag(var1, ZombieStateFlag.CanWalk, var0.isCanWalk());
      var1 = setFlag(var1, ZombieStateFlag.FakeDead, var0.isFakeDead());
      var1 = setFlag(var1, ZombieStateFlag.CanCrawlUnderVehicle, var0.isCanCrawlUnderVehicle());
      var1 = setFlag(var1, ZombieStateFlag.ReanimatedForGrappleOnly, var0.isReanimatedForGrappleOnly());
      return var1;
   }

   public static ZombieStateFlags fromZombie(IsoZombie var0) {
      return fromInt(intFromZombie(var0));
   }

   public void setFlag(ZombieStateFlag var1) {
      this.m_flags = setFlag(this.m_flags, var1);
   }

   public void clearFlag(ZombieStateFlag var1) {
      this.m_flags = clearFlag(this.m_flags, var1);
   }

   public void setFlag(ZombieStateFlag var1, boolean var2) {
      this.m_flags = setFlag(this.m_flags, var1, var2);
   }

   public boolean checkFlag(ZombieStateFlag var1) {
      return checkFlag(this.m_flags, var1);
   }

   public static int setFlag(int var0, ZombieStateFlag var1) {
      return var0 | var1.Flag;
   }

   public static int clearFlag(int var0, ZombieStateFlag var1) {
      return var0 & ~var1.Flag;
   }

   public static int setFlag(int var0, ZombieStateFlag var1, boolean var2) {
      return var2 ? setFlag(var0, var1) : clearFlag(var0, var1);
   }

   public static boolean checkFlag(int var0, ZombieStateFlag var1) {
      return (var0 & var1.Flag) != 0;
   }

   public int asInt() {
      return this.m_flags;
   }

   public boolean isInitialized() {
      return this.checkFlag(ZombieStateFlag.Initialized);
   }

   public boolean isCrawling() {
      return this.checkFlag(ZombieStateFlag.Crawling);
   }

   public boolean isCanWalk() {
      return this.checkFlag(ZombieStateFlag.CanWalk);
   }

   public boolean isFakeDead() {
      return this.checkFlag(ZombieStateFlag.FakeDead);
   }

   public boolean isCanCrawlUnderVehicle() {
      return this.checkFlag(ZombieStateFlag.CanCrawlUnderVehicle);
   }

   public boolean isReanimatedForGrappleOnly() {
      return this.checkFlag(ZombieStateFlag.ReanimatedForGrappleOnly);
   }

   public ZombieStateFlag[] toArray() {
      ArrayList var1 = new ArrayList();
      ZombieStateFlag[] var2 = ZombieStateFlag.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ZombieStateFlag var5 = var2[var4];
         if (this.checkFlag(var5)) {
            var1.add(var5);
         }
      }

      return (ZombieStateFlag[])var1.toArray(new ZombieStateFlag[0]);
   }

   public String toString() {
      String var1 = PZArrayUtil.arrayToString((Object[])this.toArray(), Enum::toString, "{ ", " }", ", ");
      String var10000 = this.getClass().getName();
      return var10000 + "{ " + var1 + "}";
   }

   public static String intToString(int var0) {
      return fromInt(var0).toString();
   }
}
