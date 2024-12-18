package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.worldgen.biomes.IBiome;

public final class NatureGeneric extends ErosionCategory {
   private ArrayList<ErosionObj> objs = new ArrayList();
   private static final int GRASS = 0;
   private static final int FERNS = 1;
   private static final int GENERIC = 2;
   private ArrayList<ArrayList<Integer>> objsRef = new ArrayList();
   private int[] spawnChance = new int[100];

   public NatureGeneric() {
   }

   public boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6) {
      int var7 = var1.getObjects().size();

      for(int var8 = var7 - 1; var8 >= 1; --var8) {
         IsoObject var9 = (IsoObject)var1.getObjects().get(var8);
         IsoSprite var10 = var9.getSprite();
         if (var10 != null && var10.getName() != null && var10.getName().startsWith("blends_grassoverlays")) {
            float var11 = 0.3F;
            float var12 = 12.0F;
            if ("Forest".equals(var1.getZoneType())) {
               var11 = 0.5F;
               var12 = 6.0F;
            } else if ("DeepForest".equals(var1.getZoneType())) {
               var11 = 0.7F;
               var12 = 3.0F;
            }

            CategoryData var13 = (CategoryData)this.setCatModData(var2);
            ArrayList var14 = (ArrayList)this.objsRef.get(0);
            int var15 = var2.noiseMainInt;
            int var16 = var2.rand(var1.x, var1.y, 101);
            if ((float)var16 < (float)var15 / var12) {
               if (var2.magicNum < var11) {
                  var14 = (ArrayList)this.objsRef.get(1);
               } else {
                  var14 = (ArrayList)this.objsRef.get(2);
               }

               var13.notGrass = true;
               var13.maxStage = var15 > 60 ? 1 : 0;
            } else {
               var13.maxStage = var15 > 67 ? 2 : (var15 > 50 ? 1 : 0);
            }

            var13.gameObj = (Integer)var14.get(var2.rand(var1.x, var1.y, var14.size()));
            var13.stage = var13.maxStage;
            var13.spawnTime = 0;
            var13.dispSeason = -1;
            ErosionObj var17 = (ErosionObj)this.objs.get(var13.gameObj);
            var9.setName(var17.name);
            var9.doNotSync = true;
            var13.hasSpawned = true;
            return true;
         }
      }

      return false;
   }

   public boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6, boolean var7) {
      if (var1.getObjects().size() > (var6 ? 2 : 1)) {
         return false;
      } else {
         int var8 = var2.noiseMainInt;
         if (var2.rand(var1.x, var1.y, 101) < this.spawnChance[var8]) {
            float var9 = 0.3F;
            float var10 = 12.0F;
            if ("Forest".equals(var1.getZoneType())) {
               var9 = 0.5F;
               var10 = 6.0F;
            } else if ("DeepForest".equals(var1.getZoneType())) {
               var9 = 0.7F;
               var10 = 3.0F;
            }

            CategoryData var11 = (CategoryData)this.setCatModData(var2);
            ArrayList var12 = (ArrayList)this.objsRef.get(0);
            int var13 = var2.rand(var1.x, var1.y, 101);
            if ((float)var13 < (float)var8 / var10) {
               if (var2.magicNum < var9) {
                  var12 = (ArrayList)this.objsRef.get(1);
               } else {
                  var12 = (ArrayList)this.objsRef.get(2);
               }

               var11.notGrass = true;
               var11.maxStage = var8 > 60 ? 1 : 0;
            } else {
               var11.maxStage = var8 > 67 ? 2 : (var8 > 50 ? 1 : 0);
            }

            var11.gameObj = (Integer)var12.get(var2.rand(var1.x, var1.y, var12.size()));
            var11.stage = 0;
            var11.spawnTime = 100 - var8;
            return true;
         } else {
            return false;
         }
      }
   }

   public void update(IsoGridSquare var1, ErosionData.Square var2, ErosionCategory.Data var3, ErosionData.Chunk var4, int var5) {
      CategoryData var6 = (CategoryData)var3;
      if (var5 >= var6.spawnTime && !var6.doNothing) {
         if (var6.gameObj >= 0 && var6.gameObj < this.objs.size()) {
            ErosionObj var7 = (ErosionObj)this.objs.get(var6.gameObj);
            int var8 = var6.maxStage;
            int var9 = (int)Math.floor((double)((float)(var5 - var6.spawnTime) / ((float)var7.cycleTime / ((float)var8 + 1.0F))));
            if (var9 > var8) {
               var9 = var8;
            }

            if (var9 >= var7.stages) {
               var9 = var7.stages - 1;
            }

            if (var6.stage == var6.maxStage) {
               var9 = var6.maxStage;
            }

            int var10 = 0;
            if (!var6.notGrass) {
               var10 = this.currentSeason(var2.magicNum, var7);
               int var11 = this.getGroundGrassType(var1);
               if (var11 == 2) {
                  var10 = Math.max(var10, 3);
               } else if (var11 == 3) {
                  var10 = Math.max(var10, 4);
               }
            }

            boolean var13 = false;
            boolean var12 = false;
            this.updateObj(var2, var3, var1, var7, var13, var9, var10, var12);
         } else {
            var6.doNothing = true;
         }

      }
   }

   public void init() {
      for(int var1 = 0; var1 < 100; ++var1) {
         this.spawnChance[var1] = (int)this.clerp((float)(var1 - 0) / 100.0F, 0.0F, 99.0F);
      }

      this.seasonDisp[5].season1 = 5;
      this.seasonDisp[5].season2 = 0;
      this.seasonDisp[5].split = false;
      this.seasonDisp[1].season1 = 1;
      this.seasonDisp[1].season2 = 0;
      this.seasonDisp[1].split = false;
      this.seasonDisp[2].season1 = 2;
      this.seasonDisp[2].season2 = 3;
      this.seasonDisp[2].split = true;
      this.seasonDisp[4].season1 = 4;
      this.seasonDisp[4].season2 = 5;
      this.seasonDisp[4].split = true;
      int[] var8 = new int[]{1, 2, 3, 4, 5};
      int[] var2 = new int[]{2, 1, 0};

      int var3;
      for(var3 = 0; var3 < 3; ++var3) {
         this.objsRef.add(new ArrayList());
      }

      ErosionObjSprites var4;
      int var5;
      int var6;
      int var7;
      ErosionObj var11;
      for(var3 = 0; var3 <= 5; ++var3) {
         var4 = new ErosionObjSprites(3, "Grass", false, false, false);

         for(var5 = 0; var5 < var8.length; ++var5) {
            for(var6 = 0; var6 < var2.length; ++var6) {
               var7 = 0 + var5 * 18 + var6 * 6 + var3;
               var4.setBase(var2[var6], "e_newgrass_1_" + var7, var8[var5]);
            }
         }

         var11 = new ErosionObj(var4, 60, 0.0F, 0.0F, false);
         this.objs.add(var11);
         ((ArrayList)this.objsRef.get(0)).add(this.objs.size() - 1);
      }

      for(var3 = 0; var3 <= 15; ++var3) {
         var4 = new ErosionObjSprites(2, "Generic", false, false, false);

         for(var5 = 0; var5 <= 1; ++var5) {
            var6 = var5 * 16 + var3;
            var4.setBase(var5, (String)("d_generic_1_" + var6), 0);
         }

         var11 = new ErosionObj(var4, 60, 0.0F, 0.0F, true);
         this.objs.add(var11);
         ((ArrayList)this.objsRef.get(2)).add(this.objs.size() - 1);
      }

      ErosionIceQueen var10 = ErosionIceQueen.instance;

      for(int var9 = 0; var9 <= 7; ++var9) {
         ErosionObjSprites var12 = new ErosionObjSprites(2, "Fern", true, false, false);

         for(var6 = 0; var6 <= 1; ++var6) {
            var7 = 48 + var6 * 32 + var9;
            var12.setBase(var6, (String)("d_generic_1_" + var7), 0);
            var10.addSprite("d_generic_1_" + var7, "d_generic_1_" + (var7 + 16));
         }

         ErosionObj var13 = new ErosionObj(var12, 60, 0.0F, 0.0F, true);
         this.objs.add(var13);
         ((ArrayList)this.objsRef.get(1)).add(this.objs.size() - 1);
      }

   }

   protected ErosionCategory.Data allocData() {
      return new CategoryData();
   }

   private int toInt(char var1) {
      switch (var1) {
         case '0':
            return 0;
         case '1':
            return 1;
         case '2':
            return 2;
         case '3':
            return 3;
         case '4':
            return 4;
         case '5':
            return 5;
         case '6':
            return 6;
         case '7':
            return 7;
         case '8':
            return 8;
         case '9':
            return 9;
         default:
            return 0;
      }
   }

   private int getGroundGrassType(IsoGridSquare var1) {
      IsoObject var2 = var1.getFloor();
      if (var2 == null) {
         return 0;
      } else {
         IsoSprite var3 = var2.getSprite();
         if (var3 != null && var3.getName() != null && var3.getName().startsWith("blends_natural_01_")) {
            int var4 = 0;

            int var5;
            for(var5 = 18; var5 < var3.getName().length(); ++var5) {
               var4 += this.toInt(var3.getName().charAt(var5));
               if (var5 < var3.getName().length() - 1) {
                  var4 *= 10;
               }
            }

            var5 = var4 / 8;
            int var6 = var4 % 8;
            if (var5 == 2 && (var6 == 0 || var6 >= 5)) {
               return 1;
            }

            if (var5 == 4 && (var6 == 0 || var6 >= 5)) {
               return 2;
            }

            if (var5 == 6 && (var6 == 0 || var6 >= 5)) {
               return 3;
            }
         }

         return 0;
      }
   }

   public void getObjectNames(ArrayList<String> var1) {
      for(int var2 = 0; var2 < this.objs.size(); ++var2) {
         if (((ErosionObj)this.objs.get(var2)).name != null && !var1.contains(((ErosionObj)this.objs.get(var2)).name)) {
            var1.add(((ErosionObj)this.objs.get(var2)).name);
         }
      }

   }

   private static final class CategoryData extends ErosionCategory.Data {
      public int gameObj;
      public int maxStage;
      public int spawnTime;
      public boolean notGrass;

      private CategoryData() {
      }

      public void save(ByteBuffer var1) {
         super.save(var1);
         var1.put((byte)this.gameObj);
         var1.put((byte)this.maxStage);
         var1.putShort((short)this.spawnTime);
         var1.put((byte)(this.notGrass ? 1 : 0));
      }

      public void load(ByteBuffer var1, int var2) {
         super.load(var1, var2);
         this.gameObj = var1.get();
         this.maxStage = var1.get();
         this.spawnTime = var1.getShort();
         this.notGrass = var1.get() == 1;
      }
   }
}
