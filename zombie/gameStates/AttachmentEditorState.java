package zombie.gameStates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

public final class AttachmentEditorState extends GameState {
   public static AttachmentEditorState instance;
   private EditVehicleState.LuaEnvironment m_luaEnv;
   private boolean bExit = false;
   private final ArrayList<UIElementInterface> m_gameUI = new ArrayList();
   private final ArrayList<UIElementInterface> m_selfUI = new ArrayList();
   private boolean m_bSuspendUI;
   private KahluaTable m_table = null;
   private final ArrayList<String> m_clipNames = new ArrayList();

   public AttachmentEditorState() {
   }

   public void enter() {
      instance = this;
      if (this.m_luaEnv == null) {
         this.m_luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
      }

      this.saveGameUI();
      if (this.m_selfUI.size() == 0) {
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_luaEnv.env.rawget("AttachmentEditorState_InitUI"), new Object[0]);
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
      this.restoreGameUI();
   }

   public void render() {
      byte var1 = 0;
      Core.getInstance().StartFrame(var1, true);
      this.renderScene();
      Core.getInstance().EndFrame(var1);
      Core.getInstance().RenderOffScreenBuffer();
      if (Core.getInstance().StartFrameUI()) {
         this.renderUI();
      }

      Core.getInstance().EndFrameUI();
   }

   public GameStateMachine.StateAction update() {
      if (!this.bExit && !GameKeyboard.isKeyPressed(65)) {
         this.updateScene();
         return GameStateMachine.StateAction.Remain;
      } else {
         return GameStateMachine.StateAction.Continue;
      }
   }

   public static AttachmentEditorState checkInstance() {
      if (instance != null) {
         if (instance.m_table != null && instance.m_table.getMetatable() != null) {
            if (instance.m_table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
               instance = null;
            }
         } else {
            instance = null;
         }
      }

      return instance == null ? new AttachmentEditorState() : instance;
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
         case "getClipNames":
            if (this.m_clipNames.isEmpty()) {
               Collection var4 = ModelManager.instance.getAllAnimationClips();
               Iterator var5 = var4.iterator();

               while(var5.hasNext()) {
                  AnimationClip var6 = (AnimationClip)var5.next();
                  this.m_clipNames.add(var6.Name);
               }

               this.m_clipNames.sort(Comparator.naturalOrder());
            }

            return this.m_clipNames;
         case "exit":
            this.bExit = true;
            return null;
         default:
            throw new IllegalArgumentException("unhandled \"" + var1 + "\"");
      }
   }

   public Object fromLua1(String var1, Object var2) {
      switch (var1) {
         case "writeScript":
            ModelScript var5 = ScriptManager.instance.getModelScript((String)var2);
            if (var5 == null) {
               throw new NullPointerException("model script \"" + var2 + "\" not found");
            }

            ArrayList var6 = readScript(var5.getFileName());
            if (var6 != null) {
               String var7 = var5.getFileName();
               if (updateScript(var7, var6, var5)) {
                  String var8 = ZomboidFileSystem.instance.getString(var7);
                  this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("wroteScript"), new Object[]{this.m_table, var8});
               }
            }

            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", var1, var2));
      }
   }

   public static ArrayList<String> readScript(String var0) {
      StringBuilder var1 = new StringBuilder();
      var0 = ZomboidFileSystem.instance.getString(var0);
      File var2 = new File(var0);

      try {
         FileReader var3 = new FileReader(var2);

         try {
            BufferedReader var4 = new BufferedReader(var3);

            try {
               String var5 = System.lineSeparator();

               String var6;
               while((var6 = var4.readLine()) != null) {
                  var1.append(var6);
                  var1.append(var5);
               }
            } catch (Throwable var9) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var4.close();
         } catch (Throwable var10) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         var3.close();
      } catch (Throwable var11) {
         ExceptionLogger.logException(var11);
         return null;
      }

      String var12 = ScriptParser.stripComments(var1.toString());
      return ScriptParser.parseTokens(var12);
   }

   public static boolean updateScript(String var0, ArrayList<String> var1, ModelScript var2) {
      var0 = ZomboidFileSystem.instance.getString(var0);

      for(int var3 = var1.size() - 1; var3 >= 0; --var3) {
         String var4 = ((String)var1.get(var3)).trim();
         int var5 = var4.indexOf("{");
         int var6 = var4.lastIndexOf("}");
         String var7 = var4.substring(0, var5);
         if (var7.startsWith("module")) {
            var7 = var4.substring(0, var5).trim();
            String[] var8 = var7.split("\\s+");
            String var9 = var8.length > 1 ? var8[1].trim() : "";
            if (var9.equals(var2.getModule().getName())) {
               String var10 = var4.substring(var5 + 1, var6).trim();
               ArrayList var11 = ScriptParser.parseTokens(var10);

               for(int var12 = var11.size() - 1; var12 >= 0; --var12) {
                  String var13 = ((String)var11.get(var12)).trim();
                  if (var13.startsWith("model")) {
                     var5 = var13.indexOf("{");
                     var7 = var13.substring(0, var5).trim();
                     var8 = var7.split("\\s+");
                     String var14 = var8.length > 1 ? var8[1].trim() : "";
                     if (var14.equals(var2.getName())) {
                        var13 = modelScriptToText(var2, var13).trim();
                        var11.set(var12, var13);
                        String var15 = System.lineSeparator();
                        String var16 = String.join(var15 + "\t", var11);
                        var16 = "module " + var9 + var15 + "{" + var15 + "\t" + var16 + var15 + "}" + var15;
                        var1.set(var3, var16);
                        return writeScript(var0, var1);
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static String modelScriptToText(ModelScript var0, String var1) {
      ScriptParser.Block var2 = ScriptParser.parse(var1);
      var2 = (ScriptParser.Block)var2.children.get(0);

      int var3;
      for(var3 = var2.children.size() - 1; var3 >= 0; --var3) {
         ScriptParser.Block var4 = (ScriptParser.Block)var2.children.get(var3);
         if ("attachment".equals(var4.type)) {
            var2.elements.remove(var4);
            var2.children.remove(var3);
         }
      }

      for(var3 = 0; var3 < var0.getAttachmentCount(); ++var3) {
         ModelAttachment var7 = var0.getAttachment(var3);
         ScriptParser.Block var5 = var2.getBlock("attachment", var7.getId());
         if (var5 == null) {
            var5 = new ScriptParser.Block();
            var5.type = "attachment";
            var5.id = var7.getId();
            var5.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var7.getOffset().x(), var7.getOffset().y(), var7.getOffset().z()));
            var5.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var7.getRotate().x(), var7.getRotate().y(), var7.getRotate().z()));
            if (var7.getScale() != 1.0F) {
               var5.setValue("scale", String.format(Locale.US, "%.4f", var7.getScale()));
            }

            if (var7.getBone() != null) {
               var5.setValue("bone", var7.getBone());
            }

            var2.elements.add(var5);
            var2.children.add(var5);
         } else {
            var5.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var7.getOffset().x(), var7.getOffset().y(), var7.getOffset().z()));
            var5.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var7.getRotate().x(), var7.getRotate().y(), var7.getRotate().z()));
            if (var7.getScale() != 1.0F) {
               var5.setValue("scale", String.format(Locale.US, "%.4f", var7.getScale()));
            }
         }
      }

      StringBuilder var6 = new StringBuilder();
      String var8 = System.lineSeparator();
      var2.prettyPrint(1, var6, var8);
      return var6.toString();
   }

   public static boolean writeScript(String var0, ArrayList<String> var1) {
      String var2 = ZomboidFileSystem.instance.getString(var0);
      File var3 = new File(var2);

      try {
         FileWriter var4 = new FileWriter(var3);

         boolean var13;
         try {
            BufferedWriter var5 = new BufferedWriter(var4);

            try {
               DebugLog.General.printf("writing %s\n", var0);
               Iterator var6 = var1.iterator();

               while(var6.hasNext()) {
                  String var7 = (String)var6.next();
                  var5.write(var7);
               }

               var13 = true;
            } catch (Throwable var10) {
               try {
                  var5.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            var5.close();
         } catch (Throwable var11) {
            try {
               var4.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         var4.close();
         return var13;
      } catch (Throwable var12) {
         ExceptionLogger.logException(var12);
         return false;
      }
   }
}
