package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public final class FBORenderObjectPicker {
   private static FBORenderObjectPicker instance;
   private final ObjectPool<IsoObjectPicker.ClickObject> m_clickObjectPool = new ObjectPool(IsoObjectPicker.ClickObject::new);
   private final ArrayList<IsoObject> m_objects = new ArrayList();
   private final ArrayList<IsoObjectPicker.ClickObject> m_clickObjects = new ArrayList();
   private final ArrayList<IsoObjectPicker.ClickObject> m_choices = new ArrayList();
   private final int[] m_leftSideXY = new int[]{0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3};
   private final int[] m_rightSideXY = new int[]{0, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3};

   public FBORenderObjectPicker() {
   }

   public static FBORenderObjectPicker getInstance() {
      if (instance == null) {
         instance = new FBORenderObjectPicker();
      }

      return instance;
   }

   public IsoObjectPicker.ClickObject ContextPick(int var1, int var2) {
      byte var3 = 0;
      float var4 = Core.getInstance().getZoom(var3);
      float var5 = (float)var1 * var4;
      float var6 = (float)var2 * var4;
      this.m_clickObjectPool.releaseAll(this.m_clickObjects);
      this.m_clickObjects.clear();
      this.getClickObjects(var1, var2, this.m_clickObjects);
      this.m_choices.clear();

      int var7;
      IsoObjectPicker.ClickObject var8;
      for(var7 = this.m_clickObjects.size() - 1; var7 >= 0; --var7) {
         var8 = (IsoObjectPicker.ClickObject)this.m_clickObjects.get(var7);
         IsoObject var9 = var8.tile;
         if (!(var9 instanceof IsoPlayer) || var9 != IsoPlayer.players[0]) {
            IsoSprite var10 = var9.sprite;
            float var11;
            if (var10 != null) {
               var11 = var9.getRenderInfo(var3).m_targetAlpha;
               if (var11 == 0.0F || (var10.Properties.Is(IsoFlagType.cutW) || var10.Properties.Is(IsoFlagType.cutN)) && !(var9 instanceof IsoWindow) && (!(var9 instanceof IsoThumpable) || !((IsoThumpable)var9).isDoor()) && var11 < 1.0F) {
                  continue;
               }
            }

            if (var5 > (float)var8.x && var6 > (float)var8.y && var5 <= (float)(var8.x + var8.width) && var6 <= (float)(var8.y + var8.height) && !(var9 instanceof IsoPlayer)) {
               if (var8.scaleX == 1.0F && var8.scaleY == 1.0F) {
                  if (var9.isMaskClicked((int)(var5 - (float)var8.x), (int)(var6 - (float)var8.y), var8.flip)) {
                     if (var9.rerouteMask != null) {
                        var8.tile = var9.rerouteMask;
                     }

                     var8.lx = PZMath.fastfloor(var5) - var8.x;
                     var8.ly = PZMath.fastfloor(var6) - var8.y;
                     this.m_choices.add(var8);
                  }
               } else {
                  var11 = (float)var8.x + (var5 - (float)var8.x) / var8.scaleX;
                  float var12 = (float)var8.y + (var6 - (float)var8.y) / var8.scaleY;
                  if (var9.isMaskClicked((int)(var11 - (float)var8.x), (int)(var12 - (float)var8.y), var8.flip)) {
                     if (var9.rerouteMask != null) {
                        var8.tile = var9.rerouteMask;
                     }

                     var8.lx = PZMath.fastfloor(var5) - var8.x;
                     var8.ly = PZMath.fastfloor(var6) - var8.y;
                     this.m_choices.add(var8);
                  }
               }
            }
         }
      }

      if (this.m_choices.isEmpty()) {
         return null;
      } else {
         for(var7 = 0; var7 < this.m_choices.size(); ++var7) {
            var8 = (IsoObjectPicker.ClickObject)this.m_choices.get(var7);
            var8.score = var8.calculateScore();
         }

         try {
            this.m_choices.sort(IsoObjectPicker.comp);
         } catch (IllegalArgumentException var13) {
            if (Core.bDebug) {
               ExceptionLogger.logException(var13);
            }

            return null;
         }

         IsoObjectPicker.ClickObject var14 = (IsoObjectPicker.ClickObject)this.m_choices.get(this.m_choices.size() - 1);
         return var14;
      }
   }

   public void getClickObjects(int var1, int var2, ArrayList<IsoObjectPicker.ClickObject> var3) {
      byte var4 = 0;
      float var5 = Core.getInstance().getZoom(var4);
      float var6 = (float)var1 * var5;
      float var7 = (float)var2 * var5;
      this.getObjectsAt((int)var6, (int)var7, this.m_objects);

      for(int var8 = 0; var8 < this.m_objects.size(); ++var8) {
         IsoObject var9 = (IsoObject)this.m_objects.get(var8);
         FBORenderLevels var10 = var9.getSquare().getChunk().getRenderLevels(var4);
         FBORenderChunk var11 = var10.getFBOForLevel(var9.getSquare().z, var5);
         if (var11 != null && !this.handleWaterShader(var9, var3)) {
            ObjectRenderInfo var12 = var9.getRenderInfo(var4);
            if (var12.m_bCutaway) {
               if (!(var9 instanceof IsoWindowFrame)) {
                  continue;
               }

               IsoWindowFrame var13 = (IsoWindowFrame)var9;
               if (var13.hasWindow()) {
                  continue;
               }
            }

            IsoObjectPicker.ClickObject var14 = (IsoObjectPicker.ClickObject)this.m_clickObjectPool.alloc();
            var14.tile = var9;
            var14.square = var9.getSquare();
            if (var12.m_layer == ObjectRenderLayer.Translucent) {
               var14.x = (int)var12.m_renderX;
               var14.y = (int)var12.m_renderY;
            } else if (DebugOptions.instance.FBORenderChunk.CombinedFBO.getValue()) {
               var14.x = (int)(var11.m_renderX + var12.m_renderX);
               var14.y = (int)(var11.m_renderY + var12.m_renderY);
            } else {
               var14.x = (int)(var11.m_renderX * var5 + var12.m_renderX);
               var14.y = (int)(var11.m_renderY * var5 + var12.m_renderY);
            }

            var14.width = (int)var12.m_renderWidth;
            var14.height = (int)var12.m_renderHeight;
            var14.scaleX = (float)((int)var12.m_renderScaleX);
            var14.scaleY = (float)((int)var12.m_renderScaleY);
            var14.flip = false;
            var14.score = 0;
            var3.add(var14);
         }
      }

      var3.sort((var0, var1x) -> {
         int var2 = var0.square.z - var1x.square.z;
         if (var2 != 0) {
            return var2;
         } else {
            var2 = compareRenderLayer(var0, var1x);
            return var2 != 0 ? var2 : compareSquare(var0, var1x);
         }
      });
   }

   boolean handleWaterShader(IsoObject var1, ArrayList<IsoObjectPicker.ClickObject> var2) {
      byte var3 = 0;
      if (var1.getRenderInfo(var3).m_layer != ObjectRenderLayer.None) {
         return false;
      } else if (var1.sprite != null && var1.sprite.getProperties().Is(IsoFlagType.water)) {
         IsoGridSquare var4 = var1.square;
         IsoObjectPicker.ClickObject var5 = (IsoObjectPicker.ClickObject)this.m_clickObjectPool.alloc();
         var5.tile = var1;
         var5.square = var4;
         var5.x = (int)(IsoUtils.XToScreen((float)var4.x, (float)var4.y, (float)var4.z, 0) - IsoCamera.frameState.OffX - var1.offsetX);
         var5.y = (int)(IsoUtils.YToScreen((float)var4.x, (float)var4.y, (float)var4.z, 0) - IsoCamera.frameState.OffY - var1.offsetY);
         var5.width = 64 * Core.TileScale;
         var5.height = 128 * Core.TileScale;
         var5.scaleX = 1.0F;
         var5.scaleY = 1.0F;
         var5.flip = false;
         var5.score = 0;
         var2.add(var5);
         return true;
      } else {
         return false;
      }
   }

   static int compareRenderLayer(IsoObjectPicker.ClickObject var0, IsoObjectPicker.ClickObject var1) {
      return renderLayerIndex(var0) - renderLayerIndex(var1);
   }

   static int renderLayerIndex(IsoObjectPicker.ClickObject var0) {
      short var10000;
      switch (var0.tile.getRenderInfo(0).m_layer) {
         case None:
            var10000 = 1000;
            break;
         case Floor:
         case TranslucentFloor:
            var10000 = 0;
            break;
         case Vegetation:
            var10000 = 1;
            break;
         case Corpse:
            var10000 = 2;
            break;
         case MinusFloor:
         case Translucent:
            var10000 = 3;
            break;
         case WorldInventoryObject:
            var10000 = 4;
            break;
         case MinusFloorSE:
            var10000 = 5;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   static int compareSquare(IsoObjectPicker.ClickObject var0, IsoObjectPicker.ClickObject var1) {
      int var2 = var0.square.x + var0.square.y * 100000;
      int var3 = var1.square.x + var1.square.y * 100000;
      return var2 - var3;
   }

   void getObjectsAt(int var1, int var2, ArrayList<IsoObject> var3) {
      var3.clear();
      byte var4 = 0;
      IsoPlayer var5 = IsoPlayer.players[var4];
      int var6;
      float var7;
      float var8;
      boolean var9;
      int[] var10;
      int var11;
      if (var5.getZ() < 0.0F) {
         for(var6 = -32; var6 < 0; ++var6) {
            var7 = IsoUtils.XToIso((float)var1, (float)var2, (float)var6);
            var8 = IsoUtils.YToIso((float)var1, (float)var2, (float)var6);
            var9 = var7 % 1.0F > var8 % 1.0F;
            var10 = var9 ? this.m_rightSideXY : this.m_leftSideXY;

            for(var11 = 0; var11 < var10.length; var11 += 2) {
               this.getObjectsOnSquare((int)var7 + var10[var11], (int)var8 + var10[var11 + 1], var6, var3);
            }

            this.getCorpsesNear(PZMath.fastfloor(var7), PZMath.fastfloor(var8), var6, var3, var9, var1, var2);
         }

      } else {
         for(var6 = 0; var6 <= 31; ++var6) {
            var7 = IsoUtils.XToIso((float)var1, (float)var2, (float)var6);
            var8 = IsoUtils.YToIso((float)var1, (float)var2, (float)var6);
            var9 = PZMath.coordmodulof(var7, 1) > PZMath.coordmodulof(var8, 1);
            var10 = var9 ? this.m_rightSideXY : this.m_leftSideXY;

            for(var11 = 0; var11 < var10.length; var11 += 2) {
               this.getObjectsOnSquare(PZMath.fastfloor(var7) + var10[var11], PZMath.fastfloor(var8) + var10[var11 + 1], var6, var3);
            }

            this.getCorpsesNear(PZMath.fastfloor(var7), PZMath.fastfloor(var8), var6, var3, var9, var1, var2);
         }

      }
   }

   void getObjectsOnSquare(int var1, int var2, int var3, ArrayList<IsoObject> var4) {
      IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var5 != null) {
         byte var6 = 0;
         IsoObject[] var7 = (IsoObject[])var5.getObjects().getElements();
         int var8 = var5.getObjects().size();

         for(int var9 = 0; var9 < var8; ++var9) {
            IsoObject var10 = var7[var9];
            ObjectRenderInfo var11 = var10.getRenderInfo(var6);
            if (var11.m_layer != ObjectRenderLayer.None && var11.m_targetAlpha != 0.0F) {
               if (!(var11.m_renderWidth <= 0.0F) && !(var11.m_renderHeight <= 0.0F)) {
                  var4.add(var10);
               }
            } else if (var10.sprite != null && var10.sprite.getProperties().Is(IsoFlagType.water)) {
               var4.add(var10);
            }
         }

      }
   }

   void getCorpsesNear(int var1, int var2, int var3, ArrayList<IsoObject> var4, boolean var5, int var6, int var7) {
      for(int var8 = -1; var8 <= 1; ++var8) {
         for(int var9 = -1; var9 <= 1; ++var9) {
            this.getCorpsesNear(var1 + var9, var2 + var8, var3, var4, var6, var7);
         }
      }

   }

   void getCorpsesNear(int var1, int var2, int var3, ArrayList<IsoObject> var4, int var5, int var6) {
      IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var7 != null) {
         ArrayList var8 = var7.getStaticMovingObjects();

         for(int var9 = 0; var9 < var8.size(); ++var9) {
            IsoDeadBody var10 = (IsoDeadBody)Type.tryCastTo((IsoMovingObject)var8.get(var9), IsoDeadBody.class);
            if (var10 != null && var10.isMouseOver((float)var5, (float)var6)) {
               var4.add(var10);
            }
         }

      }
   }

   IsoObject getFirst(int var1, int var2, IObjectPickerPredicate var3) {
      byte var4 = 0;
      float var5 = Core.getInstance().getZoom(var4);
      float var6 = (float)var1 * var5;
      float var7 = (float)var2 * var5;
      this.m_clickObjectPool.releaseAll(this.m_clickObjects);
      this.m_clickObjects.clear();
      this.getClickObjects(var1, var2, this.m_clickObjects);

      for(int var8 = this.m_clickObjects.size() - 1; var8 >= 0; --var8) {
         IsoObjectPicker.ClickObject var9 = (IsoObjectPicker.ClickObject)this.m_clickObjects.get(var8);
         int var10 = var3.test(var9, var6, var7);
         if (var10 == -1) {
            return null;
         }

         if (var10 == 1) {
            return var9.tile;
         }
      }

      return null;
   }

   public IsoObject PickDoor(int var1, int var2, boolean var3) {
      return this.getFirst(var1, var2, (var1x, var2x, var3x) -> {
         byte var4 = 0;
         if (!(var1x.tile instanceof IsoDoor)) {
            return 0;
         } else if (var1x.tile.getRenderInfo(var4).m_targetAlpha == 0.0F) {
            return 0;
         } else if (var3 != var1x.tile.getRenderInfo(var4).m_targetAlpha < 1.0F) {
            return 0;
         } else {
            if (var1x.contains(var2x, var3x)) {
               int var5 = PZMath.fastfloor(var2x - (float)var1x.x);
               int var6 = PZMath.fastfloor(var3x - (float)var1x.y);
               if (var1x.tile.isMaskClicked(var5, var6, var1x.flip)) {
                  return 1;
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickWindow(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (!(var0.tile instanceof IsoWindow) && !(var0.tile instanceof IsoCurtain)) {
            return 0;
         } else if (var0.tile.sprite != null && var0.tile.getRenderInfo(var3).m_targetAlpha == 0.0F) {
            return 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               int var4 = PZMath.fastfloor(var1x - (float)var0.x);
               int var5 = PZMath.fastfloor(var2x - (float)var0.y);
               if (var0.tile.isMaskClicked(var4, var5, var0.flip)) {
                  return 1;
               }

               if (var0.tile instanceof IsoWindow) {
                  boolean var6 = false;
                  boolean var7 = false;

                  int var8;
                  for(var8 = var5; var8 >= 0; --var8) {
                     if (var0.tile.isMaskClicked(var4, var8)) {
                        var6 = true;
                        break;
                     }
                  }

                  for(var8 = var5; var8 < var0.height; ++var8) {
                     if (var0.tile.isMaskClicked(var4, var8)) {
                        var7 = true;
                        break;
                     }
                  }

                  if (var6 && var7) {
                     return 1;
                  }
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickWindowFrame(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (!(var0.tile instanceof IsoWindowFrame)) {
            return 0;
         } else if (var0.tile.sprite != null && var0.tile.getRenderInfo(var3).m_targetAlpha == 0.0F) {
            return 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               int var4 = PZMath.fastfloor(var1x - (float)var0.x);
               int var5 = PZMath.fastfloor(var2x - (float)var0.y);
               if (var0.tile.isMaskClicked(var4, var5, var0.flip)) {
                  return 1;
               }

               boolean var6 = false;
               boolean var7 = false;

               int var8;
               for(var8 = var5; var8 >= 0; --var8) {
                  if (var0.tile.isMaskClicked(var4, var8)) {
                     var6 = true;
                     break;
                  }
               }

               for(var8 = var5; var8 < var0.height; ++var8) {
                  if (var0.tile.isMaskClicked(var4, var8)) {
                     var7 = true;
                     break;
                  }
               }

               if (var6 && var7) {
                  return 1;
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickThumpable(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (!(var0.tile instanceof IsoThumpable)) {
            return 0;
         } else if (var0.tile.sprite != null && var0.tile.getRenderInfo(var3).m_targetAlpha == 0.0F) {
            return 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               int var4 = (int)(var1x - (float)var0.x);
               int var5 = (int)(var2x - (float)var0.y);
               if (var0.tile.isMaskClicked(var4, var5, var0.flip)) {
                  return 1;
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickHoppable(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (!var0.tile.isHoppable()) {
            return 0;
         } else if (var0.tile.sprite != null && var0.tile.getRenderInfo(var3).m_targetAlpha == 0.0F) {
            return 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               int var4 = (int)(var1x - (float)var0.x);
               int var5 = (int)(var2x - (float)var0.y);
               if (var0.tile.isMaskClicked(var4, var5, var0.flip)) {
                  return 1;
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickCorpse(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (var0.tile instanceof IsoDeadBody) {
            return ((IsoDeadBody)var0.tile).isMouseOver(var1x, var2x) ? 1 : 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               if (var0.tile.getRenderInfo(var3).m_targetAlpha < 1.0F) {
                  return 0;
               }

               if (var0.tile.isMaskClicked((int)(var1x - (float)var0.x), (int)(var2x - (float)var0.y), var0.flip) && !(var0.tile instanceof IsoWindow)) {
                  return -1;
               }
            }

            return 0;
         }
      });
   }

   public IsoObject PickTree(int var1, int var2) {
      return this.getFirst(var1, var2, (var0, var1x, var2x) -> {
         byte var3 = 0;
         if (!(var0.tile instanceof IsoTree)) {
            return 0;
         } else if (var0.tile.sprite != null && var0.tile.getRenderInfo(var3).m_targetAlpha == 0.0F) {
            return 0;
         } else {
            if (var0.contains(var1x, var2x)) {
               int var4 = (int)(var1x - (float)var0.x);
               int var5 = (int)(var2x - (float)var0.y);
               if (var0.tile.isMaskClicked(var4, var5, var0.flip)) {
                  return 1;
               }
            }

            return 0;
         }
      });
   }

   public interface IObjectPickerPredicate {
      int test(IsoObjectPicker.ClickObject var1, float var2, float var3);
   }
}
