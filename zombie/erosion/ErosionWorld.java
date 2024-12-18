package zombie.erosion;

import zombie.erosion.categories.ErosionCategory;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.worldgen.biomes.IBiome;

public final class ErosionWorld {
   public ErosionWorld() {
   }

   public boolean init() {
      ErosionRegions.init();
      return true;
   }

   public void validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3) {
      boolean var4 = var1.Is(IsoFlagType.exterior);
      boolean var5 = var1.Has(IsoObjectType.wall);
      IsoObject var6 = var1.getFloor();
      String var7 = var6 != null && var6.getSprite() != null ? var6.getSprite().getName() : null;
      if (var7 == null) {
         var2.doNothing = true;
      } else {
         IBiome var8 = var1.getBiome();
         boolean var9 = false;

         for(int var10 = 0; var10 < ErosionRegions.regions.size(); ++var10) {
            ErosionRegions.Region var11 = (ErosionRegions.Region)ErosionRegions.regions.get(var10);
            String var12 = var11.tileNameMatch;
            if ((var12 == null || var7.startsWith(var12)) && (!var11.checkExterior || var11.isExterior == var4) && (!var11.hasWall || var11.hasWall == var5)) {
               for(int var13 = 0; var13 < var11.categories.size(); ++var13) {
                  ErosionCategory var14 = (ErosionCategory)var11.categories.get(var13);
                  boolean var15 = var14.replaceExistingObject(var1, var2, var3, var8, var4, var5);
                  if (!var15) {
                     var15 = var14.validateSpawn(var1, var2, var3, var8, var4, var5, false);
                  }

                  if (var15) {
                     var9 = true;
                     break;
                  }
               }
            }
         }

         if (!var9) {
            var2.doNothing = true;
         }

      }
   }

   public void update(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, int var4) {
      if (var2.regions != null) {
         for(int var5 = 0; var5 < var2.regions.size(); ++var5) {
            ErosionCategory.Data var6 = (ErosionCategory.Data)var2.regions.get(var5);
            ErosionCategory var7 = ErosionRegions.getCategory(var6.regionID, var6.categoryID);
            int var8 = var2.regions.size();
            var7.update(var1, var2, var6, var3, var4);
            if (var8 > var2.regions.size()) {
               --var5;
            }
         }

      }
   }
}
