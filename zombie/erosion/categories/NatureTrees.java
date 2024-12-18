package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.worldgen.biomes.IBiome;

public final class NatureTrees extends ErosionCategory {
   private final int[][] soilRef = new int[][]{{2, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5}, {1, 1, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5}, {2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 3, 3, 4, 4, 4, 5}, {1, 7, 7, 7, 9, 9, 9, 9, 9, 9, 9}, {2, 2, 1, 1, 1, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 9, 9, 9, 9}, {1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 7, 7, 7, 9}, {1, 2, 8, 8, 8, 6, 6, 6, 6, 6, 6, 6, 6}, {1, 1, 2, 2, 3, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 6, 6, 6, 6, 6}, {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 8, 8, 8, 6}, {3, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11}, {1, 1, 3, 3, 3, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11}, {1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 10, 10, 10, 11}};
   private final TreeInit[] trees = new TreeInit[]{new TreeInit("American Holly", "e_americanholly_1", true), new TreeInit("Canadian Hemlock", "e_canadianhemlock_1", true), new TreeInit("Virginia Pine", "e_virginiapine_1", true), new TreeInit("Riverbirch", "e_riverbirch_1", false), new TreeInit("Cockspur Hawthorn", "e_cockspurhawthorn_1", false), new TreeInit("Dogwood", "e_dogwood_1", false), new TreeInit("Carolina Silverbell", "e_carolinasilverbell_1", false), new TreeInit("Yellowwood", "e_yellowwood_1", false), new TreeInit("Eastern Redbud", "e_easternredbud_1", false), new TreeInit("Redmaple", "e_redmaple_1", false), new TreeInit("American Linden", "e_americanlinden_1", false)};
   private final int[] spawnChance = new int[100];
   private final ArrayList<ErosionObj> objs = new ArrayList();

   public NatureTrees() {
   }

   public boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6) {
      int var7 = var1.getObjects().size();

      for(int var8 = var7 - 1; var8 >= 1; --var8) {
         IsoObject var9 = (IsoObject)var1.getObjects().get(var8);
         IsoSprite var10 = var9.getSprite();
         if (var10 != null && var10.getName() != null) {
            int var11;
            int var13;
            int[] var16;
            CategoryData var17;
            ErosionObj var18;
            if (var10.getName().startsWith("jumbo_tree_01")) {
               var11 = var2.soil;
               if (var11 < 0 || var11 >= this.soilRef.length) {
                  var11 = var2.rand(var1.x, var1.y, this.soilRef.length);
               }

               var16 = this.soilRef[var11];
               var13 = var2.noiseMainInt;
               var17 = (CategoryData)this.setCatModData(var2);
               var17.gameObj = var16[var2.rand(var1.x, var1.y, var16.length)] - 1;
               var17.maxStage = 5 + (int)Math.floor((double)((float)var13 / 51.0F)) - 1;
               var17.stage = var17.maxStage;
               var17.spawnTime = 0;
               var17.dispSeason = -1;
               var18 = (ErosionObj)this.objs.get(var17.gameObj);
               var9.setName(var18.name);
               var17.hasSpawned = true;
               return true;
            }

            if (var10.getName().startsWith("vegetation_trees")) {
               var11 = var2.soil;
               if (var11 < 0 || var11 >= this.soilRef.length) {
                  var11 = var2.rand(var1.x, var1.y, this.soilRef.length);
               }

               var16 = this.soilRef[var11];
               var13 = var2.noiseMainInt;
               var17 = (CategoryData)this.setCatModData(var2);
               var17.gameObj = var16[var2.rand(var1.x, var1.y, var16.length)] - 1;
               var17.maxStage = 3 + (int)Math.floor((double)((float)var13 / 51.0F)) - 1;
               var17.stage = var17.maxStage;
               var17.spawnTime = 0;
               var17.dispSeason = -1;
               var18 = (ErosionObj)this.objs.get(var17.gameObj);
               var9.setName(var18.name);
               var17.hasSpawned = true;
               return true;
            }

            for(var11 = 0; var11 < this.trees.length; ++var11) {
               CategoryData var12;
               if (var10.getName().startsWith(this.trees[var11].tilesetName)) {
                  var12 = (CategoryData)this.setCatModData(var2);
                  var12.gameObj = var11;
                  var12.maxStage = 3;
                  var12.stage = var12.maxStage;
                  var12.spawnTime = 0;
                  var1.RemoveTileObject(var9);
                  return true;
               }

               if (var10.getName().startsWith(this.trees[var11].jumboTilesetName)) {
                  var12 = (CategoryData)this.setCatModData(var2);
                  var12.gameObj = var11;
                  var12.maxStage = 5;
                  var13 = var10.getName().lastIndexOf(95);
                  int var14 = PZMath.tryParseInt(var10.getName().substring(var13 + 1), 0);
                  byte var15 = 2;
                  var12.stage = var14 % var15 == 0 ? 4 : 5;
                  var12.spawnTime = 0;
                  var1.RemoveTileObject(var9);
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6, boolean var7) {
      if (var1.getObjects().size() > (var6 ? 2 : 1)) {
         return false;
      } else if (var2.soil >= 0 && var2.soil < this.soilRef.length) {
         int[] var8 = this.soilRef[var2.soil];
         int var9 = var2.noiseMainInt;
         int var10 = this.spawnChance[var9];
         if (var10 > 0 && var2.rand(var1.x, var1.y, 101) < var10) {
            CategoryData var11 = (CategoryData)this.setCatModData(var2);
            var11.gameObj = var8[var2.rand(var1.x, var1.y, var8.length)] - 1;
            var11.maxStage = 2 + (int)Math.floor((double)((var9 - 50) / 17)) - 1;
            var11.stage = 0;
            var11.spawnTime = 30 + (100 - var9);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void update(IsoGridSquare var1, ErosionData.Square var2, ErosionCategory.Data var3, ErosionData.Chunk var4, int var5) {
      CategoryData var6 = (CategoryData)var3;
      if (var5 >= var6.spawnTime && !var6.doNothing) {
         if (var6.gameObj >= 0 && var6.gameObj < this.objs.size()) {
            ErosionObj var7 = (ErosionObj)this.objs.get(var6.gameObj);
            int var8 = var6.maxStage;
            int var9 = (int)Math.floor((double)((float)(var5 - var6.spawnTime) / ((float)var7.cycleTime / ((float)var8 + 1.0F))));
            if (var9 < var3.stage) {
               var9 = var3.stage;
            }

            if (var9 > var8) {
               var9 = var8;
            }

            boolean var10 = true;
            int var11 = this.currentSeason(var2.magicNum, var7);
            boolean var12 = false;
            this.updateObj(var2, var3, var1, var7, var10, var9, var11, var12);
         } else {
            this.clearCatModData(var2);
         }

      }
   }

   public void init() {
      for(int var1 = 0; var1 < 100; ++var1) {
         this.spawnChance[var1] = var1 >= 50 ? (int)this.clerp((float)(var1 - 50) / 50.0F, 0.0F, 90.0F) : 0;
      }

      int[] var12 = new int[]{0, 5, 1, 2, 3, 4};
      this.seasonDisp[5].season1 = 0;
      this.seasonDisp[5].season2 = 0;
      this.seasonDisp[5].split = false;
      this.seasonDisp[1].season1 = 1;
      this.seasonDisp[1].season2 = 0;
      this.seasonDisp[1].split = false;
      this.seasonDisp[2].season1 = 2;
      this.seasonDisp[2].season2 = 3;
      this.seasonDisp[2].split = true;
      this.seasonDisp[4].season1 = 4;
      this.seasonDisp[4].season2 = 0;
      this.seasonDisp[4].split = true;
      String var2 = null;
      ErosionIceQueen var3 = ErosionIceQueen.instance;

      for(int var4 = 0; var4 < this.trees.length; ++var4) {
         String var5 = this.trees[var4].name;
         String var6 = this.trees[var4].tilesetName;
         boolean var7 = !this.trees[var4].evergreen;
         ErosionObjSprites var8 = new ErosionObjSprites(6, var5, true, false, var7);

         for(int var9 = 0; var9 < 6; ++var9) {
            for(int var10 = 0; var10 < var12.length; ++var10) {
               int var11;
               if (var9 > 3) {
                  var11 = 0 + var10 * 2 + (var9 - 4);
                  if (var10 == 0) {
                     String var10000 = var6.replace("_1", "JUMBO_1");
                     var2 = var10000 + "_" + var11;
                     var8.setBase(var9, (String)var2, 0);
                  } else if (var10 == 1) {
                     String var10002 = var6.replace("_1", "JUMBO_1");
                     var3.addSprite(var2, var10002 + "_" + var11);
                  } else if (var7) {
                     var8.setChildSprite(var9, var6.replace("_1", "JUMBO_1") + "_" + var11, var12[var10]);
                  }
               } else {
                  var11 = 0 + var10 * 4 + var9;
                  if (var10 == 0) {
                     var2 = var6 + "_" + var11;
                     var8.setBase(var9, (String)var2, 0);
                  } else if (var10 == 1) {
                     var3.addSprite(var2, var6 + "_" + var11);
                  } else if (var7) {
                     var8.setChildSprite(var9, var6 + "_" + var11, var12[var10]);
                  }
               }
            }
         }

         ErosionObj var13 = new ErosionObj(var8, 60, 0.0F, 0.0F, true);
         this.objs.add(var13);
      }

   }

   protected ErosionCategory.Data allocData() {
      return new CategoryData();
   }

   public void getObjectNames(ArrayList<String> var1) {
      for(int var2 = 0; var2 < this.objs.size(); ++var2) {
         if (((ErosionObj)this.objs.get(var2)).name != null && !var1.contains(((ErosionObj)this.objs.get(var2)).name)) {
            var1.add(((ErosionObj)this.objs.get(var2)).name);
         }
      }

   }

   private static final class TreeInit {
      public String name;
      public String tilesetName;
      public String jumboTilesetName;
      public boolean evergreen;

      public TreeInit(String var1, String var2, boolean var3) {
         this.name = var1;
         this.tilesetName = var2;
         this.jumboTilesetName = var2.replace("_1", "JUMBO_1");
         this.evergreen = var3;
      }
   }

   private static final class CategoryData extends ErosionCategory.Data {
      public int gameObj;
      public int maxStage;
      public int spawnTime;

      private CategoryData() {
      }

      public void save(ByteBuffer var1) {
         super.save(var1);
         var1.put((byte)this.gameObj);
         var1.put((byte)this.maxStage);
         var1.putShort((short)this.spawnTime);
      }

      public void load(ByteBuffer var1, int var2) {
         super.load(var1, var2);
         this.gameObj = var1.get();
         this.maxStage = var1.get();
         this.spawnTime = var1.getShort();
      }
   }
}
