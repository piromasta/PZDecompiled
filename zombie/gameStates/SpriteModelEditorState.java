package zombie.gameStates;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.iso.SpriteModel;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.spriteModel.SpriteModelManager;
import zombie.spriteModel.TilesetImageCreator;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

public final class SpriteModelEditorState extends GameState {
   public static SpriteModelEditorState instance;
   private EditVehicleState.LuaEnvironment m_luaEnv;
   private boolean bExit = false;
   private final ArrayList<UIElementInterface> m_gameUI = new ArrayList();
   private final ArrayList<UIElementInterface> m_selfUI = new ArrayList();
   private boolean m_bSuspendUI;
   private KahluaTable m_table = null;
   private final ArrayList<String> m_clipNames = new ArrayList();
   private static final int VERSION = 1;
   private final ArrayList<ConfigOption> options = new ArrayList();
   private final BooleanDebugOption DrawGrid = new BooleanDebugOption("DrawGrid", false);
   private final BooleanDebugOption DrawNorthWall = new BooleanDebugOption("DrawNorthWall", false);
   private final BooleanDebugOption DrawWestWall = new BooleanDebugOption("DrawWestWall", false);

   public SpriteModelEditorState() {
   }

   public void enter() {
      instance = this;
      this.load();
      if (this.m_luaEnv == null) {
         this.m_luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
      }

      this.saveGameUI();
      if (this.m_selfUI.size() == 0) {
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_luaEnv.env.rawget("SpriteModelEditor_InitUI"), new Object[0]);
         if (this.m_table != null && this.m_table.getMetatable() != null) {
            this.m_table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
         }
      } else {
         UIManager.UI.addAll(this.m_selfUI);
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("showUI"), this.m_table);
      }

      this.bExit = false;
      DebugOptions.instance.IsoSprite.ForceNearestMagFilter.setValue(true);
   }

   public void yield() {
      this.restoreGameUI();
   }

   public void reenter() {
      this.saveGameUI();
   }

   public void exit() {
      DebugOptions.instance.IsoSprite.ForceNearestMagFilter.setValue(false);
      this.save();
      this.restoreGameUI();
   }

   public void render() {
      byte var1 = 0;
      Core.getInstance().StartFrame(var1, true);
      this.renderScene();
      Core.getInstance().EndFrame(var1);
      Core.getInstance().RenderOffScreenBuffer();
      UIManager.useUIFBO = Core.getInstance().supportsFBO() && Core.getInstance().getOptionUIFBO();
      if (Core.getInstance().StartFrameUI()) {
         this.renderUI();
      }

      Core.getInstance().EndFrameUI();
   }

   public GameStateMachine.StateAction update() {
      if (!this.bExit && !GameKeyboard.isKeyPressed(66) && !GameKeyboard.isKeyPressed(1)) {
         this.updateScene();
         return GameStateMachine.StateAction.Remain;
      } else {
         SpriteModelManager.getInstance().toScriptManager();
         return GameStateMachine.StateAction.Continue;
      }
   }

   public static SpriteModelEditorState checkInstance() {
      if (instance != null) {
         if (instance.m_table != null && instance.m_table.getMetatable() != null) {
            if (instance.m_table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
               instance = null;
            }
         } else {
            instance = null;
         }
      }

      return instance == null ? new SpriteModelEditorState() : instance;
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

   private void updateScene() {
      ModelManager.instance.update();
      if (GameKeyboard.isKeyPressed(17)) {
         DebugOptions.instance.Model.Render.Wireframe.setValue(!DebugOptions.instance.Model.Render.Wireframe.getValue());
      }

   }

   private void renderScene() {
   }

   private void renderUI() {
      UIManager.render();
   }

   public void setTable(KahluaTable var1) {
      this.m_table = var1;
   }

   public Object fromLua0(String var1) {
      switch (var1) {
         case "exit":
            this.bExit = true;
            return null;
         default:
            throw new IllegalArgumentException("unhandled \"" + var1 + "\"");
      }
   }

   public Object fromLua1(String var1, Object var2) {
      String var5;
      switch (var1) {
         case "getClipNames":
            var5 = (String)var2;
            ModelScript var6 = ScriptManager.instance.getModelScript(var5);
            String var7 = var6.animationsMesh;
            if (var7 == null) {
               var7 = var6.meshName;
               if (var7 != null && var7.contains("/")) {
                  var7 = var7.substring(var7.lastIndexOf(47) + 1);
               }
            }

            AnimationsMesh var8 = ScriptManager.instance.getAnimationsMesh(var7);
            if (var8 != null && var8.modelMesh != null && var8.modelMesh.skinningData != null) {
               HashMap var9 = var8.modelMesh.skinningData.AnimationClips;
               if (this.m_clipNames.isEmpty() || !var9.containsKey(this.m_clipNames.get(0))) {
                  this.m_clipNames.clear();
                  Iterator var10 = var9.values().iterator();

                  while(var10.hasNext()) {
                     AnimationClip var11 = (AnimationClip)var10.next();
                     this.m_clipNames.add(var11.Name);
                  }

                  this.m_clipNames.sort(Comparator.naturalOrder());
               }

               return this.m_clipNames;
            }

            this.m_clipNames.clear();
            return this.m_clipNames;
         case "writeSpriteModelsFile":
            var5 = (String)var2;
            SpriteModelManager.getInstance().write(var5);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", var1, var2));
      }
   }

   public Object fromLua2(String var1, Object var2, Object var3) {
      byte var5 = -1;
      var1.hashCode();
      switch (var5) {
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", var1, var2, var3));
      }
   }

   public Object fromLua3(String var1, Object var2, Object var3, Object var4) {
      switch (var1) {
         case "saveTilesetImage":
            String var7 = (String)var2;
            String var8 = (String)var3;
            String var9 = (String)var4;
            TilesetImageCreator var10 = new TilesetImageCreator();
            var10.createImage(var7, var8, var9);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4));
      }
   }

   public Object fromLua4(String var1, Object var2, Object var3, Object var4, Object var5) {
      String var8;
      String var9;
      int var10;
      int var11;
      switch (var1) {
         case "clearTileProperties":
            var8 = (String)var2;
            var9 = (String)var3;
            var10 = ((Double)var4).intValue();
            var11 = ((Double)var5).intValue();
            SpriteModelManager.getInstance().clearTileProperties(var8, var9, var10, var11);
            return null;
         case "getOrCreateTileProperties":
            var8 = (String)var2;
            var9 = (String)var3;
            var10 = ((Double)var4).intValue();
            var11 = ((Double)var5).intValue();
            SpriteModel var12 = SpriteModelManager.getInstance().getTileProperties(var8, var9, var10, var11);
            if (var12 == null) {
               var12 = new SpriteModel();
               SpriteModelManager.getInstance().setTileProperties(var8, var9, var10, var11, var12);
               var12 = SpriteModelManager.getInstance().getTileProperties(var8, var9, var10, var11);
            }

            return var12;
         case "getTileProperties":
            var8 = (String)var2;
            var9 = (String)var3;
            var10 = ((Double)var4).intValue();
            var11 = ((Double)var5).intValue();
            return SpriteModelManager.getInstance().getTileProperties(var8, var9, var10, var11);
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5));
      }
   }

   public Object fromLua5(String var1, Object var2, Object var3, Object var4, Object var5, Object var6) {
      switch (var1) {
         case "setTileModelScript":
            String var9 = (String)var2;
            String var10 = (String)var3;
            int var11 = ((Double)var4).intValue();
            int var12 = ((Double)var5).intValue();
            String var13 = (String)var6;
            SpriteModel var14 = SpriteModelManager.getInstance().getTileProperties(var9, var10, var11, var12);
            if (var14 == null) {
               var14 = new SpriteModel();
               SpriteModelManager.getInstance().setTileProperties(var9, var10, var11, var12, var14);
            } else {
               var14.modelScriptName = var13;
            }

            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", var1, var2, var3, var4, var5, var6));
      }
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
      String var1 = var10000 + File.separator + "SpriteModelEditorState-options.ini";
      ConfigFile var2 = new ConfigFile();
      var2.write(var1, 1, this.options);
   }

   public void load() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "SpriteModelEditorState-options.ini";
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
         SpriteModelEditorState.this.options.add(this);
      }
   }
}
