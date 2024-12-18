package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.worldgen.biomes.IBiome;

public final class NaturePlants extends ErosionCategory {
   private final int[][] soilRef = new int[][]{{17, 17, 17, 17, 17, 17, 17, 17, 17, 1, 2, 8, 8}, {11, 12, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8}, {11, 12, 11, 12, 11, 12, 11, 12, 15, 16, 18, 19}, {22, 22, 22, 22, 22, 22, 22, 22, 22, 3, 4, 14}, {15, 16, 3, 4, 14, 3, 4, 14, 3, 4, 14, 3, 4, 14}, {11, 12, 15, 16, 15, 16, 15, 16, 15, 16, 21}, {13, 13, 13, 13, 13, 13, 13, 13, 13, 5, 6, 24}, {18, 19, 5, 6, 24, 5, 6, 24, 5, 6, 24, 5, 6, 24}, {18, 19, 18, 19, 18, 19, 18, 19, 20, 21}, {7, 7, 7, 7, 7, 7, 7, 7, 7, 9, 10, 23}, {19, 20, 9, 10, 23, 9, 10, 23, 9, 10, 23, 9, 10, 23}, {15, 16, 18, 19, 20, 19, 20, 19, 20}};
   private final int[] spawnChance = new int[100];
   private final ArrayList<ErosionObj> objs = new ArrayList();
   private final PlantInit[] plants = new PlantInit[]{new PlantInit("Butterfly Weed", true, 0.05F, 0.25F), new PlantInit("Butterfly Weed", true, 0.05F, 0.25F), new PlantInit("Swamp Sunflower", true, 0.2F, 0.45F), new PlantInit("Swamp Sunflower", true, 0.2F, 0.45F), new PlantInit("Purple Coneflower", true, 0.1F, 0.35F), new PlantInit("Purple Coneflower", true, 0.1F, 0.35F), new PlantInit("Joe-Pye Weed", true, 0.8F, 1.0F), new PlantInit("Blazing Star", true, 0.25F, 0.65F), new PlantInit("Wild Bergamot", true, 0.45F, 0.6F), new PlantInit("Wild Bergamot", true, 0.45F, 0.6F), new PlantInit("White Beard-tongue", true, 0.2F, 0.65F), new PlantInit("White Beard-tongue", true, 0.2F, 0.65F), new PlantInit("Ironweed", true, 0.75F, 0.85F), new PlantInit("White Baneberry", true, 0.4F, 0.8F), new PlantInit("Wild Columbine", true, 0.85F, 1.0F), new PlantInit("Wild Columbine", true, 0.85F, 1.0F), new PlantInit("Jack-in-the-pulpit", false, 0.0F, 0.0F), new PlantInit("Wild Ginger", true, 0.1F, 0.9F), new PlantInit("Wild Ginger", true, 0.1F, 0.9F), new PlantInit("Wild Geranium", true, 0.65F, 0.9F), new PlantInit("Alumroot", true, 0.35F, 0.75F), new PlantInit("Wild Blue Phlox", true, 0.15F, 0.55F), new PlantInit("Polemonium Reptans", true, 0.4F, 0.6F), new PlantInit("Foamflower", true, 0.45F, 1.0F)};

   public NaturePlants() {
   }

   public boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6) {
      int var7 = var1.getObjects().size();

      for(int var8 = var7 - 1; var8 >= 1; --var8) {
         IsoObject var9 = (IsoObject)var1.getObjects().get(var8);
         IsoSprite var10 = var9.getSprite();
         if (var10 != null && var10.getName() != null) {
            if (var10.getName().startsWith("d_plants_1_")) {
               int var13 = Integer.parseInt(var10.getName().replace("d_plants_1_", ""));
               CategoryData var12 = (CategoryData)this.setCatModData(var2);
               var12.gameObj = var13 < 32 ? var13 % 8 : (var13 < 48 ? var13 % 8 + 8 : var13 % 8 + 16);
               var12.stage = 0;
               var12.spawnTime = 0;
               var1.RemoveTileObjectErosionNoRecalc(var9);
               return true;
            }

            CategoryData var11;
            if ("vegetation_groundcover_01_16".equals(var10.getName()) || "vegetation_groundcover_01_17".equals(var10.getName())) {
               var11 = (CategoryData)this.setCatModData(var2);
               var11.gameObj = 21;
               var11.stage = 0;
               var11.spawnTime = 0;
               var1.RemoveTileObjectErosionNoRecalc(var9);

               while(true) {
                  --var8;
                  if (var8 <= 0) {
                     return true;
                  }

                  var9 = (IsoObject)var1.getObjects().get(var8);
                  var10 = var9.getSprite();
                  if (var10 != null && var10.getName() != null && var10.getName().startsWith("vegetation_groundcover_01_")) {
                     var1.RemoveTileObjectErosionNoRecalc(var9);
                  }
               }
            }

            if ("vegetation_groundcover_01_18".equals(var10.getName()) || "vegetation_groundcover_01_19".equals(var10.getName()) || "vegetation_groundcover_01_20".equals(var10.getName()) || "vegetation_groundcover_01_21".equals(var10.getName()) || "vegetation_groundcover_01_22".equals(var10.getName()) || "vegetation_groundcover_01_23".equals(var10.getName())) {
               var11 = (CategoryData)this.setCatModData(var2);
               var11.gameObj = var2.rand(var1.x, var1.y, this.plants.length);
               var11.stage = 0;
               var11.spawnTime = 0;
               var1.RemoveTileObjectErosionNoRecalc(var9);

               while(true) {
                  --var8;
                  if (var8 <= 0) {
                     return true;
                  }

                  var9 = (IsoObject)var1.getObjects().get(var8);
                  var10 = var9.getSprite();
                  if (var10 != null && var10.getName() != null && var10.getName().startsWith("vegetation_groundcover_01_")) {
                     var1.RemoveTileObjectErosionNoRecalc(var9);
                  }
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
         if (var2.rand(var1.x, var1.y, 101) < this.spawnChance[var9]) {
            CategoryData var10 = (CategoryData)this.setCatModData(var2);
            var10.gameObj = var8[var2.rand(var1.x, var1.y, var8.length)] - 1;
            var10.stage = 0;
            var10.spawnTime = 100 - var9;
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
            boolean var8 = false;
            byte var9 = 0;
            int var10 = this.currentSeason(var2.magicNum, var7);
            boolean var11 = this.currentBloom(var2.magicNum, var7);
            this.updateObj(var2, var3, var1, var7, var8, var9, var10, var11);
         } else {
            this.clearCatModData(var2);
         }

      }
   }

   public void init() {
      for(int var1 = 0; var1 < 100; ++var1) {
         if (var1 >= 20 && var1 < 50) {
            this.spawnChance[var1] = (int)this.clerp((float)(var1 - 20) / 30.0F, 0.0F, 8.0F);
         } else if (var1 >= 50 && var1 < 80) {
            this.spawnChance[var1] = (int)this.clerp((float)(var1 - 50) / 30.0F, 8.0F, 0.0F);
         }
      }

      this.seasonDisp[5].season1 = 0;
      this.seasonDisp[5].season2 = 0;
      this.seasonDisp[5].split = false;
      this.seasonDisp[1].season1 = 1;
      this.seasonDisp[1].season2 = 0;
      this.seasonDisp[1].split = false;
      this.seasonDisp[2].season1 = 2;
      this.seasonDisp[2].season2 = 2;
      this.seasonDisp[2].split = true;
      this.seasonDisp[4].season1 = 4;
      this.seasonDisp[4].season2 = 0;
      this.seasonDisp[4].split = true;
      String var11 = "d_plants_1_";
      ArrayList var2 = new ArrayList();

      for(int var3 = 0; var3 <= 7; ++var3) {
         var2.add(var11 + var3);
      }

      ArrayList var12 = new ArrayList();

      for(int var4 = 8; var4 <= 15; ++var4) {
         var12.add(var11 + var4);
      }

      byte var13 = 16;

      for(int var5 = 0; var5 < this.plants.length; ++var5) {
         if (var5 >= 8) {
            var13 = 24;
         }

         if (var5 >= 16) {
            var13 = 32;
         }

         PlantInit var6 = this.plants[var5];
         ErosionObjSprites var7 = new ErosionObjSprites(1, var6.name, false, var6.hasFlower, false);
         var7.setBase(0, (ArrayList)var2, 1);
         var7.setBase(0, (ArrayList)var12, 4);
         var7.setBase(0, (String)(var11 + (var13 + var5)), 2);
         var7.setFlower(0, (String)(var11 + (var13 + var5 + 8)));
         float var8 = var6.hasFlower ? var6.bloomstart : 0.0F;
         float var9 = var6.hasFlower ? var6.bloomend : 0.0F;
         ErosionObj var10 = new ErosionObj(var7, 30, var8, var9, false);
         this.objs.add(var10);
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

   private static final class PlantInit {
      public String name;
      public boolean hasFlower;
      public float bloomstart;
      public float bloomend;

      public PlantInit(String var1, boolean var2, float var3, float var4) {
         this.name = var1;
         this.hasFlower = var2;
         this.bloomstart = var3;
         this.bloomend = var4;
      }
   }

   private static final class CategoryData extends ErosionCategory.Data {
      public int gameObj;
      public int spawnTime;

      private CategoryData() {
      }

      public void save(ByteBuffer var1) {
         super.save(var1);
         var1.put((byte)this.gameObj);
         var1.putShort((short)this.spawnTime);
      }

      public void load(ByteBuffer var1, int var2) {
         super.load(var1, var2);
         this.gameObj = var1.get();
         this.spawnTime = var1.getShort();
      }
   }
}
