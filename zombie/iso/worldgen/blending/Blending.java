package zombie.iso.worldgen.blending;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTree;
import zombie.iso.worldgen.WGChunk;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.WGTile;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;

public class Blending {
   private static final int maxDepth = 4;
   private final WGTile wgTile = new WGTile();
   private final List<String> plantsAdded = Lists.newArrayList(new String[]{"e_newgrass_1_40", "e_newgrass_1_42"});

   public Blending() {
   }

   public void applyBlending(IsoChunk var1) {
      BlendDirection[] var2 = BlendDirection.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         BlendDirection var5 = var2[var4];
         if (!var1.isBlendingDone(var5.index)) {
            IsoCell var6 = IsoWorld.instance.CurrentCell;
            IsoChunk var7 = var6.getChunk(var1.wx + var5.x, var1.wy + var5.y);
            if (var7 != null && var7.bLoaded) {
               IsoChunk var8 = var6.getChunk(var1.wx + var5.opposite().x, var1.wy + var5.opposite().y);
               if (!var1.isBlendingDonePartial() || var8 != null && var8.bLoaded) {
                  if (var7.isBlendingDoneFull()) {
                     DebugLog.log(String.format("BLENDING: Chunk [%s %s] | %s | Worldgen chunk [%s %s]", var1.wx, var1.wy, var5, var7.wx, var7.wy));
                     this.removeTrees(var6, var1, var5);
                     this.changeGround(var6, var1, var5);
                  }

                  var1.setBlendingModified(var5.index);
                  var7.setBlendingModified(var5.opposite().index);
               }
            }
         }
      }

   }

   private void removeTrees(IsoCell var1, IsoChunk var2, BlendDirection var3) {
      Random var4 = WGParams.instance.getRandom(var2.wx, var2.wy);

      for(int var5 = 0; var5 < 8; ++var5) {
         for(int var6 = 0; var6 < 8; ++var6) {
            boolean var10000;
            switch (var3) {
               case NORTH:
                  var10000 = var4.nextInt(100) >= var6 * 10;
                  break;
               case SOUTH:
                  var10000 = var4.nextInt(100) >= (!var2.isBlendingDonePartial() ? (8 - var6 - 1) * 10 : var6 * 10);
                  break;
               case WEST:
                  var10000 = var4.nextInt(100) >= var5 * 10;
                  break;
               case EAST:
                  var10000 = var4.nextInt(100) >= (!var2.isBlendingDonePartial() ? (8 - var5 - 1) * 10 : var5 * 10);
                  break;
               default:
                  throw new IncompatibleClassChangeError();
            }

            boolean var7 = var10000;
            if (var7) {
               IsoGridSquare var8 = this.getSquare(var1, var2, var3, var5, var6);
               IsoTree var9 = var8.getTree();
               if (var9 != null) {
                  var8.DeleteTileObject(var9);
                  if (var4.nextInt(100) >= 25) {
                     String var10 = (String)this.plantsAdded.get(var4.nextInt(this.plantsAdded.size()));
                     var8.addTileObject(var10);
                  }
               }
            }
         }
      }

   }

   private void changeGround(IsoCell var1, IsoChunk var2, BlendDirection var3) {
      Random var4 = WGParams.instance.getRandom(var2.wx, var2.wy);
      WGChunk var5 = IsoWorld.instance.getWgChunk();

      for(int var6 = 0; var6 < 8; ++var6) {
         IBiome var10000;
         switch (var3) {
            case NORTH:
               var10000 = var5.getBiome(var2.wx * 8 + var6, var2.wy * 8 - 1);
               break;
            case SOUTH:
               var10000 = var5.getBiome(var2.wx * 8 + var6, var2.wy * 8 + 8);
               break;
            case WEST:
               var10000 = var5.getBiome(var2.wx * 8 - 1, var2.wy * 8 + var6);
               break;
            case EAST:
               var10000 = var5.getBiome(var2.wx * 8 + 8, var2.wy * 8 + var6);
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         IBiome var7 = var10000;
         TileGroup var8 = this.wgTile.getGround(var7, var4);
         if (var8 != null && !var8.tiles().isEmpty()) {
            int var9 = var4.nextInt(4);

            for(int var10 = 0; var10 < var9; ++var10) {
               IsoGridSquare var11 = this.getSquare(var1, var2, var3, var6 * var3.planeY + var10 * var3.planeX, var6 * var3.planeX + var10 * var3.planeY);
               if (var11.getFloor().sprite.getName().contains("blends_natural_01")) {
                  this.wgTile.setGround(this.wgTile.getSprite((String)var8.tiles().get(0)), var11);
                  this.wgTile.deleteTiles(var11, this.plantsAdded);
                  TileGroup var12 = this.wgTile.getPlant(var7, var4);
                  if (var12 != null) {
                     var11.addTileObject((String)var12.tiles().get(0));
                  }
               }
            }
         } else {
            DebugLog.log(String.format("GROUND is empty for biome %s", var7));
         }
      }

   }

   private IsoGridSquare getSquare(IsoCell var1, IsoChunk var2, BlendDirection var3, int var4, int var5) {
      int var6 = var2.wx * 8;
      int var7 = var2.wy * 8;
      IsoGridSquare var10000;
      if (!var2.isBlendingDonePartial()) {
         var10000 = var1.getGridSquare(var6 + var4, var7 + var5, 0);
      } else {
         switch (var3) {
            case NORTH:
               var10000 = var1.getGridSquare(var6 + var4, var7 + var2.getModifDepth(BlendDirection.NORTH) + var5, 0);
               break;
            case SOUTH:
               var10000 = var1.getGridSquare(var6 + var4, var7 + var2.getModifDepth(BlendDirection.SOUTH) - var5, 0);
               break;
            case WEST:
               var10000 = var1.getGridSquare(var6 + var2.getModifDepth(BlendDirection.WEST) + var4, var7 + var5, 0);
               break;
            case EAST:
               var10000 = var1.getGridSquare(var6 + var2.getModifDepth(BlendDirection.EAST) - var4, var7 + var5, 0);
               break;
            default:
               throw new IncompatibleClassChangeError();
         }
      }

      IsoGridSquare var8 = var10000;
      if (var8 == null) {
         throw new RuntimeException(String.format("Square is null at [%s, %s]+(%s, %s) | %s", var6, var7, var4, var5, var3));
      } else {
         return var8;
      }
   }
}
