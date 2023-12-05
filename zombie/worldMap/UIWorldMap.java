package zombie.worldMap;

import java.util.ArrayList;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.inventory.types.MapItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.worldMap.editor.WorldMapEditorState;
import zombie.worldMap.markers.WorldMapGridSquareMarker;
import zombie.worldMap.markers.WorldMapMarkers;
import zombie.worldMap.markers.WorldMapMarkersV1;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapStyleV1;
import zombie.worldMap.symbols.MapSymbolDefinitions;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapSymbolsV1;

public class UIWorldMap extends UIElement {
   static final ArrayList<WorldMapFeature> s_tempFeatures = new ArrayList();
   protected final WorldMap m_worldMap = new WorldMap();
   protected final WorldMapStyle m_style = new WorldMapStyle();
   protected final WorldMapRenderer m_renderer = new WorldMapRenderer();
   protected final WorldMapMarkers m_markers = new WorldMapMarkers();
   protected WorldMapSymbols m_symbols = null;
   protected final WorldMapStyleLayer.RGBAf m_color = (new WorldMapStyleLayer.RGBAf()).init(0.85882354F, 0.84313726F, 0.7529412F, 1.0F);
   protected final UIWorldMapV1 m_APIv1 = new UIWorldMapV1(this);
   private boolean m_dataWasReady = false;
   private final ArrayList<BuildingDef> m_buildingsWithoutFeatures = new ArrayList();
   private boolean m_bBuildingsWithoutFeatures = false;

   public UIWorldMap(KahluaTable var1) {
      super(var1);
   }

   public UIWorldMapV1 getAPI() {
      return this.m_APIv1;
   }

   public UIWorldMapV1 getAPIv1() {
      return this.m_APIv1;
   }

   protected void setMapItem(MapItem var1) {
      this.m_symbols = var1.getSymbols();
   }

   public void render() {
      if (this.isVisible()) {
         if (this.Parent == null || this.Parent.getMaxDrawHeight() == -1.0 || !(this.Parent.getMaxDrawHeight() <= this.getY())) {
            this.DrawTextureScaledColor((Texture)null, 0.0, 0.0, this.getWidth(), this.getHeight(), (double)this.m_color.r, (double)this.m_color.g, (double)this.m_color.b, (double)this.m_color.a);
            if (!this.m_worldMap.hasData()) {
            }

            this.setStencilRect(0.0, 0.0, this.getWidth(), this.getHeight());
            this.m_renderer.setMap(this.m_worldMap, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue(), this.getWidth().intValue(), this.getHeight().intValue());
            this.m_renderer.updateView();
            float var1 = this.m_renderer.getDisplayZoomF();
            float var2 = this.m_renderer.getCenterWorldX();
            float var3 = this.m_renderer.getCenterWorldY();
            this.m_APIv1.getWorldScale(var1);
            if (this.m_renderer.getBoolean("HideUnvisited") && WorldMapVisited.getInstance() != null) {
               this.m_renderer.setVisited(WorldMapVisited.getInstance());
            } else {
               this.m_renderer.setVisited((WorldMapVisited)null);
            }

            this.m_renderer.render(this);
            if (this.m_renderer.getBoolean("Symbols")) {
               this.m_symbols.render(this);
            }

            this.m_markers.render(this);
            this.renderLocalPlayers();
            this.renderRemotePlayers();
            int var5 = TextManager.instance.getFontHeight(UIFont.Small);
            float var6;
            float var7;
            double var8;
            int var16;
            if (Core.bDebug && this.m_renderer.getBoolean("DebugInfo")) {
               this.DrawTextureScaledColor((Texture)null, 0.0, 0.0, 200.0, (double)var5 * 4.0, 1.0, 1.0, 1.0, 1.0);
               var6 = this.m_APIv1.mouseToWorldX();
               var7 = this.m_APIv1.mouseToWorldY();
               var8 = 0.0;
               double var10 = 0.0;
               double var12 = 0.0;
               double var14 = 1.0;
               var16 = 0;
               this.DrawText("SQUARE = " + (int)var6 + "," + (int)var7, 0.0, (double)var16, var8, var10, var12, var14);
               var16 += var5;
               this.DrawText("CELL = " + (int)(var6 / 300.0F) + "," + (int)(var7 / 300.0F), 0.0, (double)var5, var8, var10, var12, var14);
               var16 += var5;
               this.DrawText("ZOOM = " + this.m_renderer.getDisplayZoomF(), 0.0, (double)var16, var8, var10, var12, var14);
               var16 += var5;
               WorldMapRenderer var10001 = this.m_renderer;
               this.DrawText("SCALE = " + var10001.getWorldScale(this.m_renderer.getZoomF()), 0.0, (double)var16, var8, var10, var12, var14);
               int var10000 = var16 + var5;
            }

            this.clearStencilRect();
            this.repaintStencilRect(0.0, 0.0, (double)this.width, (double)this.height);
            if (Core.bDebug && DebugOptions.instance.UIRenderOutline.getValue()) {
               Double var23 = -this.getXScroll();
               Double var24 = -this.getYScroll();
               var8 = this.isMouseOver() ? 0.0 : 1.0;
               this.DrawTextureScaledColor((Texture)null, var23, var24, 1.0, (double)this.height, var8, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var23 + 1.0, var24, (double)this.width - 2.0, 1.0, var8, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var23 + (double)this.width - 1.0, var24, 1.0, (double)this.height, var8, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var23 + 1.0, var24 + (double)this.height - 1.0, (double)this.width - 2.0, 1.0, var8, 1.0, 1.0, 0.5);
            }

            if (Core.bDebug && this.m_renderer.getBoolean("HitTest")) {
               var6 = this.m_APIv1.mouseToWorldX();
               var7 = this.m_APIv1.mouseToWorldY();
               s_tempFeatures.clear();
               Iterator var25 = this.m_worldMap.m_data.iterator();

               while(var25.hasNext()) {
                  WorldMapData var9 = (WorldMapData)var25.next();
                  if (var9.isReady()) {
                     var9.hitTest(var6, var7, s_tempFeatures);
                  }
               }

               if (!s_tempFeatures.isEmpty()) {
                  WorldMapFeature var26 = (WorldMapFeature)s_tempFeatures.get(s_tempFeatures.size() - 1);
                  int var27 = var26.m_cell.m_x * 300;
                  int var28 = var26.m_cell.m_y * 300;
                  int var11 = this.getAbsoluteX().intValue();
                  int var29 = this.getAbsoluteY().intValue();
                  WorldMapPoints var13 = (WorldMapPoints)((WorldMapGeometry)var26.m_geometries.get(0)).m_points.get(0);

                  for(int var30 = 0; var30 < var13.numPoints(); ++var30) {
                     int var15 = var13.getX(var30);
                     var16 = var13.getY(var30);
                     int var17 = var13.getX((var30 + 1) % var13.numPoints());
                     int var18 = var13.getY((var30 + 1) % var13.numPoints());
                     float var19 = this.m_APIv1.worldToUIX((float)(var27 + var15), (float)(var28 + var16));
                     float var20 = this.m_APIv1.worldToUIY((float)(var27 + var15), (float)(var28 + var16));
                     float var21 = this.m_APIv1.worldToUIX((float)(var27 + var17), (float)(var28 + var18));
                     float var22 = this.m_APIv1.worldToUIY((float)(var27 + var17), (float)(var28 + var18));
                     SpriteRenderer.instance.renderline((Texture)null, var11 + (int)var19, var29 + (int)var20, var11 + (int)var21, var29 + (int)var22, 1.0F, 0.0F, 0.0F, 1.0F);
                  }
               }
            }

            if (Core.bDebug && this.m_renderer.getBoolean("BuildingsWithoutFeatures") && !this.m_renderer.getBoolean("Isometric")) {
               this.renderBuildingsWithoutFeatures();
            }

            super.render();
         }
      }
   }

   private void renderLocalPlayers() {
      if (this.m_renderer.getBoolean("Players")) {
         float var1 = this.m_renderer.getDisplayZoomF();
         if (!(var1 >= 20.0F)) {
            for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
               IsoPlayer var3 = IsoPlayer.players[var2];
               if (var3 != null && !var3.isDead()) {
                  float var4 = var3.x;
                  float var5 = var3.y;
                  if (var3.getVehicle() != null) {
                     var4 = var3.getVehicle().getX();
                     var5 = var3.getVehicle().getY();
                  }

                  this.renderPlayer(var4, var5);
                  if (GameClient.bClient) {
                     this.renderPlayerName(var4, var5, var3.getUsername());
                  }
               }
            }

         }
      }
   }

   private void renderRemotePlayers() {
      if (GameClient.bClient) {
         if (this.m_renderer.getBoolean("Players")) {
            if (this.m_renderer.getBoolean("RemotePlayers")) {
               ArrayList var1 = WorldMapRemotePlayers.instance.getPlayers();

               for(int var2 = 0; var2 < var1.size(); ++var2) {
                  WorldMapRemotePlayer var3 = (WorldMapRemotePlayer)var1.get(var2);
                  if (this.shouldShowRemotePlayer(var3)) {
                     this.renderPlayer(var3.getX(), var3.getY());
                     this.renderPlayerName(var3.getX(), var3.getY(), var3.getUsername());
                  }
               }

            }
         }
      }
   }

   private boolean shouldShowRemotePlayer(WorldMapRemotePlayer var1) {
      if (!var1.hasFullData()) {
         return false;
      } else if (var1.isInvisible()) {
         return this.isAdminSeeRemotePlayers();
      } else if (ServerOptions.getInstance().MapRemotePlayerVisibility.getValue() == 3) {
         return true;
      } else if (this.isAdminSeeRemotePlayers()) {
         return true;
      } else if (ServerOptions.getInstance().MapRemotePlayerVisibility.getValue() == 1) {
         return false;
      } else {
         for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
            IsoPlayer var3 = IsoPlayer.players[var2];
            if (var3 != null) {
               if (this.isInSameFaction(var3, var1)) {
                  return true;
               }

               if (this.isInSameSafehouse(var3, var1)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isAdminSeeRemotePlayers() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         IsoPlayer var2 = IsoPlayer.players[var1];
         if (var2 != null && !var2.isAccessLevel("none")) {
            return true;
         }
      }

      return false;
   }

   private boolean isInSameFaction(IsoPlayer var1, WorldMapRemotePlayer var2) {
      Faction var3 = Faction.getPlayerFaction(var1);
      Faction var4 = Faction.getPlayerFaction(var2.getUsername());
      return var3 != null && var3 == var4;
   }

   private boolean isInSameSafehouse(IsoPlayer var1, WorldMapRemotePlayer var2) {
      ArrayList var3 = SafeHouse.getSafehouseList();

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         SafeHouse var5 = (SafeHouse)var3.get(var4);
         if (var5.playerAllowed(var1.getUsername()) && var5.playerAllowed(var2.getUsername())) {
            return true;
         }
      }

      return false;
   }

   private void renderPlayer(float var1, float var2) {
      float var3 = this.m_renderer.getDisplayZoomF();
      float var4 = this.m_renderer.getCenterWorldX();
      float var5 = this.m_renderer.getCenterWorldY();
      float var6 = this.m_APIv1.worldToUIX(var1, var2, var3, var4, var5, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
      float var7 = this.m_APIv1.worldToUIY(var1, var2, var3, var4, var5, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
      var6 = PZMath.floor(var6);
      var7 = PZMath.floor(var7);
      this.DrawTextureScaledColor((Texture)null, (double)var6 - 3.0, (double)var7 - 3.0, 6.0, 6.0, 1.0, 0.0, 0.0, 1.0);
   }

   private void renderPlayerName(float var1, float var2, String var3) {
      if (this.m_renderer.getBoolean("PlayerNames")) {
         if (!StringUtils.isNullOrWhitespace(var3)) {
            float var4 = this.m_renderer.getDisplayZoomF();
            float var5 = this.m_renderer.getCenterWorldX();
            float var6 = this.m_renderer.getCenterWorldY();
            float var7 = this.m_APIv1.worldToUIX(var1, var2, var4, var5, var6, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
            float var8 = this.m_APIv1.worldToUIY(var1, var2, var4, var5, var6, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
            var7 = PZMath.floor(var7);
            var8 = PZMath.floor(var8);
            int var9 = TextManager.instance.MeasureStringX(UIFont.Small, var3) + 16;
            int var10 = TextManager.instance.font.getLineHeight();
            int var11 = (int)Math.ceil((double)var10 * 1.25);
            this.DrawTextureScaledColor((Texture)null, (double)var7 - (double)var9 / 2.0, (double)var8 + 4.0, (double)var9, (double)var11, 0.5, 0.5, 0.5, 0.5);
            this.DrawTextCentre(var3, (double)var7, (double)(var8 + 4.0F) + (double)(var11 - var10) / 2.0, 0.0, 0.0, 0.0, 1.0);
         }
      }
   }

   public void update() {
      super.update();
   }

   public Boolean onMouseDown(double var1, double var3) {
      if (GameKeyboard.isKeyDown(42)) {
         this.m_renderer.resetView();
      }

      return super.onMouseDown(var1, var3);
   }

   public Boolean onMouseUp(double var1, double var3) {
      return super.onMouseUp(var1, var3);
   }

   public void onMouseUpOutside(double var1, double var3) {
      super.onMouseUpOutside(var1, var3);
   }

   public Boolean onMouseMove(double var1, double var3) {
      return super.onMouseMove(var1, var3);
   }

   public Boolean onMouseWheel(double var1) {
      return super.onMouseWheel(var1);
   }

   public static void setExposed(LuaManager.Exposer var0) {
      var0.setExposed(MapItem.class);
      var0.setExposed(MapSymbolDefinitions.class);
      var0.setExposed(MapSymbolDefinitions.MapSymbolDefinition.class);
      var0.setExposed(UIWorldMap.class);
      var0.setExposed(UIWorldMapV1.class);
      var0.setExposed(WorldMapGridSquareMarker.class);
      var0.setExposed(WorldMapMarkers.class);
      var0.setExposed(WorldMapRenderer.WorldMapBooleanOption.class);
      var0.setExposed(WorldMapRenderer.WorldMapDoubleOption.class);
      var0.setExposed(WorldMapVisited.class);
      WorldMapMarkersV1.setExposed(var0);
      WorldMapStyleV1.setExposed(var0);
      WorldMapSymbolsV1.setExposed(var0);
      var0.setExposed(WorldMapEditorState.class);
      var0.setExposed(WorldMapSettings.class);
   }

   private void renderBuildingsWithoutFeatures() {
      if (this.m_bBuildingsWithoutFeatures) {
         Iterator var12 = this.m_buildingsWithoutFeatures.iterator();

         while(var12.hasNext()) {
            BuildingDef var13 = (BuildingDef)var12.next();
            this.debugRenderBuilding(var13, 1.0F, 0.0F, 0.0F, 1.0F);
         }

      } else {
         this.m_bBuildingsWithoutFeatures = true;
         this.m_buildingsWithoutFeatures.clear();
         IsoMetaGrid var1 = IsoWorld.instance.MetaGrid;

         for(int var2 = 0; var2 < var1.Buildings.size(); ++var2) {
            BuildingDef var3 = (BuildingDef)var1.Buildings.get(var2);
            boolean var4 = false;

            for(int var5 = 0; var5 < var3.rooms.size(); ++var5) {
               RoomDef var6 = (RoomDef)var3.rooms.get(var5);
               if (var6.level <= 0) {
                  ArrayList var7 = var6.getRects();

                  for(int var8 = 0; var8 < var7.size(); ++var8) {
                     RoomDef.RoomRect var9 = (RoomDef.RoomRect)var7.get(var8);
                     s_tempFeatures.clear();
                     Iterator var10 = this.m_worldMap.m_data.iterator();

                     while(var10.hasNext()) {
                        WorldMapData var11 = (WorldMapData)var10.next();
                        if (var11.isReady()) {
                           var11.hitTest((float)var9.x + (float)var9.w / 2.0F, (float)var9.y + (float)var9.h / 2.0F, s_tempFeatures);
                        }
                     }

                     if (!s_tempFeatures.isEmpty()) {
                        var4 = true;
                        break;
                     }
                  }

                  if (var4) {
                     break;
                  }
               }
            }

            if (!var4) {
               this.m_buildingsWithoutFeatures.add(var3);
            }
         }

      }
   }

   private void debugRenderBuilding(BuildingDef var1, float var2, float var3, float var4, float var5) {
      for(int var6 = 0; var6 < var1.rooms.size(); ++var6) {
         ArrayList var7 = ((RoomDef)var1.rooms.get(var6)).getRects();

         for(int var8 = 0; var8 < var7.size(); ++var8) {
            RoomDef.RoomRect var9 = (RoomDef.RoomRect)var7.get(var8);
            float var10 = this.m_APIv1.worldToUIX((float)var9.x, (float)var9.y);
            float var11 = this.m_APIv1.worldToUIY((float)var9.x, (float)var9.y);
            float var12 = this.m_APIv1.worldToUIX((float)var9.getX2(), (float)var9.getY2());
            float var13 = this.m_APIv1.worldToUIY((float)var9.getX2(), (float)var9.getY2());
            this.DrawTextureScaledColor((Texture)null, (double)var10, (double)var11, (double)(var12 - var10), (double)(var13 - var11), (double)var2, (double)var3, (double)var4, (double)var5);
         }
      }

   }
}
