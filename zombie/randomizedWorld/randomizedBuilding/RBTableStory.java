package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public final class RBTableStory extends RandomizedBuildingBase {
   public static ArrayList<StoryDef> allStories = new ArrayList();
   private float xOffset = 0.0F;
   private float yOffset = 0.0F;
   private IsoGridSquare currentSquare = null;
   public ArrayList<HashMap<String, Integer>> fullTableMap = new ArrayList();
   public IsoObject table1 = null;
   public IsoObject table2 = null;

   public RBTableStory() {
   }

   public void initStories() {
      if (allStories.isEmpty()) {
         ArrayList var1 = new ArrayList();
         var1.add("livingroom");
         var1.add("kitchen");
         ArrayList var2 = new ArrayList();
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("BakingPan", 50);
         var3.put("CakePrep", 50);
         var2.add(new StorySpawnItem(var3, (String)null, 100));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "Chocolate", 100));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "Butter", 70));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "Flour2", 70));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "Spoon", 100));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "EggCarton", 100));
         var2.add(new StorySpawnItem((LinkedHashMap)null, "Egg", 100));
         allStories.add(new StoryDef(var2, var1));
      }
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      return false;
   }

   public void randomizeBuilding(BuildingDef var1) {
      this.initStories();
      if (this.table1 != null && this.table2 != null) {
         if (this.table1.getSquare() != null && this.table1.getSquare().getRoom() != null) {
            ArrayList var2 = new ArrayList();

            for(int var3 = 0; var3 < allStories.size(); ++var3) {
               StoryDef var4 = (StoryDef)allStories.get(var3);
               if (var4.rooms == null || var4.rooms.contains(this.table1.getSquare().getRoom().getName())) {
                  var2.add(var4);
               }
            }

            if (!var2.isEmpty()) {
               StoryDef var13 = (StoryDef)var2.get(Rand.Next(0, var2.size()));
               if (var13 != null) {
                  boolean var14 = true;
                  if ((int)this.table1.getY() != (int)this.table2.getY()) {
                     var14 = false;
                  }

                  this.doSpawnTable(var13.items, var14);
                  if (var13.addBlood) {
                     int var5 = (int)this.table1.getX() - 1;
                     int var6 = (int)this.table1.getX() + 1;
                     int var7 = (int)this.table1.getY() - 1;
                     int var8 = (int)this.table2.getY() + 1;
                     if (var14) {
                        var5 = (int)this.table1.getX() - 1;
                        var6 = (int)this.table2.getX() + 1;
                        var7 = (int)this.table1.getY() - 1;
                        var8 = (int)this.table2.getY() + 1;
                     }

                     for(int var9 = var5; var9 < var6 + 1; ++var9) {
                        for(int var10 = var7; var10 < var8 + 1; ++var10) {
                           int var11 = Rand.Next(7, 15);

                           for(int var12 = 0; var12 < var11; ++var12) {
                              this.currentSquare.getChunk().addBloodSplat((float)var9 + Rand.Next(-0.5F, 0.5F), (float)var10 + Rand.Next(-0.5F, 0.5F), this.table1.getZ(), Rand.Next(8));
                           }
                        }
                     }
                  }

               }
            }
         }
      }
   }

   private void doSpawnTable(ArrayList<StorySpawnItem> var1, boolean var2) {
      this.xOffset = 0.0F;
      this.yOffset = 0.0F;
      int var3 = 0;
      if (var2) {
         this.xOffset = 0.6F;
         this.yOffset = Rand.Next(0.5F, 1.1F);
      } else {
         this.yOffset = 0.6F;
         this.xOffset = Rand.Next(0.5F, 1.1F);
      }

      for(this.currentSquare = this.table1.getSquare(); var3 < var1.size(); ++var3) {
         StorySpawnItem var4 = (StorySpawnItem)var1.get(var3);
         String var5 = this.getItemFromSSI(var4);
         if (var5 != null) {
            InventoryItem var6 = ItemSpawner.spawnItem(var5, this.currentSquare, this.xOffset, this.yOffset, 0.4F);
            if (var6 != null) {
               this.increaseOffsets(var2, var4);
            }
         }
      }

   }

   private void increaseOffsets(boolean var1, StorySpawnItem var2) {
      float var3 = 0.15F + var2.forcedOffset;
      float var4;
      if (var1) {
         this.xOffset += var3;
         if (this.xOffset > 1.0F) {
            this.currentSquare = this.table2.getSquare();
            this.xOffset = 0.35F;
         }

         for(var4 = this.yOffset; Math.abs(var4 - this.yOffset) < 0.11F; this.yOffset = Rand.Next(0.5F, 1.1F)) {
         }
      } else {
         this.yOffset += var3;
         if (this.yOffset > 1.0F) {
            this.currentSquare = this.table2.getSquare();
            this.yOffset = 0.35F;
         }

         for(var4 = this.xOffset; Math.abs(var4 - this.xOffset) < 0.11F; this.xOffset = Rand.Next(0.5F, 1.1F)) {
         }
      }

   }

   private String getItemFromSSI(StorySpawnItem var1) {
      if (Rand.Next(100) > var1.chanceToSpawn) {
         return null;
      } else if (var1.eitherObject != null && !var1.eitherObject.isEmpty()) {
         int var2 = Rand.Next(100);
         int var3 = 0;
         Iterator var4 = var1.eitherObject.keySet().iterator();

         String var5;
         do {
            if (!var4.hasNext()) {
               return null;
            }

            var5 = (String)var4.next();
            int var6 = (Integer)var1.eitherObject.get(var5);
            var3 += var6;
         } while(var3 < var2);

         return var5;
      } else {
         return var1.object;
      }
   }

   public class StorySpawnItem {
      LinkedHashMap<String, Integer> eitherObject = null;
      String object = null;
      Integer chanceToSpawn = null;
      float forcedOffset = 0.0F;

      public StorySpawnItem(LinkedHashMap<String, Integer> var2, String var3, Integer var4) {
         this.eitherObject = var2;
         this.object = var3;
         this.chanceToSpawn = var4;
      }

      public StorySpawnItem(LinkedHashMap<String, Integer> var2, String var3, Integer var4, float var5) {
         this.eitherObject = var2;
         this.object = var3;
         this.chanceToSpawn = var4;
         this.forcedOffset = var5;
      }
   }

   public class StoryDef {
      public ArrayList<StorySpawnItem> items = null;
      public boolean addBlood = false;
      public ArrayList<String> rooms = null;

      public StoryDef(ArrayList<StorySpawnItem> var2) {
         this.items = var2;
      }

      public StoryDef(ArrayList<StorySpawnItem> var2, ArrayList<String> var3) {
         this.items = var2;
         this.rooms = var3;
      }
   }
}
