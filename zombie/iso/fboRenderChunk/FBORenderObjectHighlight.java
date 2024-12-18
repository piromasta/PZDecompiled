package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.characters.IsoPlayer;
import zombie.core.PerformanceSettings;
import zombie.debug.DebugOptions;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;

public final class FBORenderObjectHighlight {
   private static FBORenderObjectHighlight instance;
   private boolean m_bRendering = false;
   private boolean m_bRenderingGhostTile = false;
   private final HashSet<IsoObject> m_objectSet = new HashSet();
   private final HashMap<IsoObject, Byte> m_objectHighlightRendered = new HashMap();
   private final ArrayList<IsoObject> m_objectList = new ArrayList();

   public FBORenderObjectHighlight() {
   }

   public static FBORenderObjectHighlight getInstance() {
      if (instance == null) {
         instance = new FBORenderObjectHighlight();
      }

      return instance;
   }

   public boolean isRendering() {
      return this.m_bRendering;
   }

   public void setRenderingGhostTile(boolean var1) {
      this.m_bRenderingGhostTile = var1;
   }

   public boolean isRenderingGhostTile() {
      return this.m_bRenderingGhostTile;
   }

   public void registerObject(IsoObject var1) {
      if (!(var1 instanceof IsoDeadBody)) {
         this.m_objectHighlightRendered.put(var1, (byte)15);
         if (!this.m_objectSet.contains(var1)) {
            this.m_objectSet.add(var1);
            if (this.isRenderedToChunkTexture(var1)) {
               var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
            }

         }
      }
   }

   public void unregisterObject(IsoObject var1) {
      boolean var2 = this.m_objectSet.remove(var1);
      if (var2) {
         this.m_objectHighlightRendered.remove(var1);
         if (this.isRenderedToChunkTexture(var1)) {
            var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
         }

      }
   }

   private boolean isRenderedToChunkTexture(IsoObject var1) {
      if (var1 instanceof IsoDeadBody) {
         return DebugOptions.instance.FBORenderChunk.CorpsesInChunkTexture.getValue();
      } else {
         return var1 instanceof IsoMannequin;
      }
   }

   public void render(int var1) {
      this.m_objectList.clear();
      this.m_objectList.addAll(this.m_objectSet);
      FBORenderCell.instance.bRenderTranslucentOnly = true;
      this.m_bRendering = true;

      for(int var2 = 0; var2 < this.m_objectList.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.m_objectList.get(var2);
         if (!this.isRenderedToChunkTexture(var3)) {
            if (var3.getObjectIndex() == -1) {
               this.m_objectSet.remove(var3);
               this.m_objectHighlightRendered.remove(var3);
            } else if ((var3.highlightFlags & 1) == 0) {
               this.m_objectSet.remove(var3);
               this.m_objectHighlightRendered.remove(var3);
            } else {
               byte var4 = (Byte)this.m_objectHighlightRendered.getOrDefault(var3, (byte)0);
               if ((var3.highlightFlags & 2) == 0 || (var4 & 1 << var1) != 0) {
                  this.renderObject(var1, var3);
                  int var5 = 0;

                  for(int var6 = 0; var6 < IsoPlayer.numPlayers; ++var6) {
                     var5 |= 1 << var6;
                  }

                  var4 = (byte)(var4 & var5);
                  var4 = (byte)(var4 & ~((byte)(1 << var1)));
                  this.m_objectHighlightRendered.put(var3, var4);
               }
            }
         }
      }

      this.renderSafehouses(var1);
      FBORenderCell.instance.bRenderTranslucentOnly = false;
      this.m_bRendering = false;
   }

   public void clearHighlightOnceFlag() {
      this.m_objectList.clear();
      this.m_objectList.addAll(this.m_objectSet);

      for(int var1 = 0; var1 < this.m_objectList.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.m_objectList.get(var1);
         if ((var2.highlightFlags & 2) != 0) {
            byte var3 = (Byte)this.m_objectHighlightRendered.getOrDefault(var2, (byte)0);
            if (var3 == 0) {
               var2.highlightFlags &= -2;
               this.m_objectSet.remove(var2);
               this.m_objectHighlightRendered.remove(var2);
            }
         }
      }

   }

   private void renderObject(int var1, IsoObject var2) {
      ObjectRenderInfo var3 = var2.getRenderInfo(var1);
      if (var3.m_layer != ObjectRenderLayer.None && !(var3.m_targetAlpha <= 0.0F)) {
         IsoObject[] var4 = (IsoObject[])var2.square.getObjects().getElements();
         int var5 = var2.square.getObjects().size();

         for(int var6 = var2.getObjectIndex(); var6 < var5; ++var6) {
            IsoObject var7 = var4[var6];
            if (!this.isRenderedToChunkTexture(var7) && (!(var7 instanceof IsoWorldInventoryObject) || !DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue())) {
               var3 = var7.getRenderInfo(var1);
               if (var3.m_layer != ObjectRenderLayer.None && !(var3.m_targetAlpha <= 0.0F)) {
                  if (var3.m_layer == ObjectRenderLayer.Floor) {
                     FBORenderCell.instance.renderFloor(var7);
                  } else {
                     FBORenderCell.instance.renderTranslucent(var7);
                     if (var7.hasAnimatedAttachments()) {
                        FBORenderCell.instance.renderAnimatedAttachments(var7);
                     }
                  }
               }
            }
         }

      }
   }

   private void renderDesignationZones(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      boolean var3 = var2 != null && var2.isSeeDesignationZone();
      if (var3) {
         Double var4 = var2.getSelectedZoneForHighlight();
         IsoChunkMap var5 = IsoWorld.instance.CurrentCell.getChunkMap(var1);

         for(int var6 = 0; var6 < DesignationZone.allZones.size(); ++var6) {
            DesignationZone var7 = (DesignationZone)DesignationZone.allZones.get(var6);
            if (var7.x + var7.w >= var5.getWorldXMinTiles() && var7.x < var5.getWorldXMaxTiles() && var7.y + var7.h > var5.getWorldYMinTiles() && var7.y < var5.getWorldYMaxTiles()) {
               for(int var8 = var7.y; var8 < var7.y + var7.h; ++var8) {
                  for(int var9 = var7.x; var9 < var7.x + var7.w; ++var9) {
                     IsoGridSquare var10 = IsoWorld.instance.getCell().getGridSquare(var9, var8, var7.z);
                     if (var10 != null && !var10.getObjects().isEmpty()) {
                        if (var10.getFloor() != null) {
                           var10.getFloor().setHighlighted(true);
                           if (var4 > 0.0 && var7.getId().intValue() == var4.intValue()) {
                              var10.getFloor().setHighlightColor(DesignationZoneAnimal.ZONESELECTEDCOLORR, DesignationZoneAnimal.ZONESELECTEDCOLORG, DesignationZoneAnimal.ZONESELECTEDCOLORB, 0.8F);
                           } else {
                              var10.getFloor().setHighlightColor(DesignationZoneAnimal.ZONECOLORR, DesignationZoneAnimal.ZONECOLORG, DesignationZoneAnimal.ZONECOLORB, 0.8F);
                           }
                        }

                        this.renderObject(var1, (IsoObject)var10.getObjects().get(0));

                        for(int var11 = 0; var11 < var10.getObjects().size(); ++var11) {
                           IsoObject var12 = (IsoObject)var10.getObjects().get(var11);
                           var12.highlightFlags &= -2;
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private void renderSafehouses(int var1) {
   }

   public boolean shouldRenderObjectHighlight(IsoObject var1) {
      if (var1 == null) {
         return false;
      } else if (var1.isHighlighted()) {
         return PerformanceSettings.FBORenderChunk ? this.isRendering() : true;
      } else {
         return false;
      }
   }
}
