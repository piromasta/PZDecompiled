package zombie.basements;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;

public class BasementsV1 {
   public BasementsV1() {
   }

   public void addAccessDefinitions(String var1, KahluaTable var2) {
      BasementsPerMap var3 = Basements.getInstance().getOrCreatePerMap(var1);
      KahluaTableIterator var4 = var2.iterator();

      while(var4.advance()) {
         String var5 = (String)var4.getKey();
         KahluaTable var6 = (KahluaTable)var4.getValue();
         int var7 = ((Double)var6.rawget("width")).intValue();
         int var8 = ((Double)var6.rawget("height")).intValue();
         int var9 = ((Double)var6.rawget("stairx")).intValue();
         int var10 = ((Double)var6.rawget("stairy")).intValue();
         boolean var11 = "N".equals(var6.rawget("stairDir"));
         BasementDefinition var12 = new BasementDefinition();
         var12.width = var7;
         var12.height = var8;
         var12.stairx = var9;
         var12.stairy = var10;
         var12.north = var11;
         var12.name = var5;
         var3.basementAccessDefinitions.add(var12);
         var3.basementAccessDefinitionByName.put(var12.name, var12);
      }

   }

   public void addBasementDefinitions(String var1, KahluaTable var2) {
      BasementsPerMap var3 = Basements.getInstance().getOrCreatePerMap(var1);
      KahluaTableIterator var4 = var2.iterator();

      while(var4.advance()) {
         String var5 = (String)var4.getKey();
         KahluaTable var6 = (KahluaTable)var4.getValue();
         int var7 = ((Double)var6.rawget("width")).intValue();
         int var8 = ((Double)var6.rawget("height")).intValue();
         int var9 = ((Double)var6.rawget("stairx")).intValue();
         int var10 = ((Double)var6.rawget("stairy")).intValue();
         boolean var11 = "N".equals(var6.rawget("stairDir"));
         BasementDefinition var12 = new BasementDefinition();
         var12.width = var7;
         var12.height = var8;
         var12.stairx = var9;
         var12.stairy = var10;
         var12.north = var11;
         var12.name = var5;
         var3.basementDefinitions.add(var12);
         var3.basementDefinitionByName.put(var12.name, var12);
      }

   }

   public void addSpawnLocations(String var1, KahluaTable var2) {
      BasementsPerMap var3 = Basements.getInstance().getOrCreatePerMap(var1);
      KahluaTableIterator var4 = var2.iterator();

      while(var4.advance()) {
         KahluaTable var5 = (KahluaTable)var4.getValue();
         int var6 = ((Double)var5.rawget("x")).intValue();
         int var7 = ((Double)var5.rawget("y")).intValue();
         boolean var8 = "N".equals((String)var5.rawget("stairDir"));
         BasementSpawnLocation var9 = new BasementSpawnLocation();
         var9.x = var6;
         var9.y = var7;
         if (var5.rawget("z") instanceof Double) {
            var9.z = ((Double)var5.rawget("z")).intValue();
         }

         var9.w = 1;
         var9.h = 1;
         var9.stairX = 0;
         var9.stairY = 0;
         var9.north = var8;
         Object var10 = var5.rawget("choices");
         if (var10 instanceof KahluaTable) {
            var9.specificBasement = new ArrayList();
            KahluaTableIterator var11 = ((KahluaTable)var10).iterator();

            while(var11.advance()) {
               Object var12 = var11.getValue();
               if (var12 instanceof String) {
                  String var13 = ((String)var12).trim();
                  if (!var13.isEmpty()) {
                     var9.specificBasement.add(var13);
                  }
               }
            }
         }

         var9.access = var5.getString("access");
         var3.basementSpawnLocations.add(var9);
      }

   }

   public BasementSpawnLocation registerBasementSpawnLocation(String var1, String var2, String var3, int var4, int var5, int var6, int var7, int var8, KahluaTable var9) {
      BasementsPerMap var10 = Basements.getInstance().getOrCreatePerMap(var1);
      BasementSpawnLocation var11 = new BasementSpawnLocation();
      var11.x = var4;
      var11.y = var5;
      var11.z = var6;
      var11.w = var7;
      var11.h = var8;
      var11.north = true;
      var11.stairX = 0;
      var11.stairY = 0;
      if (var9 != null) {
         if (var9.rawget("StairX") instanceof Double) {
            var11.stairX = ((KahluaTableImpl)var9).rawgetInt("StairX");
         }

         if (var9.rawget("StairY") instanceof Double) {
            var11.stairY = ((KahluaTableImpl)var9).rawgetInt("StairY");
         }

         if (var9.rawget("StairDirection") instanceof String) {
            var11.north = "N".equals(var9.rawget("StairDirection"));
         }

         if (var9.rawget("Access") instanceof String) {
            var11.access = var9.getString("Access");
         }
      }

      var10.basementSpawnLocations.add(var11);
      return var11;
   }
}
