package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.worldgen.biomes.IBiome;

public final class Flowerbed extends ErosionCategory {
   private final int[] tileID = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 28, 29, 30, 31};
   private final ArrayList<ErosionObj> objs = new ArrayList();

   public Flowerbed() {
   }

   public boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6) {
      int var7 = var1.getObjects().size();

      for(int var8 = var7 - 1; var8 >= 0; --var8) {
         IsoSprite var9 = ((IsoObject)var1.getObjects().get(var8)).getSprite();
         if (var9 != null && var9.getName() != null) {
            int var10;
            if (var9.getName().startsWith("f_flowerbed_1")) {
               var10 = Integer.parseInt(var9.getName().replace("f_flowerbed_1_", ""));
               if (var10 <= 23) {
                  if (var10 >= 12) {
                     var10 -= 12;
                  }

                  CategoryData var14 = (CategoryData)this.setCatModData(var2);
                  var14.hasSpawned = true;
                  var14.gameObj = var10;
                  var14.dispSeason = -1;
                  ErosionObj var15 = (ErosionObj)this.objs.get(var14.gameObj);
                  ((IsoObject)var1.getObjects().get(var8)).setName(var15.name);
                  return true;
               }
            }

            if (var9.getName().startsWith("vegetation_ornamental_01")) {
               var10 = Integer.parseInt(var9.getName().replace("vegetation_ornamental_01_", ""));

               for(int var11 = 0; var11 < this.tileID.length; ++var11) {
                  if (this.tileID[var11] == var10) {
                     CategoryData var12 = (CategoryData)this.setCatModData(var2);
                     var12.hasSpawned = true;
                     var12.gameObj = var11;
                     var12.dispSeason = -1;
                     ErosionObj var13 = (ErosionObj)this.objs.get(var12.gameObj);
                     ((IsoObject)var1.getObjects().get(var8)).setName(var13.name);
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6, boolean var7) {
      return false;
   }

   public void update(IsoGridSquare var1, ErosionData.Square var2, ErosionCategory.Data var3, ErosionData.Chunk var4, int var5) {
      CategoryData var6 = (CategoryData)var3;
      if (!var6.doNothing) {
         if (var6.gameObj >= 0 && var6.gameObj < this.objs.size()) {
            ErosionObj var7 = (ErosionObj)this.objs.get(var6.gameObj);
            boolean var8 = false;
            byte var9 = 0;
            int var10 = ErosionMain.getInstance().getSeasons().getSeason();
            boolean var11 = false;
            if (var10 == 5) {
               IsoObject var12 = var7.getObject(var1, false);
               if (var12 != null) {
                  var12.setSprite(ErosionMain.getInstance().getSpriteManager().getSprite("blends_natural_01_64"));
                  var12.setName((String)null);
               }

               this.clearCatModData(var2);
            } else {
               this.updateObj(var2, var3, var1, var7, var8, var9, var10, var11);
            }
         } else {
            this.clearCatModData(var2);
         }

      }
   }

   public void init() {
      String var1 = "vegetation_ornamental_01_";

      for(int var2 = 0; var2 < this.tileID.length; ++var2) {
         ErosionObjSprites var3 = new ErosionObjSprites(1, "Flowerbed", false, false, false);
         var3.setBase(0, (String)(var1 + this.tileID[var2]), 1);
         var3.setBase(0, (String)(var1 + this.tileID[var2]), 2);
         var3.setBase(0, (String)(var1 + (this.tileID[var2] + 16)), 4);
         ErosionObj var4 = new ErosionObj(var3, 30, 0.0F, 0.0F, false);
         this.objs.add(var4);
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

   private static final class CategoryData extends ErosionCategory.Data {
      public int gameObj;

      private CategoryData() {
      }

      public void save(ByteBuffer var1) {
         super.save(var1);
         var1.put((byte)this.gameObj);
      }

      public void load(ByteBuffer var1, int var2) {
         super.load(var1, var2);
         this.gameObj = var1.get();
      }
   }
}
