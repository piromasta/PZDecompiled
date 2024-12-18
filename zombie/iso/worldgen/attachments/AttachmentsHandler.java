package zombie.iso.worldgen.attachments;

import java.util.List;
import java.util.Random;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WGChunk;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.utils.Direction;
import zombie.iso.worldgen.utils.SquareCoord;

public class AttachmentsHandler {
   public AttachmentsHandler() {
   }

   public void resetAttachments(IsoChunk var1) {
      var1.setAttachmentsDoneFull(false);
      var1.setAttachmentsState(0, false);
      var1.setAttachmentsState(1, false);
      var1.setAttachmentsState(2, false);
      var1.setAttachmentsState(3, false);
      var1.setAttachmentsState(4, false);
   }

   public void applyAttachments(IsoChunk var1) {
      int var2 = var1.wx * 8;
      int var3 = var1.wy * 8;
      int var4 = (var1.wx + 1) * 8;
      int var5 = (var1.wy + 1) * 8;
      WGChunk var6 = IsoWorld.instance.getWgChunk();
      int var7;
      int var8;
      int var9;
      int var16;
      int var17;
      IsoGridSquare var19;
      IsoObject var20;
      String var21;
      String var24;
      if (!var1.isAttachmentsDone(4)) {
         for(var7 = var2; var7 < var4; ++var7) {
            for(var8 = var3; var8 < var5; ++var8) {
               var9 = var7 - var2;
               int var10 = var8 - var3;
               Random var11 = WGParams.instance.getRandom(var7, var8);
               IsoGridSquare var12 = var1.getGridSquare(var9, var10, 0);
               if (var12 != null) {
                  IsoObject var13 = var12.getFloor();
                  if (var13 != null) {
                     String var14 = var13.getTile();
                     Direction[] var15 = Direction.values();
                     var16 = var15.length;

                     for(var17 = 0; var17 < var16; ++var17) {
                        Direction var18 = var15[var17];
                        if (var9 + var18.x < 8 && var9 + var18.x >= 0 && var10 + var18.y < 8 && var10 + var18.y >= 0) {
                           var19 = var1.getGridSquare(var9 + var18.x, var10 + var18.y, 0);
                           if (var19 != null) {
                              var20 = var19.getFloor();
                              if (var20 != null) {
                                 var21 = var20.getTile();
                                 if (!var6.areSimilar(var14, var21) && var6.priority(var21, var14)) {
                                    List var22 = var6.getAttachment(var21, var18);
                                    if (var22 != null) {
                                       TileGroup var23 = (TileGroup)var22.get(var11.nextInt(var22.size()));
                                       var24 = (String)var23.tiles().get(var11.nextInt(var23.tiles().size()));
                                       var13.addAttachedAnimSprite((IsoSprite)IsoSpriteManager.instance.NamedMap.get(var24));
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         var1.setAttachmentsState(4, true);
      }

      Direction[] var28 = Direction.values();
      var8 = var28.length;

      for(var9 = 0; var9 < var8; ++var9) {
         Direction var29 = var28[var9];
         if (!var1.isAttachmentsDone(var29.index)) {
            IsoCell var30 = IsoWorld.instance.CurrentCell;
            IsoChunk var31 = var30.getChunk(var1.wx + var29.x, var1.wy + var29.y);
            if (var31 != null && var31.bLoaded) {
               for(int var32 = 0; var32 < 8; ++var32) {
                  SquareCoord var10000;
                  switch (var29) {
                     case NORTH:
                        var10000 = new SquareCoord(var1.wx * 8 + var32, var1.wy * 8 + 0, 0);
                        break;
                     case SOUTH:
                        var10000 = new SquareCoord(var1.wx * 8 + var32, var1.wy * 8 + 7, 0);
                        break;
                     case WEST:
                        var10000 = new SquareCoord(var1.wx * 8 + 0, var1.wy * 8 + var32, 0);
                        break;
                     case EAST:
                        var10000 = new SquareCoord(var1.wx * 8 + 7, var1.wy * 8 + var32, 0);
                        break;
                     default:
                        throw new IncompatibleClassChangeError();
                  }

                  SquareCoord var33 = var10000;
                  switch (var29) {
                     case NORTH:
                        var10000 = new SquareCoord(var1.wx * 8 + var32, var1.wy * 8 - 1, 0);
                        break;
                     case SOUTH:
                        var10000 = new SquareCoord(var1.wx * 8 + var32, var1.wy * 8 + 8, 0);
                        break;
                     case WEST:
                        var10000 = new SquareCoord(var1.wx * 8 - 1, var1.wy * 8 + var32, 0);
                        break;
                     case EAST:
                        var10000 = new SquareCoord(var1.wx * 8 + 8, var1.wy * 8 + var32, 0);
                        break;
                     default:
                        throw new IncompatibleClassChangeError();
                  }

                  SquareCoord var34 = var10000;
                  var16 = var33.x() - var2;
                  var17 = var33.y() - var3;
                  Random var35 = WGParams.instance.getRandom(var33.x(), var33.y());
                  var19 = var1.getGridSquare(var16, var17, 0);
                  if (var19 != null) {
                     var20 = var19.getFloor();
                     if (var20 != null) {
                        var21 = var20.getTile();
                        IsoGridSquare var36 = var30.getGridSquare(var34.x(), var34.y(), 0);
                        if (var36 != null) {
                           IsoObject var37 = var36.getFloor();
                           if (var37 != null) {
                              var24 = var37.getTile();
                              if (!var6.areSimilar(var21, var24) && var6.priority(var24, var21)) {
                                 List var25 = var6.getAttachment(var24, var29);
                                 if (var25 != null) {
                                    TileGroup var26 = (TileGroup)var25.get(var35.nextInt(var25.size()));
                                    String var27 = (String)var26.tiles().get(var35.nextInt(var26.tiles().size()));
                                    var20.addAttachedAnimSprite((IsoSprite)IsoSpriteManager.instance.NamedMap.get(var27));
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               var1.setAttachmentsState(var29.index, true);
            }
         }
      }

      var1.setAttachmentsDoneFull(true);

      for(var7 = 0; var7 <= 4; ++var7) {
         if (!var1.isAttachmentsDone(var7)) {
            var1.setAttachmentsDoneFull(false);
         }
      }

   }
}
