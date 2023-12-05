package zombie.gameStates;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Consumer;
import org.joml.Vector2f;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientStreamManager;
import zombie.FliesSound;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.textures.Texture;
import zombie.core.utils.BooleanGrid;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.erosion.ErosionData;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.NearestWalls;
import zombie.iso.ParticlesFire;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.ClipperOffset;
import zombie.vehicles.EditVehicleState;
import zombie.vehicles.PolygonalMap2;

public final class DebugChunkState extends GameState {
   public static DebugChunkState instance;
   private EditVehicleState.LuaEnvironment m_luaEnv;
   private boolean bExit = false;
   private final ArrayList<UIElement> m_gameUI = new ArrayList();
   private final ArrayList<UIElement> m_selfUI = new ArrayList();
   private boolean m_bSuspendUI;
   private KahluaTable m_table = null;
   private int m_playerIndex = 0;
   private int m_z = 0;
   private int gridX = -1;
   private int gridY = -1;
   private UIFont FONT;
   private String m_vehicleStory;
   static boolean keyQpressed = false;
   private static ClipperOffset m_clipperOffset = null;
   private static ByteBuffer m_clipperBuffer;
   private static final int VERSION = 1;
   private final ArrayList<ConfigOption> options;
   private BooleanDebugOption BuildingRect;
   private BooleanDebugOption ChunkGrid;
   private BooleanDebugOption ClosestRoomSquare;
   private BooleanDebugOption EmptySquares;
   private BooleanDebugOption FlyBuzzEmitters;
   private BooleanDebugOption LightSquares;
   private BooleanDebugOption LineClearCollide;
   private BooleanDebugOption NearestWallsOpt;
   private BooleanDebugOption ObjectPicker;
   private BooleanDebugOption RoomLightRects;
   private BooleanDebugOption VehicleStory;
   private BooleanDebugOption RandomSquareInZone;
   private BooleanDebugOption ZoneRect;

   public DebugChunkState() {
      this.FONT = UIFont.DebugConsole;
      this.m_vehicleStory = "Basic Car Crash";
      this.options = new ArrayList();
      this.BuildingRect = new BooleanDebugOption("BuildingRect", true);
      this.ChunkGrid = new BooleanDebugOption("ChunkGrid", true);
      this.ClosestRoomSquare = new BooleanDebugOption("ClosestRoomSquare", true);
      this.EmptySquares = new BooleanDebugOption("EmptySquares", true);
      this.FlyBuzzEmitters = new BooleanDebugOption("FlyBuzzEmitters", true);
      this.LightSquares = new BooleanDebugOption("LightSquares", true);
      this.LineClearCollide = new BooleanDebugOption("LineClearCollide", true);
      this.NearestWallsOpt = new BooleanDebugOption("NearestWalls", true);
      this.ObjectPicker = new BooleanDebugOption("ObjectPicker", true);
      this.RoomLightRects = new BooleanDebugOption("RoomLightRects", true);
      this.VehicleStory = new BooleanDebugOption("VehicleStory", true);
      this.RandomSquareInZone = new BooleanDebugOption("RandomSquareInZone", true);
      this.ZoneRect = new BooleanDebugOption("ZoneRect", true);
      instance = this;
   }

   public void enter() {
      instance = this;
      this.load();
      if (this.m_luaEnv == null) {
         this.m_luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
      }

      this.saveGameUI();
      if (this.m_selfUI.size() == 0) {
         IsoPlayer var1 = IsoPlayer.players[this.m_playerIndex];
         this.m_z = var1 == null ? 0 : (int)var1.z;
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_luaEnv.env.rawget("DebugChunkState_InitUI"), this);
         if (this.m_table != null && this.m_table.getMetatable() != null) {
            this.m_table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
         }
      } else {
         UIManager.UI.addAll(this.m_selfUI);
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("showUI"), this.m_table);
      }

      this.bExit = false;
   }

   public void yield() {
      this.restoreGameUI();
   }

   public void reenter() {
      this.saveGameUI();
   }

   public void exit() {
      this.save();
      this.restoreGameUI();

      for(int var1 = 0; var1 < IsoCamera.cameras.length; ++var1) {
         IsoCamera.cameras[var1].DeferedX = IsoCamera.cameras[var1].DeferedY = 0.0F;
      }

   }

   public void render() {
      IsoPlayer.setInstance(IsoPlayer.players[this.m_playerIndex]);
      IsoCamera.CamCharacter = IsoPlayer.players[this.m_playerIndex];
      boolean var1 = true;

      int var2;
      for(var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         if (var2 != this.m_playerIndex && IsoPlayer.players[var2] != null) {
            Core.getInstance().StartFrame(var2, var1);
            Core.getInstance().EndFrame(var2);
            var1 = false;
         }
      }

      Core.getInstance().StartFrame(this.m_playerIndex, var1);
      this.renderScene();
      Core.getInstance().EndFrame(this.m_playerIndex);
      Core.getInstance().RenderOffScreenBuffer();

      for(var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
         TextDrawObject.NoRender(var2);
         ChatElement.NoRender(var2);
      }

      if (Core.getInstance().StartFrameUI()) {
         this.renderUI();
      }

      Core.getInstance().EndFrameUI();
   }

   public GameStateMachine.StateAction update() {
      return !this.bExit && !GameKeyboard.isKeyPressed(60) ? this.updateScene() : GameStateMachine.StateAction.Continue;
   }

   public static DebugChunkState checkInstance() {
      instance = null;
      if (instance != null) {
         if (instance.m_table != null && instance.m_table.getMetatable() != null) {
            if (instance.m_table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
               instance = null;
            }
         } else {
            instance = null;
         }
      }

      return instance == null ? new DebugChunkState() : instance;
   }

   public void renderScene() {
      IsoCamera.frameState.set(this.m_playerIndex);
      SpriteRenderer.instance.doCoreIntParam(0, IsoCamera.CamCharacter.x);
      SpriteRenderer.instance.doCoreIntParam(1, IsoCamera.CamCharacter.y);
      SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.CamCharacter.z);
      IsoSprite.globalOffsetX = -1.0F;
      IsoWorld.instance.CurrentCell.render();
      if (this.ChunkGrid.getValue()) {
         this.drawGrid();
      }

      this.drawCursor();
      int var2;
      if (this.LightSquares.getValue()) {
         Stack var1 = IsoWorld.instance.getCell().getLamppostPositions();

         for(var2 = 0; var2 < var1.size(); ++var2) {
            IsoLightSource var3 = (IsoLightSource)var1.get(var2);
            if (var3.z == this.m_z) {
               this.paintSquare(var3.x, var3.y, var3.z, 1.0F, 1.0F, 0.0F, 0.5F);
            }
         }
      }

      if (this.ZoneRect.getValue()) {
         this.drawZones();
      }

      IsoGridSquare var5;
      if (this.BuildingRect.getValue()) {
         var5 = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, this.m_z);
         if (var5 != null && var5.getBuilding() != null) {
            BuildingDef var8 = var5.getBuilding().getDef();
            this.DrawIsoLine((float)var8.getX(), (float)var8.getY(), (float)var8.getX2(), (float)var8.getY(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
            this.DrawIsoLine((float)var8.getX2(), (float)var8.getY(), (float)var8.getX2(), (float)var8.getY2(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
            this.DrawIsoLine((float)var8.getX2(), (float)var8.getY2(), (float)var8.getX(), (float)var8.getY2(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
            this.DrawIsoLine((float)var8.getX(), (float)var8.getY2(), (float)var8.getX(), (float)var8.getY(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
         }
      }

      if (this.RoomLightRects.getValue()) {
         ArrayList var6 = IsoWorld.instance.CurrentCell.roomLights;

         for(var2 = 0; var2 < var6.size(); ++var2) {
            IsoRoomLight var9 = (IsoRoomLight)var6.get(var2);
            if (var9.z == this.m_z) {
               this.DrawIsoRect((float)var9.x, (float)var9.y, (float)var9.width, (float)var9.height, 0.0F, 1.0F, 1.0F, 1.0F, 1);
            }
         }
      }

      if (this.FlyBuzzEmitters.getValue()) {
         FliesSound.instance.render();
      }

      if (this.ClosestRoomSquare.getValue()) {
         float var7 = IsoPlayer.players[this.m_playerIndex].getX();
         float var10 = IsoPlayer.players[this.m_playerIndex].getY();
         Vector2f var11 = new Vector2f();
         BuildingDef var4 = ((AmbientStreamManager)AmbientStreamManager.getInstance()).getNearestBuilding(var7, var10, var11);
         if (var4 != null) {
            this.DrawIsoLine(var7, var10, var11.x, var11.y, 1.0F, 1.0F, 1.0F, 1.0F, 1);
         }
      }

      if (this.m_table != null && this.m_table.rawget("selectedSquare") != null) {
         var5 = (IsoGridSquare)Type.tryCastTo(this.m_table.rawget("selectedSquare"), IsoGridSquare.class);
         if (var5 != null) {
            this.DrawIsoRect((float)var5.x, (float)var5.y, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 2);
         }
      }

      LineDrawer.render();
      LineDrawer.clear();
   }

   private void renderUI() {
      int var1 = this.m_playerIndex;
      Stack var2 = IsoWorld.instance.getCell().getLamppostPositions();
      int var3 = 0;

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         IsoLightSource var5 = (IsoLightSource)var2.get(var4);
         if (var5.bActive) {
            ++var3;
         }
      }

      UIManager.render();
   }

   public void setTable(KahluaTable var1) {
      this.m_table = var1;
   }

   public GameStateMachine.StateAction updateScene() {
      IsoPlayer.setInstance(IsoPlayer.players[this.m_playerIndex]);
      IsoCamera.CamCharacter = IsoPlayer.players[this.m_playerIndex];
      UIManager.setPicked(IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA()));
      IsoObject var1 = UIManager.getPicked() == null ? null : UIManager.getPicked().tile;
      UIManager.setLastPicked(var1);
      if (GameKeyboard.isKeyDown(16)) {
         if (!keyQpressed) {
            IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, 0);
            if (var2 != null) {
               GameClient.instance.worldObjectsSyncReq.putRequestSyncIsoChunk(var2.chunk);
               DebugLog.General.debugln("Requesting sync IsoChunk %s", var2.chunk);
            }

            keyQpressed = true;
         }
      } else {
         keyQpressed = false;
      }

      if (GameKeyboard.isKeyDown(19)) {
         if (!keyQpressed) {
            DebugOptions.instance.Terrain.RenderTiles.NewRender.setValue(true);
            keyQpressed = true;
            DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.Terrain.RenderTiles.NewRender.getValue());
         }
      } else {
         keyQpressed = false;
      }

      if (GameKeyboard.isKeyDown(20)) {
         if (!keyQpressed) {
            DebugOptions.instance.Terrain.RenderTiles.NewRender.setValue(false);
            keyQpressed = true;
            DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.Terrain.RenderTiles.NewRender.getValue());
         }
      } else {
         keyQpressed = false;
      }

      if (GameKeyboard.isKeyDown(31)) {
         if (!keyQpressed) {
            ParticlesFire.getInstance().reloadShader();
            keyQpressed = true;
            DebugLog.General.debugln("ParticlesFire.reloadShader");
         }
      } else {
         keyQpressed = false;
      }

      IsoCamera.update();
      this.updateCursor();
      return GameStateMachine.StateAction.Remain;
   }

   private void saveGameUI() {
      this.m_gameUI.clear();
      this.m_gameUI.addAll(UIManager.UI);
      UIManager.UI.clear();
      this.m_bSuspendUI = UIManager.bSuspend;
      UIManager.bSuspend = false;
      UIManager.setShowPausedMessage(false);
      UIManager.defaultthread = this.m_luaEnv.thread;
   }

   private void restoreGameUI() {
      this.m_selfUI.clear();
      this.m_selfUI.addAll(UIManager.UI);
      UIManager.UI.clear();
      UIManager.UI.addAll(this.m_gameUI);
      UIManager.bSuspend = this.m_bSuspendUI;
      UIManager.setShowPausedMessage(true);
      UIManager.defaultthread = LuaManager.thread;
   }

   public Object fromLua0(String var1) {
      switch (var1) {
         case "exit":
            this.bExit = true;
            return null;
         case "getCameraDragX":
            return BoxedStaticValues.toDouble((double)(-IsoCamera.cameras[this.m_playerIndex].DeferedX));
         case "getCameraDragY":
            return BoxedStaticValues.toDouble((double)(-IsoCamera.cameras[this.m_playerIndex].DeferedY));
         case "getPlayerIndex":
            return BoxedStaticValues.toDouble((double)this.m_playerIndex);
         case "getVehicleStory":
            return this.m_vehicleStory;
         case "getZ":
            return BoxedStaticValues.toDouble((double)this.m_z);
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\"", var1));
      }
   }

   public Object fromLua1(String var1, Object var2) {
      switch (var1) {
         case "getCameraDragX":
            return BoxedStaticValues.toDouble((double)(-IsoCamera.cameras[this.m_playerIndex].DeferedX));
         case "getCameraDragY":
            return BoxedStaticValues.toDouble((double)(-IsoCamera.cameras[this.m_playerIndex].DeferedY));
         case "setPlayerIndex":
            this.m_playerIndex = PZMath.clamp(((Double)var2).intValue(), 0, 3);
            return null;
         case "setVehicleStory":
            this.m_vehicleStory = (String)var2;
            return null;
         case "setZ":
            this.m_z = PZMath.clamp(((Double)var2).intValue(), 0, 7);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", var1, var2));
      }
   }

   public Object fromLua2(String var1, Object var2, Object var3) {
      switch (var1) {
         case "dragCamera":
            float var6 = ((Double)var2).floatValue();
            float var7 = ((Double)var3).floatValue();
            IsoCamera.cameras[this.m_playerIndex].DeferedX = -var6;
            IsoCamera.cameras[this.m_playerIndex].DeferedY = -var7;
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \\\"%s\\\"", var1, var2, var3));
      }
   }

   private void updateCursor() {
      int var1 = this.m_playerIndex;
      int var2 = Core.TileScale;
      float var3 = (float)Mouse.getXA();
      float var4 = (float)Mouse.getYA();
      var3 -= (float)IsoCamera.getScreenLeft(var1);
      var4 -= (float)IsoCamera.getScreenTop(var1);
      var3 *= Core.getInstance().getZoom(var1);
      var4 *= Core.getInstance().getZoom(var1);
      int var5 = this.m_z;
      this.gridX = (int)IsoUtils.XToIso(var3, var4, (float)var5);
      this.gridY = (int)IsoUtils.YToIso(var3, var4, (float)var5);
   }

   private void DrawIsoLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
      float var10 = (float)this.m_z;
      float var11 = IsoUtils.XToScreenExact(var1, var2, var10, 0);
      float var12 = IsoUtils.YToScreenExact(var1, var2, var10, 0);
      float var13 = IsoUtils.XToScreenExact(var3, var4, var10, 0);
      float var14 = IsoUtils.YToScreenExact(var3, var4, var10, 0);
      LineDrawer.drawLine(var11, var12, var13, var14, var5, var6, var7, var8, var9);
   }

   private void DrawIsoRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
      this.DrawIsoLine(var1, var2, var1 + var3, var2, var5, var6, var7, var8, var9);
      this.DrawIsoLine(var1 + var3, var2, var1 + var3, var2 + var4, var5, var6, var7, var8, var9);
      this.DrawIsoLine(var1 + var3, var2 + var4, var1, var2 + var4, var5, var6, var7, var8, var9);
      this.DrawIsoLine(var1, var2 + var4, var1, var2, var5, var6, var7, var8, var9);
   }

   private void drawGrid() {
      int var1 = this.m_playerIndex;
      float var2 = IsoUtils.XToIso(-128.0F, -256.0F, 0.0F);
      float var3 = IsoUtils.YToIso((float)(Core.getInstance().getOffscreenWidth(var1) + 128), -256.0F, 0.0F);
      float var4 = IsoUtils.XToIso((float)(Core.getInstance().getOffscreenWidth(var1) + 128), (float)(Core.getInstance().getOffscreenHeight(var1) + 256), 6.0F);
      float var5 = IsoUtils.YToIso(-128.0F, (float)(Core.getInstance().getOffscreenHeight(var1) + 256), 6.0F);
      int var7 = (int)var3;
      int var9 = (int)var5;
      int var6 = (int)var2;
      int var8 = (int)var4;
      var6 -= 2;
      var7 -= 2;

      int var10;
      for(var10 = var7; var10 <= var9; ++var10) {
         if (var10 % 10 == 0) {
            this.DrawIsoLine((float)var6, (float)var10, (float)var8, (float)var10, 1.0F, 1.0F, 1.0F, 0.5F, 1);
         }
      }

      for(var10 = var6; var10 <= var8; ++var10) {
         if (var10 % 10 == 0) {
            this.DrawIsoLine((float)var10, (float)var7, (float)var10, (float)var9, 1.0F, 1.0F, 1.0F, 0.5F, 1);
         }
      }

      for(var10 = var7; var10 <= var9; ++var10) {
         if (var10 % 300 == 0) {
            this.DrawIsoLine((float)var6, (float)var10, (float)var8, (float)var10, 0.0F, 1.0F, 0.0F, 0.5F, 1);
         }
      }

      for(var10 = var6; var10 <= var8; ++var10) {
         if (var10 % 300 == 0) {
            this.DrawIsoLine((float)var10, (float)var7, (float)var10, (float)var9, 0.0F, 1.0F, 0.0F, 0.5F, 1);
         }
      }

      if (GameClient.bClient) {
         for(var10 = var7; var10 <= var9; ++var10) {
            if (var10 % 50 == 0) {
               this.DrawIsoLine((float)var6, (float)var10, (float)var8, (float)var10, 1.0F, 0.0F, 0.0F, 0.5F, 1);
            }
         }

         for(var10 = var6; var10 <= var8; ++var10) {
            if (var10 % 50 == 0) {
               this.DrawIsoLine((float)var10, (float)var7, (float)var10, (float)var9, 1.0F, 0.0F, 0.0F, 0.5F, 1);
            }
         }
      }

   }

   private void drawCursor() {
      int var1 = this.m_playerIndex;
      int var2 = Core.TileScale;
      float var3 = (float)this.m_z;
      int var4 = (int)IsoUtils.XToScreenExact((float)this.gridX, (float)(this.gridY + 1), var3, 0);
      int var5 = (int)IsoUtils.YToScreenExact((float)this.gridX, (float)(this.gridY + 1), var3, 0);
      SpriteRenderer.instance.renderPoly((float)var4, (float)var5, (float)(var4 + 32 * var2), (float)(var5 - 16 * var2), (float)(var4 + 64 * var2), (float)var5, (float)(var4 + 32 * var2), (float)(var5 + 16 * var2), 0.0F, 0.0F, 1.0F, 0.5F);
      IsoChunkMap var6 = IsoWorld.instance.getCell().ChunkMap[var1];

      for(int var7 = var6.getWorldYMinTiles(); var7 < var6.getWorldYMaxTiles(); ++var7) {
         for(int var8 = var6.getWorldXMinTiles(); var8 < var6.getWorldXMaxTiles(); ++var8) {
            IsoGridSquare var9 = IsoWorld.instance.getCell().getGridSquare((double)var8, (double)var7, (double)var3);
            if (var9 != null) {
               if (var9 != var6.getGridSquare(var8, var7, (int)var3)) {
                  var4 = (int)IsoUtils.XToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  var5 = (int)IsoUtils.YToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  SpriteRenderer.instance.renderPoly((float)var4, (float)var5, (float)(var4 + 32), (float)(var5 - 16), (float)(var4 + 64), (float)var5, (float)(var4 + 32), (float)(var5 + 16), 1.0F, 0.0F, 0.0F, 0.8F);
               }

               if (var9 == null || var9.getX() != var8 || var9.getY() != var7 || (float)var9.getZ() != var3 || var9.e != null && var9.e.w != null && var9.e.w != var9 || var9.w != null && var9.w.e != null && var9.w.e != var9 || var9.n != null && var9.n.s != null && var9.n.s != var9 || var9.s != null && var9.s.n != null && var9.s.n != var9 || var9.nw != null && var9.nw.se != null && var9.nw.se != var9 || var9.se != null && var9.se.nw != null && var9.se.nw != var9) {
                  var4 = (int)IsoUtils.XToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  var5 = (int)IsoUtils.YToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  SpriteRenderer.instance.renderPoly((float)var4, (float)var5, (float)(var4 + 32), (float)(var5 - 16), (float)(var4 + 64), (float)var5, (float)(var4 + 32), (float)(var5 + 16), 1.0F, 0.0F, 0.0F, 0.5F);
               }

               if (var9 != null) {
                  IsoGridSquare var10 = var9.testPathFindAdjacent((IsoMovingObject)null, -1, 0, 0) ? null : var9.nav[IsoDirections.W.index()];
                  IsoGridSquare var11 = var9.testPathFindAdjacent((IsoMovingObject)null, 0, -1, 0) ? null : var9.nav[IsoDirections.N.index()];
                  IsoGridSquare var12 = var9.testPathFindAdjacent((IsoMovingObject)null, 1, 0, 0) ? null : var9.nav[IsoDirections.E.index()];
                  IsoGridSquare var13 = var9.testPathFindAdjacent((IsoMovingObject)null, 0, 1, 0) ? null : var9.nav[IsoDirections.S.index()];
                  IsoGridSquare var14 = var9.testPathFindAdjacent((IsoMovingObject)null, -1, -1, 0) ? null : var9.nav[IsoDirections.NW.index()];
                  IsoGridSquare var15 = var9.testPathFindAdjacent((IsoMovingObject)null, 1, -1, 0) ? null : var9.nav[IsoDirections.NE.index()];
                  IsoGridSquare var16 = var9.testPathFindAdjacent((IsoMovingObject)null, -1, 1, 0) ? null : var9.nav[IsoDirections.SW.index()];
                  IsoGridSquare var17 = var9.testPathFindAdjacent((IsoMovingObject)null, 1, 1, 0) ? null : var9.nav[IsoDirections.SE.index()];
                  if (var10 != var9.w || var11 != var9.n || var12 != var9.e || var13 != var9.s || var14 != var9.nw || var15 != var9.ne || var16 != var9.sw || var17 != var9.se) {
                     this.paintSquare(var8, var7, (int)var3, 1.0F, 0.0F, 0.0F, 0.5F);
                  }
               }

               if (var9 != null && (var9.nav[IsoDirections.NW.index()] != null && var9.nav[IsoDirections.NW.index()].nav[IsoDirections.SE.index()] != var9 || var9.nav[IsoDirections.NE.index()] != null && var9.nav[IsoDirections.NE.index()].nav[IsoDirections.SW.index()] != var9 || var9.nav[IsoDirections.SW.index()] != null && var9.nav[IsoDirections.SW.index()].nav[IsoDirections.NE.index()] != var9 || var9.nav[IsoDirections.SE.index()] != null && var9.nav[IsoDirections.SE.index()].nav[IsoDirections.NW.index()] != var9 || var9.nav[IsoDirections.N.index()] != null && var9.nav[IsoDirections.N.index()].nav[IsoDirections.S.index()] != var9 || var9.nav[IsoDirections.S.index()] != null && var9.nav[IsoDirections.S.index()].nav[IsoDirections.N.index()] != var9 || var9.nav[IsoDirections.W.index()] != null && var9.nav[IsoDirections.W.index()].nav[IsoDirections.E.index()] != var9 || var9.nav[IsoDirections.E.index()] != null && var9.nav[IsoDirections.E.index()].nav[IsoDirections.W.index()] != var9)) {
                  var4 = (int)IsoUtils.XToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  var5 = (int)IsoUtils.YToScreenExact((float)var8, (float)(var7 + 1), var3, 0);
                  SpriteRenderer.instance.renderPoly((float)var4, (float)var5, (float)(var4 + 32), (float)(var5 - 16), (float)(var4 + 64), (float)var5, (float)(var4 + 32), (float)(var5 + 16), 1.0F, 0.0F, 0.0F, 0.5F);
               }

               if (this.EmptySquares.getValue() && var9.getObjects().isEmpty()) {
                  this.paintSquare(var8, var7, (int)var3, 1.0F, 1.0F, 0.0F, 0.5F);
               }

               if (var9.getRoom() != null && var9.isFree(false) && !VirtualZombieManager.instance.canSpawnAt(var8, var7, (int)var3)) {
                  this.paintSquare(var8, var7, (int)var3, 1.0F, 1.0F, 1.0F, 1.0F);
               }

               if (var9.roofHideBuilding != null) {
                  this.paintSquare(var8, var7, (int)var3, 0.0F, 0.0F, 1.0F, 0.25F);
               }
            }
         }
      }

      if (IsoCamera.CamCharacter.getCurrentSquare() != null && Math.abs(this.gridX - (int)IsoCamera.CamCharacter.x) <= 1 && Math.abs(this.gridY - (int)IsoCamera.CamCharacter.y) <= 1) {
         IsoGridSquare var18 = IsoWorld.instance.CurrentCell.getGridSquare(this.gridX, this.gridY, this.m_z);
         IsoObject var19 = IsoCamera.CamCharacter.getCurrentSquare().testCollideSpecialObjects(var18);
         if (var19 != null) {
            var19.getSprite().RenderGhostTileRed((int)var19.getX(), (int)var19.getY(), (int)var19.getZ());
         }
      }

      if (this.LineClearCollide.getValue()) {
         this.lineClearCached(IsoWorld.instance.CurrentCell, this.gridX, this.gridY, (int)var3, (int)IsoCamera.CamCharacter.getX(), (int)IsoCamera.CamCharacter.getY(), this.m_z, false);
      }

      if (this.NearestWallsOpt.getValue()) {
         NearestWalls.render(this.gridX, this.gridY, this.m_z);
      }

      if (this.VehicleStory.getValue()) {
         this.drawVehicleStory();
      }

   }

   private void drawZones() {
      ArrayList var1 = IsoWorld.instance.MetaGrid.getZonesAt(this.gridX, this.gridY, this.m_z, new ArrayList());
      IsoMetaGrid.Zone var2 = null;

      int var6;
      int var8;
      int var9;
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         IsoMetaGrid.Zone var4 = (IsoMetaGrid.Zone)var1.get(var3);
         if (var4.isPreferredZoneForSquare) {
            var2 = var4;
         }

         if (!var4.isPolyline()) {
            if (!var4.points.isEmpty()) {
               for(int var5 = 0; var5 < var4.points.size(); var5 += 2) {
                  var6 = var4.points.get(var5);
                  int var7 = var4.points.get(var5 + 1);
                  var8 = var4.points.get((var5 + 2) % var4.points.size());
                  var9 = var4.points.get((var5 + 3) % var4.points.size());
                  this.DrawIsoLine((float)var6, (float)var7, (float)var8, (float)var9, 1.0F, 1.0F, 0.0F, 1.0F, 1);
               }
            } else {
               this.DrawIsoLine((float)var4.x, (float)var4.y, (float)(var4.x + var4.w), (float)var4.y, 1.0F, 1.0F, 0.0F, 1.0F, 1);
               this.DrawIsoLine((float)var4.x, (float)(var4.y + var4.h), (float)(var4.x + var4.w), (float)(var4.y + var4.h), 1.0F, 1.0F, 0.0F, 1.0F, 1);
               this.DrawIsoLine((float)var4.x, (float)var4.y, (float)var4.x, (float)(var4.y + var4.h), 1.0F, 1.0F, 0.0F, 1.0F, 1);
               this.DrawIsoLine((float)(var4.x + var4.w), (float)var4.y, (float)(var4.x + var4.w), (float)(var4.y + var4.h), 1.0F, 1.0F, 0.0F, 1.0F, 1);
            }
         }
      }

      var1 = IsoWorld.instance.MetaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.m_z, 3, 3, new ArrayList());
      PolygonalMap2.LiangBarsky var17 = new PolygonalMap2.LiangBarsky();
      double[] var18 = new double[2];
      IsoChunk var19 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(this.gridX, this.gridY, this.m_z);

      int var11;
      int var12;
      float var13;
      float var14;
      float var27;
      for(var6 = 0; var6 < var1.size(); ++var6) {
         IsoMetaGrid.Zone var21 = (IsoMetaGrid.Zone)var1.get(var6);
         if (var21 != null && var21.isPolyline() && !var21.points.isEmpty()) {
            for(var8 = 0; var8 < var21.points.size() - 2; var8 += 2) {
               var9 = var21.points.get(var8);
               int var10 = var21.points.get(var8 + 1);
               var11 = var21.points.get(var8 + 2);
               var12 = var21.points.get(var8 + 3);
               this.DrawIsoLine((float)var9, (float)var10, (float)var11, (float)var12, 1.0F, 1.0F, 0.0F, 1.0F, 1);
               var13 = (float)(var11 - var9);
               var14 = (float)(var12 - var10);
               if (var19 != null && var17.lineRectIntersect((float)var9, (float)var10, var13, var14, (float)(var19.wx * 10), (float)(var19.wy * 10), (float)(var19.wx * 10 + 10), (float)(var19.wy * 10 + 10), var18)) {
                  this.DrawIsoLine((float)var9 + (float)var18[0] * var13, (float)var10 + (float)var18[0] * var14, (float)var9 + (float)var18[1] * var13, (float)var10 + (float)var18[1] * var14, 0.0F, 1.0F, 0.0F, 1.0F, 1);
               }
            }

            if (var21.polylineOutlinePoints != null) {
               float[] var24 = var21.polylineOutlinePoints;

               for(var9 = 0; var9 < var24.length; var9 += 2) {
                  var27 = var24[var9];
                  float var28 = var24[var9 + 1];
                  float var29 = var24[(var9 + 2) % var24.length];
                  var13 = var24[(var9 + 3) % var24.length];
                  this.DrawIsoLine(var27, var28, var29, var13, 1.0F, 1.0F, 0.0F, 1.0F, 1);
               }
            }
         }
      }

      IsoMetaGrid.VehicleZone var20 = IsoWorld.instance.MetaGrid.getVehicleZoneAt(this.gridX, this.gridY, this.m_z);
      if (var20 != null) {
         float var22 = 0.5F;
         float var25 = 1.0F;
         float var26 = 0.5F;
         var27 = 1.0F;
         int var15;
         int var31;
         int var32;
         if (var20.isPolygon()) {
            for(var11 = 0; var11 < var20.points.size(); var11 += 2) {
               var12 = var20.points.get(var11);
               var31 = var20.points.get(var11 + 1);
               var32 = var20.points.get((var11 + 2) % var20.points.size());
               var15 = var20.points.get((var11 + 3) % var20.points.size());
               this.DrawIsoLine((float)var12, (float)var31, (float)var32, (float)var15, 1.0F, 1.0F, 0.0F, 1.0F, 1);
            }
         } else if (var20.isPolyline()) {
            for(var11 = 0; var11 < var20.points.size() - 2; var11 += 2) {
               var12 = var20.points.get(var11);
               var31 = var20.points.get(var11 + 1);
               var32 = var20.points.get(var11 + 2);
               var15 = var20.points.get(var11 + 3);
               this.DrawIsoLine((float)var12, (float)var31, (float)var32, (float)var15, 1.0F, 1.0F, 0.0F, 1.0F, 1);
            }

            if (var20.polylineOutlinePoints != null) {
               float[] var30 = var20.polylineOutlinePoints;

               for(var12 = 0; var12 < var30.length; var12 += 2) {
                  var13 = var30[var12];
                  var14 = var30[var12 + 1];
                  float var33 = var30[(var12 + 2) % var30.length];
                  float var16 = var30[(var12 + 3) % var30.length];
                  this.DrawIsoLine(var13, var14, var33, var16, 1.0F, 1.0F, 0.0F, 1.0F, 1);
               }
            }
         } else {
            this.DrawIsoLine((float)var20.x, (float)var20.y, (float)(var20.x + var20.w), (float)var20.y, var22, var25, var26, var27, 1);
            this.DrawIsoLine((float)var20.x, (float)(var20.y + var20.h), (float)(var20.x + var20.w), (float)(var20.y + var20.h), var22, var25, var26, var27, 1);
            this.DrawIsoLine((float)var20.x, (float)var20.y, (float)var20.x, (float)(var20.y + var20.h), var22, var25, var26, var27, 1);
            this.DrawIsoLine((float)(var20.x + var20.w), (float)var20.y, (float)(var20.x + var20.w), (float)(var20.y + var20.h), var22, var25, var26, var27, 1);
         }
      }

      if (this.RandomSquareInZone.getValue() && var2 != null) {
         IsoGridSquare var23 = var2.getRandomSquareInZone();
         if (var23 != null) {
            this.paintSquare(var23.x, var23.y, var23.z, 0.0F, 1.0F, 0.0F, 0.5F);
         }
      }

   }

   private void drawVehicleStory() {
      ArrayList var1 = IsoWorld.instance.MetaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.m_z, 3, 3, new ArrayList());
      if (!var1.isEmpty()) {
         IsoChunk var2 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(this.gridX, this.gridY, this.m_z);
         if (var2 != null) {
            for(int var3 = 0; var3 < var1.size(); ++var3) {
               IsoMetaGrid.Zone var4 = (IsoMetaGrid.Zone)var1.get(var3);
               if ("Nav".equals(var4.type)) {
                  VehicleStorySpawner var5 = VehicleStorySpawner.getInstance();
                  RandomizedVehicleStoryBase var6 = IsoWorld.instance.getRandomizedVehicleStoryByName(this.m_vehicleStory);
                  if (var6 != null && var6.isValid(var4, var2, true) && var6.initVehicleStorySpawner(var4, var2, true)) {
                     int var7 = var6.getMinZoneWidth();
                     int var8 = var6.getMinZoneHeight();
                     float[] var9 = new float[3];
                     if (var6.getSpawnPoint(var4, var2, var9)) {
                        float var10 = var9[0];
                        float var11 = var9[1];
                        float var12 = var9[2] + 1.5707964F;
                        var5.spawn(var10, var11, 0.0F, var12, (var0, var1x) -> {
                        });
                        var5.render(var10, var11, 0.0F, (float)var7, (float)var8, var9[2]);
                     }
                  }
               }
            }

         }
      }
   }

   private void DrawBehindStuff() {
      this.IsBehindStuff(IsoCamera.CamCharacter.getCurrentSquare());
   }

   private boolean IsBehindStuff(IsoGridSquare var1) {
      for(int var2 = 1; var2 < 8 && var1.getZ() + var2 < 8; ++var2) {
         for(int var3 = -5; var3 <= 6; ++var3) {
            for(int var4 = -5; var4 <= 6; ++var4) {
               if (var4 >= var3 - 5 && var4 <= var3 + 5) {
                  this.paintSquare(var1.getX() + var4 + var2 * 3, var1.getY() + var3 + var2 * 3, var1.getZ() + var2, 1.0F, 1.0F, 0.0F, 0.25F);
               }
            }
         }
      }

      return true;
   }

   private boolean IsBehindStuffRecY(int var1, int var2, int var3) {
      IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var3 >= 15) {
         return false;
      } else {
         this.paintSquare(var1, var2, var3, 1.0F, 1.0F, 0.0F, 0.25F);
         return this.IsBehindStuffRecY(var1, var2 + 1, var3 + 1);
      }
   }

   private boolean IsBehindStuffRecXY(int var1, int var2, int var3, int var4) {
      IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var3 >= 15) {
         return false;
      } else {
         this.paintSquare(var1, var2, var3, 1.0F, 1.0F, 0.0F, 0.25F);
         return this.IsBehindStuffRecXY(var1 + var4, var2 + var4, var3 + 1, var4);
      }
   }

   private boolean IsBehindStuffRecX(int var1, int var2, int var3) {
      IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      if (var3 >= 15) {
         return false;
      } else {
         this.paintSquare(var1, var2, var3, 1.0F, 1.0F, 0.0F, 0.25F);
         return this.IsBehindStuffRecX(var1 + 1, var2, var3 + 1);
      }
   }

   private void paintSquare(int var1, int var2, int var3, float var4, float var5, float var6, float var7) {
      int var8 = Core.TileScale;
      int var9 = (int)IsoUtils.XToScreenExact((float)var1, (float)(var2 + 1), (float)var3, 0);
      int var10 = (int)IsoUtils.YToScreenExact((float)var1, (float)(var2 + 1), (float)var3, 0);
      SpriteRenderer.instance.renderPoly((float)var9, (float)var10, (float)(var9 + 32 * var8), (float)(var10 - 16 * var8), (float)(var9 + 64 * var8), (float)var10, (float)(var9 + 32 * var8), (float)(var10 + 16 * var8), var4, var5, var6, var7);
   }

   void drawModData() {
      int var1 = this.m_z;
      IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, var1);
      int var3 = Core.getInstance().getScreenWidth() - 250;
      int var4 = 10;
      int var5 = TextManager.instance.getFontFromEnum(this.FONT).getLineHeight();
      int var10002;
      int var10003;
      if (var2 != null && var2.getModData() != null) {
         KahluaTable var6 = var2.getModData();
         var10002 = var4 += var5;
         var10003 = var2.getX();
         this.DrawString(var3, var10002, "MOD DATA x,y,z=" + var10003 + "," + var2.getY() + "," + var2.getZ());
         KahluaTableIterator var7 = var6.iterator();

         label60:
         while(true) {
            String var18;
            do {
               if (!var7.advance()) {
                  var4 += var5;
                  break label60;
               }

               var10002 = var4 += var5;
               var18 = var7.getKey().toString();
               this.DrawString(var3, var10002, var18 + " = " + var7.getValue().toString());
            } while(!(var7.getValue() instanceof KahluaTable));

            KahluaTableIterator var8 = ((KahluaTable)var7.getValue()).iterator();

            while(var8.advance()) {
               int var10001 = var3 + 8;
               var10002 = var4 += var5;
               var18 = var8.getKey().toString();
               this.DrawString(var10001, var10002, var18 + " = " + var8.getValue().toString());
            }
         }
      }

      if (var2 != null) {
         PropertyContainer var12 = var2.getProperties();
         ArrayList var14 = var12.getPropertyNames();
         if (!var14.isEmpty()) {
            var10002 = var4 += var5;
            var10003 = var2.getX();
            this.DrawString(var3, var10002, "PROPERTIES x,y,z=" + var10003 + "," + var2.getY() + "," + var2.getZ());
            Collections.sort(var14);
            Iterator var15 = var14.iterator();

            while(var15.hasNext()) {
               String var9 = (String)var15.next();
               this.DrawString(var3, var4 += var5, var9 + " = \"" + var12.Val(var9) + "\"");
            }
         }

         IsoFlagType[] var16 = IsoFlagType.values();
         int var17 = var16.length;

         for(int var10 = 0; var10 < var17; ++var10) {
            IsoFlagType var11 = var16[var10];
            if (var12.Is(var11)) {
               this.DrawString(var3, var4 += var5, var11.toString());
            }
         }
      }

      if (var2 != null) {
         ErosionData.Square var13 = var2.getErosionData();
         if (var13 != null) {
            var4 += var5;
            var10002 = var4 += var5;
            var10003 = var2.getX();
            this.DrawString(var3, var10002, "EROSION x,y,z=" + var10003 + "," + var2.getY() + "," + var2.getZ());
            this.DrawString(var3, var4 += var5, "init=" + var13.init);
            this.DrawString(var3, var4 += var5, "doNothing=" + var13.doNothing);
            this.DrawString(var3, var4 + var5, "chunk.init=" + var2.chunk.getErosionData().init);
         }
      }

   }

   void drawPlayerInfo() {
      int var1 = Core.getInstance().getScreenWidth() - 250;
      int var2 = Core.getInstance().getScreenHeight() / 2;
      int var3 = TextManager.instance.getFontFromEnum(this.FONT).getLineHeight();
      IsoGameCharacter var4 = IsoCamera.CamCharacter;
      this.DrawString(var1, var2 += var3, "bored = " + var4.getBodyDamage().getBoredomLevel());
      this.DrawString(var1, var2 += var3, "endurance = " + var4.getStats().endurance);
      this.DrawString(var1, var2 += var3, "fatigue = " + var4.getStats().fatigue);
      this.DrawString(var1, var2 += var3, "hunger = " + var4.getStats().hunger);
      this.DrawString(var1, var2 += var3, "pain = " + var4.getStats().Pain);
      this.DrawString(var1, var2 += var3, "panic = " + var4.getStats().Panic);
      this.DrawString(var1, var2 += var3, "stress = " + var4.getStats().getStress());
      this.DrawString(var1, var2 += var3, "clothingTemp = " + ((IsoPlayer)var4).getPlayerClothingTemperature());
      this.DrawString(var1, var2 += var3, "temperature = " + var4.getTemperature());
      this.DrawString(var1, var2 += var3, "thirst = " + var4.getStats().thirst);
      this.DrawString(var1, var2 += var3, "foodPoison = " + var4.getBodyDamage().getFoodSicknessLevel());
      this.DrawString(var1, var2 += var3, "poison = " + var4.getBodyDamage().getPoisonLevel());
      this.DrawString(var1, var2 += var3, "unhappy = " + var4.getBodyDamage().getUnhappynessLevel());
      this.DrawString(var1, var2 += var3, "infected = " + var4.getBodyDamage().isInfected());
      this.DrawString(var1, var2 += var3, "InfectionLevel = " + var4.getBodyDamage().getInfectionLevel());
      this.DrawString(var1, var2 += var3, "FakeInfectionLevel = " + var4.getBodyDamage().getFakeInfectionLevel());
      var2 += var3;
      this.DrawString(var1, var2 += var3, "WORLD");
      this.DrawString(var1, var2 + var3, "globalTemperature = " + IsoWorld.instance.getGlobalTemperature());
   }

   public LosUtil.TestResults lineClearCached(IsoCell var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8) {
      int var12 = var3 - var6;
      int var13 = var2 - var5;
      int var14 = var4 - var7;
      int var15 = var13 + 100;
      int var16 = var12 + 100;
      int var17 = var14 + 16;
      if (var15 >= 0 && var16 >= 0 && var17 >= 0 && var15 < 200 && var16 < 200) {
         LosUtil.TestResults var18 = LosUtil.TestResults.Clear;
         byte var19 = 1;
         float var20 = 0.5F;
         float var21 = 0.5F;
         IsoGridSquare var25 = var1.getGridSquare(var5, var6, var7);
         int var23;
         int var24;
         float var26;
         float var27;
         IsoGridSquare var28;
         if (Math.abs(var13) > Math.abs(var12) && Math.abs(var13) > Math.abs(var14)) {
            var26 = (float)var12 / (float)var13;
            var27 = (float)var14 / (float)var13;
            var20 += (float)var6;
            var21 += (float)var7;
            var13 = var13 < 0 ? -1 : 1;
            var26 *= (float)var13;

            for(var27 *= (float)var13; var5 != var2; var24 = (int)var21) {
               var5 += var13;
               var20 += var26;
               var21 += var27;
               var28 = var1.getGridSquare(var5, (int)var20, (int)var21);
               this.paintSquare(var5, (int)var20, (int)var21, 1.0F, 1.0F, 1.0F, 0.5F);
               if (var28 != null && var25 != null && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var8) == LosUtil.TestResults.Blocked) {
                  this.paintSquare(var5, (int)var20, (int)var21, 1.0F, 0.0F, 0.0F, 0.5F);
                  this.paintSquare(var25.getX(), var25.getY(), var25.getZ(), 1.0F, 0.0F, 0.0F, 0.5F);
                  var19 = 4;
               }

               var25 = var28;
               var23 = (int)var20;
            }
         } else {
            int var22;
            if (Math.abs(var12) >= Math.abs(var13) && Math.abs(var12) > Math.abs(var14)) {
               var26 = (float)var13 / (float)var12;
               var27 = (float)var14 / (float)var12;
               var20 += (float)var5;
               var21 += (float)var7;
               var12 = var12 < 0 ? -1 : 1;
               var26 *= (float)var12;

               for(var27 *= (float)var12; var6 != var3; var24 = (int)var21) {
                  var6 += var12;
                  var20 += var26;
                  var21 += var27;
                  var28 = var1.getGridSquare((int)var20, var6, (int)var21);
                  this.paintSquare((int)var20, var6, (int)var21, 1.0F, 1.0F, 1.0F, 0.5F);
                  if (var28 != null && var25 != null && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var8) == LosUtil.TestResults.Blocked) {
                     this.paintSquare((int)var20, var6, (int)var21, 1.0F, 0.0F, 0.0F, 0.5F);
                     this.paintSquare(var25.getX(), var25.getY(), var25.getZ(), 1.0F, 0.0F, 0.0F, 0.5F);
                     var19 = 4;
                  }

                  var25 = var28;
                  var22 = (int)var20;
               }
            } else {
               var26 = (float)var13 / (float)var14;
               var27 = (float)var12 / (float)var14;
               var20 += (float)var5;
               var21 += (float)var6;
               var14 = var14 < 0 ? -1 : 1;
               var26 *= (float)var14;

               for(var27 *= (float)var14; var7 != var4; var23 = (int)var21) {
                  var7 += var14;
                  var20 += var26;
                  var21 += var27;
                  var28 = var1.getGridSquare((int)var20, (int)var21, var7);
                  this.paintSquare((int)var20, (int)var21, var7, 1.0F, 1.0F, 1.0F, 0.5F);
                  if (var28 != null && var25 != null && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var8) == LosUtil.TestResults.Blocked) {
                     var19 = 4;
                  }

                  var25 = var28;
                  var22 = (int)var20;
               }
            }
         }

         if (var19 == 1) {
            return LosUtil.TestResults.Clear;
         } else if (var19 == 2) {
            return LosUtil.TestResults.ClearThroughOpenDoor;
         } else if (var19 == 3) {
            return LosUtil.TestResults.ClearThroughWindow;
         } else {
            return var19 == 4 ? LosUtil.TestResults.Blocked : LosUtil.TestResults.Blocked;
         }
      } else {
         return LosUtil.TestResults.Blocked;
      }
   }

   private void DrawString(int var1, int var2, String var3) {
      int var4 = TextManager.instance.MeasureStringX(this.FONT, var3);
      int var5 = TextManager.instance.getFontFromEnum(this.FONT).getLineHeight();
      SpriteRenderer.instance.renderi((Texture)null, var1 - 1, var2, var4 + 2, var5, 0.0F, 0.0F, 0.0F, 0.8F, (Consumer)null);
      TextManager.instance.DrawString(this.FONT, (double)var1, (double)var2, var3, 1.0, 1.0, 1.0, 1.0);
   }

   public ConfigOption getOptionByName(String var1) {
      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         ConfigOption var3 = (ConfigOption)this.options.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public int getOptionCount() {
      return this.options.size();
   }

   public ConfigOption getOptionByIndex(int var1) {
      return (ConfigOption)this.options.get(var1);
   }

   public void setBoolean(String var1, boolean var2) {
      ConfigOption var3 = this.getOptionByName(var1);
      if (var3 instanceof BooleanConfigOption) {
         ((BooleanConfigOption)var3).setValue(var2);
      }

   }

   public boolean getBoolean(String var1) {
      ConfigOption var2 = this.getOptionByName(var1);
      return var2 instanceof BooleanConfigOption ? ((BooleanConfigOption)var2).getValue() : false;
   }

   public void save() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "debugChunkState-options.ini";
      ConfigFile var2 = new ConfigFile();
      var2.write(var1, 1, this.options);
   }

   public void load() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "debugChunkState-options.ini";
      ConfigFile var2 = new ConfigFile();
      if (var2.read(var1)) {
         for(int var3 = 0; var3 < var2.getOptions().size(); ++var3) {
            ConfigOption var4 = (ConfigOption)var2.getOptions().get(var3);
            ConfigOption var5 = this.getOptionByName(var4.getName());
            if (var5 != null) {
               var5.parse(var4.getValueAsString());
            }
         }
      }

   }

   public class BooleanDebugOption extends BooleanConfigOption {
      public BooleanDebugOption(String var2, boolean var3) {
         super(var2, var3);
         DebugChunkState.this.options.add(this);
      }
   }

   private class FloodFill {
      private IsoGridSquare start = null;
      private final int FLOOD_SIZE = 11;
      private BooleanGrid visited = new BooleanGrid(11, 11);
      private Stack<IsoGridSquare> stack = new Stack();
      private IsoBuilding building = null;
      private Mover mover = null;

      private FloodFill() {
      }

      void calculate(Mover var1, IsoGridSquare var2) {
         this.start = var2;
         this.mover = var1;
         if (this.start.getRoom() != null) {
            this.building = this.start.getRoom().getBuilding();
         }

         boolean var3 = false;
         boolean var4 = false;
         if (this.push(this.start.getX(), this.start.getY())) {
            while((var2 = this.pop()) != null) {
               int var6 = var2.getX();

               int var5;
               for(var5 = var2.getY(); this.shouldVisit(var6, var5, var6, var5 - 1); --var5) {
               }

               var4 = false;
               var3 = false;

               while(true) {
                  this.visited.setValue(this.gridX(var6), this.gridY(var5), true);
                  if (!var3 && this.shouldVisit(var6, var5, var6 - 1, var5)) {
                     if (!this.push(var6 - 1, var5)) {
                        return;
                     }

                     var3 = true;
                  } else if (var3 && !this.shouldVisit(var6, var5, var6 - 1, var5)) {
                     var3 = false;
                  } else if (var3 && !this.shouldVisit(var6 - 1, var5, var6 - 1, var5 - 1) && !this.push(var6 - 1, var5)) {
                     return;
                  }

                  if (!var4 && this.shouldVisit(var6, var5, var6 + 1, var5)) {
                     if (!this.push(var6 + 1, var5)) {
                        return;
                     }

                     var4 = true;
                  } else if (var4 && !this.shouldVisit(var6, var5, var6 + 1, var5)) {
                     var4 = false;
                  } else if (var4 && !this.shouldVisit(var6 + 1, var5, var6 + 1, var5 - 1) && !this.push(var6 + 1, var5)) {
                     return;
                  }

                  ++var5;
                  if (!this.shouldVisit(var6, var5 - 1, var6, var5)) {
                     break;
                  }
               }
            }

         }
      }

      boolean shouldVisit(int var1, int var2, int var3, int var4) {
         if (this.gridX(var3) < 11 && this.gridX(var3) >= 0) {
            if (this.gridY(var4) < 11 && this.gridY(var4) >= 0) {
               if (this.visited.getValue(this.gridX(var3), this.gridY(var4))) {
                  return false;
               } else {
                  IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, this.start.getZ());
                  if (var5 == null) {
                     return false;
                  } else if (!var5.Has(IsoObjectType.stairsBN) && !var5.Has(IsoObjectType.stairsMN) && !var5.Has(IsoObjectType.stairsTN)) {
                     if (!var5.Has(IsoObjectType.stairsBW) && !var5.Has(IsoObjectType.stairsMW) && !var5.Has(IsoObjectType.stairsTW)) {
                        if (var5.getRoom() != null && this.building == null) {
                           return false;
                        } else if (var5.getRoom() == null && this.building != null) {
                           return false;
                        } else {
                           return !IsoWorld.instance.CurrentCell.blocked(this.mover, var3, var4, this.start.getZ(), var1, var2, this.start.getZ());
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      boolean push(int var1, int var2) {
         IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, this.start.getZ());
         this.stack.push(var3);
         return true;
      }

      IsoGridSquare pop() {
         return this.stack.isEmpty() ? null : (IsoGridSquare)this.stack.pop();
      }

      int gridX(int var1) {
         return var1 - (this.start.getX() - 5);
      }

      int gridY(int var1) {
         return var1 - (this.start.getY() - 5);
      }

      int gridX(IsoGridSquare var1) {
         return var1.getX() - (this.start.getX() - 5);
      }

      int gridY(IsoGridSquare var1) {
         return var1.getY() - (this.start.getY() - 5);
      }

      void draw() {
         int var1 = this.start.getX() - 5;
         int var2 = this.start.getY() - 5;

         for(int var3 = 0; var3 < 11; ++var3) {
            for(int var4 = 0; var4 < 11; ++var4) {
               if (this.visited.getValue(var4, var3)) {
                  int var5 = (int)IsoUtils.XToScreenExact((float)(var1 + var4), (float)(var2 + var3 + 1), (float)this.start.getZ(), 0);
                  int var6 = (int)IsoUtils.YToScreenExact((float)(var1 + var4), (float)(var2 + var3 + 1), (float)this.start.getZ(), 0);
                  SpriteRenderer.instance.renderPoly((float)var5, (float)var6, (float)(var5 + 32), (float)(var6 - 16), (float)(var5 + 64), (float)var6, (float)(var5 + 32), (float)(var6 + 16), 1.0F, 1.0F, 0.0F, 0.5F);
               }
            }
         }

      }
   }
}
