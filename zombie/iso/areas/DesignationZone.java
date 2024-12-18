package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

public class DesignationZone {
   public Double id = 0.0;
   public int hourLastSeen = 0;
   public int lastActionTimestamp = 0;
   public String name;
   public String type;
   public int x;
   public int y;
   public int z;
   public int w;
   public int h;
   public boolean streamed = true;
   public static long lastUpdate = 0L;
   public static final ArrayList<DesignationZone> allZones = new ArrayList();

   public static DesignationZone addZone(String var0, String var1, int var2, int var3, int var4, int var5, int var6) {
      return new DesignationZone(var0, var1, var2, var3, var4, var5, var6);
   }

   public DesignationZone() {
   }

   public DesignationZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      int var8;
      if (var3 > var6) {
         var8 = var6;
         var6 = var3;
         var3 = var8;
      }

      if (var4 > var7) {
         var8 = var7;
         var7 = var4;
         var4 = var8;
      }

      this.id = (double)Rand.Next(9999999) + 100000.0;
      this.type = var1;
      if (StringUtils.isNullOrEmpty(var2)) {
         var2 = "";
      }

      this.name = var2;
      this.x = var3;
      this.y = var4;
      this.z = var5;
      this.w = var6 - var3;
      this.h = var7 - var4;
      if (this.getZoneById(this.id) == null) {
         allZones.add(this);
      }

   }

   public void doMeta(int var1) {
   }

   public boolean isStillStreamed() {
      IsoGridSquare var1 = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
      IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w, this.y + this.h, this.z);
      return var1 != null || var2 != null;
   }

   public static void removeZone(String var0, String var1) {
      DesignationZone var2 = getZoneByNameAndType(var0, var1);
      if (var2 != null) {
         allZones.remove(var2);
      }

   }

   public static void removeZone(DesignationZone var0) {
      if (var0 != null) {
         allZones.remove(var0);
      }

   }

   public static DesignationZone getZoneByName(String var0) {
      for(int var1 = 0; var1 < allZones.size(); ++var1) {
         DesignationZone var2 = (DesignationZone)allZones.get(var1);
         if (var2.name.equals(var0)) {
            return var2;
         }
      }

      return null;
   }

   public static DesignationZone getZoneByNameAndType(String var0, String var1) {
      for(int var2 = 0; var2 < allZones.size(); ++var2) {
         DesignationZone var3 = (DesignationZone)allZones.get(var2);
         if (var3.name.equals(var1) && var3.type.equals(var0)) {
            return var3;
         }
      }

      return null;
   }

   public static DesignationZone getZone(int var0, int var1, int var2) {
      for(int var3 = 0; var3 < allZones.size(); ++var3) {
         DesignationZone var4 = (DesignationZone)allZones.get(var3);
         if (var0 >= var4.x && var0 < var4.x + var4.w && var1 >= var4.y && var1 < var4.y + var4.h && var4.z == var2) {
            return var4;
         }
      }

      return null;
   }

   public static DesignationZone getZoneByType(String var0, int var1, int var2, int var3) {
      for(int var4 = 0; var4 < allZones.size(); ++var4) {
         DesignationZone var5 = (DesignationZone)allZones.get(var4);
         if (var1 >= var5.x && var1 < var5.x + var5.w && var2 >= var5.y && var2 < var5.y + var5.h && var5.z == var3 && var0.equals(var5.type)) {
            return var5;
         }
      }

      return null;
   }

   public boolean isFullyStreamed() {
      if (IsoWorld.instance.getCell() == null) {
         return false;
      } else {
         IsoGridSquare var1 = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
         IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w, this.y + this.h, this.z);
         return var1 != null && var2 != null;
      }
   }

   public DesignationZone getZoneById(Double var1) {
      for(int var2 = 0; var2 < allZones.size(); ++var2) {
         if (((DesignationZone)allZones.get(var2)).id == var1) {
            return (DesignationZone)allZones.get(var2);
         }
      }

      return null;
   }

   public void unloading() {
      if (!this.isStillStreamed() && this.streamed) {
         this.streamed = false;
         this.hourLastSeen = (int)GameTime.getInstance().getWorldAgeHours();
      }

   }

   public void loading() {
      if (this.isFullyStreamed() && !this.streamed) {
         this.streamed = true;
         this.doMeta((int)GameTime.getInstance().getWorldAgeHours() - this.hourLastSeen);
      }

   }

   private void checkStreamed() {
      IsoGridSquare var1 = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
      IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w, this.y + this.h, this.z);
      if (var1 != null && var2 != null && !this.streamed) {
         this.streamed = true;
         this.doMeta((int)GameTime.getInstance().getWorldAgeHours() - this.hourLastSeen);
      }

      if ((var1 == null || var2 == null) && this.streamed) {
         this.streamed = false;
         this.hourLastSeen = (int)GameTime.getInstance().getWorldAgeHours();
      }

   }

   public IsoGridSquare getRandomSquare() {
      return IsoWorld.instance.getCell().getGridSquare(Rand.Next(this.x + 1, this.x + this.w - 1), Rand.Next(this.y + 1, this.y + this.h - 1), this.z);
   }

   public IsoGridSquare getRandomFreeSquare() {
      for(int var1 = 0; var1 < 1000; ++var1) {
         IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(Rand.Next(this.x + 1, this.x + this.w - 1), Rand.Next(this.y + 1, this.y + this.h - 1), this.z);
         if (var2.isFree(true)) {
            return var2;
         }
      }

      return IsoWorld.instance.getCell().getGridSquare(Rand.Next(this.x + 1, this.x - this.w - 1), Rand.Next(this.y + 1, this.y - this.h - 1), this.z);
   }

   public static ArrayList<DesignationZone> getAllZonesByType(String var0) {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < allZones.size(); ++var2) {
         DesignationZone var3 = (DesignationZone)allZones.get(var2);
         if (var0.equals(var3.type)) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public static void update() {
      if (GameTime.getInstance().getCalender().getTimeInMillis() > lastUpdate + 20000L) {
         lastUpdate = GameTime.getInstance().getCalender().getTimeInMillis();

         for(int var0 = 0; var0 < allZones.size(); ++var0) {
            ((DesignationZone)allZones.get(var0)).checkStreamed();
            ((DesignationZone)allZones.get(var0)).check();
         }
      }

   }

   public void check() {
   }

   public int getW() {
      return this.w;
   }

   public int getH() {
      return this.h;
   }

   public void save(ByteBuffer var1) {
      var1.putDouble(this.id);
      var1.putInt(this.x);
      var1.putInt(this.y);
      var1.putInt(this.z);
      var1.putInt(this.h);
      var1.putInt(this.w);
      GameWindow.WriteString(var1, this.type);
      GameWindow.WriteString(var1, this.name);
      var1.putInt(this.hourLastSeen);
   }

   public static void load(ByteBuffer var0, int var1) {
      double var2 = var0.getDouble();
      int var4 = var0.getInt();
      int var5 = var0.getInt();
      int var6 = var0.getInt();
      int var7 = var0.getInt();
      int var8 = var0.getInt();
      String var9 = GameWindow.ReadString(var0);
      String var10 = GameWindow.ReadString(var0);
      int var11 = var0.getInt();
      if (var9.equals("AnimalZone")) {
         DesignationZoneAnimal var12 = new DesignationZoneAnimal(var10, var4, var5, var6, var4 + var8, var5 + var7);
         var12.id = var2;
         var12.hourLastSeen = var11;
      }

   }

   public static void Reset() {
      allZones.clear();
   }

   public Double getId() {
      return this.id;
   }
}
