package zombie.characters;

import java.io.IOException;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import zombie.DummySoundManager;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.iso.IsoCell;
import zombie.network.ServerMap;

public class ZombieIDMapTest extends Assert {
   HashSet<Short> IDs = new HashSet();
   IsoCell cell = new IsoCell(300, 300);

   public ZombieIDMapTest() {
   }

   @BeforeClass
   public static void beforeAll() {
      try {
         RandStandard.INSTANCE.init();
         RandLua.INSTANCE.init();
         ZomboidFileSystem.instance.init();
         LuaManager.init();
      } catch (IOException var1) {
         var1.printStackTrace();
      }

   }

   @Test
   public void test10Allocations() {
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      this.IDs.clear();
      byte var1 = 10;

      for(short var2 = 0; var2 < var1; ++var2) {
         short var3 = ServerMap.instance.getUniqueZombieId();
         System.out.println("id:" + var3);
      }

   }

   @Test
   public void test32653Allocations() {
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      this.IDs.clear();
      char var1 = '蝝';
      long var2 = System.nanoTime();

      for(int var4 = 0; var4 < var1; ++var4) {
         short var5 = ServerMap.instance.getUniqueZombieId();
         assertFalse(this.IDs.contains(var5));
         this.IDs.add(var5);
      }

      long var7 = System.nanoTime();
      float var6 = (float)(var7 - var2) / 1000000.0F;
      System.out.println("time:" + var6);
      System.out.println("time per task:" + var6 / (float)var1);
   }

   @Test
   public void test32653Adds() {
      SoundManager.instance = new DummySoundManager();
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      SurvivorFactory.addMaleForename("Bob");
      SurvivorFactory.addFemaleForename("Kate");
      SurvivorFactory.addSurname("Testova");
      this.IDs.clear();
      short var1 = 32653;
      long var2 = System.nanoTime();

      for(short var4 = 0; var4 < var1; ++var4) {
         short var5 = ServerMap.instance.getUniqueZombieId();
         assertNull(ServerMap.instance.ZombieMap.get(var5));
         assertFalse(this.IDs.contains(var5));
         IsoZombie var6 = new IsoZombie(this.cell);
         var6.OnlineID = var5;
         ServerMap.instance.ZombieMap.put(var5, var6);
         assertEquals((long)var5, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var5)).OnlineID);
         this.IDs.add(var5);
      }

      long var8 = System.nanoTime();
      float var7 = (float)(var8 - var2) / 1000000.0F;
      System.out.println("time:" + var7);
      System.out.println("time per task:" + var7 / (float)var1);
   }

   @Test
   public void test32653Process() {
      RandStandard.INSTANCE.init();
      RandLua.INSTANCE.init();
      ServerMap.instance = new ServerMap();
      SoundManager.instance = new DummySoundManager();
      SurvivorFactory.addMaleForename("Bob");
      SurvivorFactory.addFemaleForename("Kate");
      SurvivorFactory.addSurname("Testova");
      this.IDs.clear();
      short var1 = 32653;
      long var2 = System.nanoTime();

      for(short var4 = 0; var4 < var1; ++var4) {
         assertNull(ServerMap.instance.ZombieMap.get(var4));
         IsoZombie var5 = new IsoZombie(this.cell);
         var5.OnlineID = var4;
         ServerMap.instance.ZombieMap.put(var4, var5);
         assertEquals((long)var4, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var4)).OnlineID);
      }

      long var19 = System.nanoTime();

      for(short var6 = 0; var6 < var1; ++var6) {
         assertEquals((long)var6, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var6)).OnlineID);
         ServerMap.instance.ZombieMap.remove(var6);
         assertNull(ServerMap.instance.ZombieMap.get(var6));
      }

      long var20 = System.nanoTime();

      for(short var8 = 0; var8 < var1; ++var8) {
         assertNull(ServerMap.instance.ZombieMap.get(var8));
         IsoZombie var9 = new IsoZombie(this.cell);
         var9.OnlineID = var8;
         ServerMap.instance.ZombieMap.put(var8, var9);
         assertEquals((long)var8, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var8)).OnlineID);
      }

      long var21 = System.nanoTime();

      for(short var10 = 0; var10 < var1; ++var10) {
         assertEquals((long)var10, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var10)).OnlineID);
         ServerMap.instance.ZombieMap.remove(var10);
         assertNull(ServerMap.instance.ZombieMap.get(var10));
      }

      long var22 = System.nanoTime();

      for(short var12 = 0; var12 < var1; ++var12) {
         assertNull(ServerMap.instance.ZombieMap.get(var12));
         IsoZombie var13 = new IsoZombie(this.cell);
         var13.OnlineID = var12;
         ServerMap.instance.ZombieMap.put(var12, var13);
         assertEquals((long)var12, (long)((IsoZombie)ServerMap.instance.ZombieMap.get(var12)).OnlineID);
      }

      long var23 = System.nanoTime();
      float var14 = (float)(var19 - var2) / 1000000.0F;
      float var15 = (float)(var20 - var19) / 1000000.0F;
      float var16 = (float)(var21 - var20) / 1000000.0F;
      float var17 = (float)(var22 - var21) / 1000000.0F;
      float var18 = (float)(var23 - var22) / 1000000.0F;
      System.out.println("time1:" + var14);
      System.out.println("time2:" + var15);
      System.out.println("time3:" + var16);
      System.out.println("time4:" + var17);
      System.out.println("time5:" + var18);
   }
}
