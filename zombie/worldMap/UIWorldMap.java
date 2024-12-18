package zombie.worldMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.pathfind.NestedPathWanderer;
import zombie.characters.animals.pathfind.NestedPaths;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.inventory.types.MapItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.SafeHouse;
import zombie.iso.worldgen.WGDebug;
import zombie.iso.zones.Zone;
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
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapStyleV1;
import zombie.worldMap.symbols.MapSymbolDefinitions;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapSymbolsV1;
import zombie.worldMap.symbols.WorldMapSymbolsV2;

public class UIWorldMap extends UIElement {
   static final ArrayList<WorldMapFeature> s_tempFeatures = new ArrayList();
   protected final WorldMap m_worldMap = new WorldMap();
   protected final WorldMapStyle m_style = new WorldMapStyle();
   protected final WorldMapRenderer m_renderer = new WorldMapRenderer();
   protected final WorldMapMarkers m_markers = new WorldMapMarkers();
   protected WorldMapSymbols m_symbols = null;
   protected final WorldMapStyleLayer.RGBAf m_color = (new WorldMapStyleLayer.RGBAf()).init(0.85882354F, 0.84313726F, 0.7529412F, 1.0F);
   private float m_clickWorldX = 0.0F;
   private float m_clickWorldY = 0.0F;
   protected final UIWorldMapV1 m_APIv1 = new UIWorldMapV1(this);
   protected final UIWorldMapV2 m_APIv2 = new UIWorldMapV2(this);
   protected final UIWorldMapV3 m_APIv3 = new UIWorldMapV3(this);
   private boolean m_dataWasReady = false;
   private boolean doStencil = true;
   HashMap<Zone, NestedPaths> nestedPathMap = new HashMap();
   NestedPathWanderer nestedPathWanderer = new NestedPathWanderer();
   private final ArrayList<BuildingDef> m_buildingsWithoutFeatures = new ArrayList();
   private boolean m_bBuildingsWithoutFeatures = false;
   private final ArrayList<BuildingDef> m_basements = new ArrayList();
   private boolean m_bInitBasements = false;

   public UIWorldMap(KahluaTable var1) {
      super(var1);
   }

   public UIWorldMapV2 getAPI() {
      return this.m_APIv3;
   }

   public UIWorldMapV1 getAPIv1() {
      return this.m_APIv1;
   }

   public UIWorldMapV2 getAPIv2() {
      return this.m_APIv2;
   }

   public UIWorldMapV2 getAPIv3() {
      return this.m_APIv3;
   }

   protected void setMapItem(MapItem var1) {
      this.m_symbols = var1.getSymbols();
   }

   public void scaleWidthToHeight() {
      double var1 = MapProjection.zoomAtMetersPerPixel((double)this.m_worldMap.getHeightInSquares() / this.getHeight(), this.getHeight());
      float var3 = this.m_renderer.getWorldScale((float)var1);
      this.setWidth((double)((float)this.m_worldMap.getWidthInSquares() * var3));
      if (this.getTable() != null) {
         this.getTable().rawset("width", this.getWidth());
      }

      this.m_renderer.setMap(this.m_worldMap, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue(), this.getWidth().intValue(), this.getHeight().intValue());
      this.m_renderer.resetView();
   }

   public void render() {
      if (this.isVisible()) {
         if (this.Parent == null || this.Parent.getMaxDrawHeight() == -1.0 || !(this.Parent.getMaxDrawHeight() <= this.getY())) {
            if (!this.m_worldMap.hasData()) {
            }

            Double var1 = this.clampToParentX((double)this.getAbsoluteX().intValue());
            Double var2 = this.clampToParentX((double)this.getAbsoluteX().intValue() + this.getWidth());
            Double var3 = this.clampToParentY((double)this.getAbsoluteY().intValue());
            Double var4 = this.clampToParentY((double)this.getAbsoluteY().intValue() + this.getHeight());
            if (this.doStencil) {
               this.setStencilRect((double)(var1.intValue() - this.getAbsoluteX().intValue()), (double)(var3.intValue() - this.getAbsoluteY().intValue()), (double)(var2.intValue() - var1.intValue()), (double)(var4.intValue() - var3.intValue()));
            }

            this.DrawTextureScaledColor((Texture)null, 0.0, 0.0, this.getWidth(), this.getHeight(), (double)this.m_color.r, (double)this.m_color.g, (double)this.m_color.b, (double)this.m_color.a);
            this.m_renderer.setMap(this.m_worldMap, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue(), this.getWidth().intValue(), this.getHeight().intValue());
            this.m_renderer.updateView();
            float var5 = this.m_renderer.getDisplayZoomF();
            float var6 = this.m_renderer.getCenterWorldX();
            float var7 = this.m_renderer.getCenterWorldY();
            this.m_APIv1.getWorldScale(var5);
            if (this.m_renderer.getBoolean("HideUnvisited") && WorldMapVisited.getInstance() != null) {
               this.m_renderer.setVisited(WorldMapVisited.getInstance());
            } else {
               this.m_renderer.setVisited((WorldMapVisited)null);
            }

            this.m_renderer.render(this);
            if (this.m_renderer.getBoolean("Symbols") && this.m_symbols != null) {
               this.m_symbols.render(this);
            }

            this.m_markers.render(this);
            this.renderLocalPlayers();
            this.renderRemotePlayers();
            if (this.m_renderer.getBoolean("WGRoads")) {
               WGDebug.getInstance().renderRoads(this);
            }

            if (this.m_renderer.getBoolean("Animals") || this.m_renderer.getBoolean("AnimalTracks")) {
               AnimalZones.getInstance().render(this, this.m_renderer.getBoolean("Animals"), this.m_renderer.getBoolean("AnimalTracks"));
            }

            int var9;
            float var11;
            if (this.m_renderer.getBoolean("Players") && var5 < 20.0F) {
               for(var9 = 0; var9 < IsoPlayer.numPlayers; ++var9) {
                  IsoPlayer var10 = IsoPlayer.players[var9];
                  if (var10 != null && !var10.isDead()) {
                     var11 = var10.getX();
                     float var12 = var10.getY();
                     if (var10.getVehicle() != null) {
                        var11 = var10.getVehicle().getX();
                        var12 = var10.getVehicle().getY();
                     }

                     float var13 = this.m_APIv1.worldToUIX(var11, var12, var5, var6, var7, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
                     float var14 = this.m_APIv1.worldToUIY(var11, var12, var5, var6, var7, this.m_renderer.getProjectionMatrix(), this.m_renderer.getModelViewMatrix());
                     var13 = PZMath.floor(var13);
                     var14 = PZMath.floor(var14);
                     this.DrawTextureScaledColor((Texture)null, (double)var13 - 3.0, (double)var14 - 3.0, 6.0, 6.0, 1.0, 0.0, 0.0, 1.0);
                  }
               }
            }

            var9 = TextManager.instance.getFontHeight(UIFont.Small);
            int var20;
            float var29;
            double var32;
            if (Core.bDebug && this.m_renderer.getBoolean("DebugInfo")) {
               this.DrawTextureScaledColor((Texture)null, 0.0, 0.0, 200.0, (double)var9 * 6.0, 1.0, 1.0, 1.0, 1.0);
               var29 = this.m_APIv1.mouseToWorldX();
               var11 = this.m_APIv1.mouseToWorldY();
               var32 = 0.0;
               double var37 = 0.0;
               double var16 = 0.0;
               double var18 = 1.0;
               var20 = 0;
               this.DrawText("SQUARE = " + (int)var29 + "," + (int)var11, 0.0, (double)var20, var32, var37, var16, var18);
               var20 += var9;
               this.DrawText("CELL = " + (int)(var29 / (float)IsoCell.CellSizeInSquares) + "," + (int)(var11 / (float)IsoCell.CellSizeInSquares), 0.0, (double)var20, var32, var37, var16, var18);
               var20 += var9;
               this.DrawText("CELL (300) = " + (int)(var29 / 300.0F) + "," + (int)(var11 / 300.0F), 0.0, (double)var20, var32, var37, var16, var18);
               var20 += var9;
               this.DrawText("CENTER = " + PZMath.fastfloor(this.m_renderer.getCenterWorldX()) + "," + PZMath.fastfloor(this.m_renderer.getCenterWorldY()), 0.0, (double)var20, var32, var37, var16, var18);
               var20 += var9;
               this.DrawText("ZOOM = " + this.m_renderer.getDisplayZoomF(), 0.0, (double)var20, var32, var37, var16, var18);
               var20 += var9;
               WorldMapRenderer var10001 = this.m_renderer;
               this.DrawText("SCALE = " + var10001.getWorldScale(this.m_renderer.getZoomF()), 0.0, (double)var20, var32, var37, var16, var18);
               int var10000 = var20 + var9;
            }

            if (this.doStencil) {
               this.clearStencilRect();
               this.repaintStencilRect(0.0, 0.0, (double)this.width, (double)this.height);
            }

            if (Core.bDebug && DebugOptions.instance.UIRenderOutline.getValue()) {
               Double var30 = -this.getXScroll();
               Double var31 = -this.getYScroll();
               var32 = this.isMouseOver() ? 0.0 : 1.0;
               this.DrawTextureScaledColor((Texture)null, var30, var31, 1.0, (double)this.height, var32, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var30 + 1.0, var31, (double)this.width - 2.0, 1.0, var32, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var30 + (double)this.width - 1.0, var31, 1.0, (double)this.height, var32, 1.0, 1.0, 0.5);
               this.DrawTextureScaledColor((Texture)null, var30 + 1.0, var31 + (double)this.height - 1.0, (double)this.width - 2.0, 1.0, var32, 1.0, 1.0, 0.5);
            }

            if (Core.bDebug && this.m_renderer.getBoolean("HitTest")) {
               var29 = this.m_APIv1.mouseToWorldX();
               var11 = this.m_APIv1.mouseToWorldY();
               s_tempFeatures.clear();
               Iterator var33 = this.m_worldMap.m_data.iterator();

               while(var33.hasNext()) {
                  WorldMapData var34 = (WorldMapData)var33.next();
                  if (var34.isReady()) {
                     var34.hitTest(var29, var11, s_tempFeatures);
                  }
               }

               if (!s_tempFeatures.isEmpty()) {
                  WorldMapFeature var35 = (WorldMapFeature)s_tempFeatures.get(s_tempFeatures.size() - 1);
                  int var36 = var35.m_cell.m_x * IsoCell.CellSizeInSquares;
                  int var38 = var35.m_cell.m_y * IsoCell.CellSizeInSquares;
                  int var15 = this.getAbsoluteX().intValue();
                  int var39 = this.getAbsoluteY().intValue();
                  WorldMapGeometry var17 = (WorldMapGeometry)var35.m_geometries.get(0);

                  for(int var40 = 0; var40 < var17.m_points.size(); ++var40) {
                     WorldMapPoints var19 = (WorldMapPoints)var17.m_points.get(var40);

                     for(var20 = 0; var20 < var19.numPoints(); ++var20) {
                        int var21 = var19.getX(var20);
                        int var22 = var19.getY(var20);
                        int var23 = var19.getX((var20 + 1) % var19.numPoints());
                        int var24 = var19.getY((var20 + 1) % var19.numPoints());
                        float var25 = this.m_APIv1.worldToUIX((float)(var36 + var21), (float)(var38 + var22));
                        float var26 = this.m_APIv1.worldToUIY((float)(var36 + var21), (float)(var38 + var22));
                        float var27 = this.m_APIv1.worldToUIX((float)(var36 + var23), (float)(var38 + var24));
                        float var28 = this.m_APIv1.worldToUIY((float)(var36 + var23), (float)(var38 + var24));
                        SpriteRenderer.instance.renderline((Texture)null, var15 + (int)var25, var39 + (int)var26, var15 + (int)var27, var39 + (int)var28, 1.0F, 0.0F, 0.0F, 1.0F);
                     }
                  }
               }
            }

            if (Core.bDebug && this.m_renderer.getBoolean("BuildingsWithoutFeatures")) {
               this.renderBuildingsWithoutFeatures();
            } else {
               this.m_bBuildingsWithoutFeatures = false;
            }

            if (Core.bDebug && this.m_renderer.getBoolean("Basements")) {
               this.renderBasements();
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
                  float var4 = var3.getX();
                  float var5 = var3.getY();
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

               if (SafeHouse.isInSameSafehouse(var3.getUsername(), var1.getUsername())) {
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
      var0.setExposed(UIWorldMapV2.class);
      var0.setExposed(UIWorldMapV3.class);
      var0.setExposed(WorldMapGridSquareMarker.class);
      var0.setExposed(WorldMapMarkers.class);
      var0.setExposed(WorldMapRenderer.WorldMapBooleanOption.class);
      var0.setExposed(WorldMapRenderer.WorldMapDoubleOption.class);
      var0.setExposed(WorldMapVisited.class);
      WorldMapMarkersV1.setExposed(var0);
      WorldMapStyleV1.setExposed(var0);
      WorldMapSymbolsV1.setExposed(var0);
      WorldMapSymbolsV2.setExposed(var0);
      var0.setExposed(WorldMapEditorState.class);
      var0.setExposed(WorldMapSettings.class);
      var0.setExposed(WorldMapClient.class);
   }

   private void renderBuildingsWithoutFeatures() {
      if (this.m_bBuildingsWithoutFeatures) {
         long var12 = System.currentTimeMillis() / 500L;
         if ((var12 & 1L) == 0L) {
            Iterator var13 = this.m_buildingsWithoutFeatures.iterator();

            while(var13.hasNext()) {
               BuildingDef var14 = (BuildingDef)var13.next();
               this.debugRenderBuilding(var14, false, 1.0F, 0.0F, 0.0F, 1.0F);
            }

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

                     for(int var15 = 0; var15 < s_tempFeatures.size(); ++var15) {
                        WorldMapFeature var16 = (WorldMapFeature)s_tempFeatures.get(var15);
                        if (var16.m_properties.containsKey("building")) {
                           var4 = true;
                           break;
                        }
                     }

                     if (var4) {
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

   private void renderBasements() {
      if (this.m_bInitBasements) {
         long var6 = System.currentTimeMillis() / 500L;
         if ((var6 & 1L) == 0L) {
            Iterator var7 = this.m_basements.iterator();

            while(var7.hasNext()) {
               BuildingDef var8 = (BuildingDef)var7.next();
               this.debugRenderBuilding(var8, true, 1.0F, 0.0F, 0.0F, 1.0F);
            }

         }
      } else {
         this.m_bInitBasements = true;
         this.m_basements.clear();
         IsoMetaGrid var1 = IsoWorld.instance.MetaGrid;

         for(int var2 = 0; var2 < var1.Buildings.size(); ++var2) {
            BuildingDef var3 = (BuildingDef)var1.Buildings.get(var2);

            for(int var4 = 0; var4 < var3.rooms.size(); ++var4) {
               RoomDef var5 = (RoomDef)var3.rooms.get(var4);
               if (var5.level < 0) {
                  this.m_basements.add(var3);
                  break;
               }
            }
         }

      }
   }

   private void debugRenderBuilding(BuildingDef var1, boolean var2, float var3, float var4, float var5, float var6) {
      for(int var7 = 0; var7 < var1.rooms.size(); ++var7) {
         RoomDef var8 = (RoomDef)var1.rooms.get(var7);
         if (var2 == var8.level < 0) {
            ArrayList var9 = var8.getRects();

            for(int var10 = 0; var10 < var9.size(); ++var10) {
               RoomDef.RoomRect var11 = (RoomDef.RoomRect)var9.get(var10);
               float var12 = this.m_APIv1.worldToUIX((float)var11.x, (float)var11.y);
               float var13 = this.m_APIv1.worldToUIY((float)var11.x, (float)var11.y);
               float var14 = this.m_APIv1.worldToUIX((float)var11.getX2(), (float)var11.getY());
               float var15 = this.m_APIv1.worldToUIY((float)var11.getX2(), (float)var11.getY());
               float var16 = this.m_APIv1.worldToUIX((float)var11.getX2(), (float)var11.getY2());
               float var17 = this.m_APIv1.worldToUIY((float)var11.getX2(), (float)var11.getY2());
               float var18 = this.m_APIv1.worldToUIX((float)var11.getX(), (float)var11.getY2());
               float var19 = this.m_APIv1.worldToUIY((float)var11.getX(), (float)var11.getY2());
               this.DrawTexture((Texture)null, (double)var12, (double)var13, (double)var14, (double)var15, (double)var16, (double)var17, (double)var18, (double)var19, (double)var3, (double)var4, (double)var5, (double)var6);
            }
         }
      }

   }

   public void DrawSymbol(Texture var1, Double var2, Double var3, Double var4, Double var5, Double var6, Double var7, Double var8, Double var9) {
      TextManager.sdfShader.updateThreshold(0.1F);
      TextManager.sdfShader.updateShadow(0.0F);
      TextManager.sdfShader.updateOutline(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      IndieGL.StartShader(TextManager.sdfShader);
      this.DrawTextureScaledColor(var1, var2, var3, var4, var5, var7, var8, var9, var6);
      IndieGL.EndShader();
   }

   public void DrawTextSdf(UIFont var1, String var2, double var3, double var5, double var7, double var9, double var11, double var13, double var15) {
      TextManager.sdfShader.updateThreshold(Math.abs(0.5F - ((float)var7 - 0.4F) / 8.2F) / 5.0F + 0.01F);
      TextManager.sdfShader.updateShadow(0.0F);
      TextManager.sdfShader.updateOutline(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      IndieGL.StartShader(TextManager.sdfShader);
      this.DrawText(var1, var2, var3, var5, var7, var9, var11, var13, var15);
      IndieGL.EndShader();
   }

   public void setDoStencil(boolean var1) {
      this.doStencil = var1;
   }
}
