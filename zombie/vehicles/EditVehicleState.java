package zombie.vehicles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.AttachmentEditorState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;

public final class EditVehicleState extends GameState {
   public static EditVehicleState instance;
   private LuaEnvironment m_luaEnv;
   private boolean bExit = false;
   private String m_initialScript = null;
   private final ArrayList<UIElementInterface> m_gameUI = new ArrayList();
   private final ArrayList<UIElementInterface> m_selfUI = new ArrayList();
   private boolean m_bSuspendUI;
   private KahluaTable m_table = null;

   public EditVehicleState() {
      instance = this;
   }

   public void enter() {
      instance = this;
      if (this.m_luaEnv == null) {
         this.m_luaEnv = new LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
      }

      this.saveGameUI();
      if (this.m_selfUI.size() == 0) {
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_luaEnv.env.rawget("EditVehicleState_InitUI"), new Object[0]);
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

   public static EditVehicleState checkInstance() {
      if (instance != null) {
         if (instance.m_table != null && instance.m_table.getMetatable() != null) {
            if (instance.m_table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
               instance = null;
            }
         } else {
            instance = null;
         }
      }

      return instance == null ? new EditVehicleState() : instance;
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

   public void setScript(String var1) {
      if (this.m_table == null) {
         this.m_initialScript = var1;
      } else {
         this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("setScript"), new Object[]{this.m_table, var1});
      }

   }

   public Object fromLua0(String var1) {
      switch (var1) {
         case "exit":
            this.bExit = true;
            return null;
         case "getInitialScript":
            return this.m_initialScript;
         default:
            throw new IllegalArgumentException("unhandled \"" + var1 + "\"");
      }
   }

   public Object fromLua1(String var1, Object var2) {
      switch (var1) {
         case "writeScript":
            VehicleScript var5 = ScriptManager.instance.getVehicle((String)var2);
            if (var5 == null) {
               throw new NullPointerException("vehicle script \"" + var2 + "\" not found");
            }

            ArrayList var6 = this.readScript(var5.getFileName());
            if (var6 != null) {
               this.updateScript(var5.getFileName(), var6, var5);
            }

            this.updateModelScripts(var5);
            return null;
         default:
            throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", var1, var2));
      }
   }

   private ArrayList<String> readScript(String var1) {
      StringBuilder var2 = new StringBuilder();
      var1 = ZomboidFileSystem.instance.getString(var1);
      File var3 = new File(var1);

      try {
         FileReader var4 = new FileReader(var3);

         try {
            BufferedReader var5 = new BufferedReader(var4);

            try {
               String var6 = System.lineSeparator();

               String var7;
               while((var7 = var5.readLine()) != null) {
                  var2.append(var7);
                  var2.append(var6);
               }
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
      } catch (Throwable var12) {
         ExceptionLogger.logException(var12);
         return null;
      }

      String var13 = ScriptParser.stripComments(var2.toString());
      return ScriptParser.parseTokens(var13);
   }

   private void updateScript(String var1, ArrayList<String> var2, VehicleScript var3) {
      var1 = ZomboidFileSystem.instance.getString(var1);

      for(int var4 = var2.size() - 1; var4 >= 0; --var4) {
         String var5 = ((String)var2.get(var4)).trim();
         int var6 = var5.indexOf("{");
         int var7 = var5.lastIndexOf("}");
         String var8 = var5.substring(0, var6);
         if (var8.startsWith("module")) {
            var8 = var5.substring(0, var6).trim();
            String[] var9 = var8.split("\\s+");
            String var10 = var9.length > 1 ? var9[1].trim() : "";
            if (var10.equals(var3.getModule().getName())) {
               String var11 = var5.substring(var6 + 1, var7).trim();
               ArrayList var12 = ScriptParser.parseTokens(var11);

               for(int var13 = var12.size() - 1; var13 >= 0; --var13) {
                  String var14 = ((String)var12.get(var13)).trim();
                  if (var14.startsWith("vehicle")) {
                     var6 = var14.indexOf("{");
                     var8 = var14.substring(0, var6).trim();
                     var9 = var8.split("\\s+");
                     String var15 = var9.length > 1 ? var9[1].trim() : "";
                     if (var15.equals(var3.getName())) {
                        var14 = this.vehicleScriptToText(var3, var14).trim();
                        var12.set(var13, var14);
                        String var16 = System.lineSeparator();
                        String var17 = String.join(var16 + "\t", var12);
                        var17 = "module " + var10 + var16 + "{" + var16 + "\t" + var17 + var16 + "}" + var16;
                        var2.set(var4, var17);
                        this.writeScript(var1, var2);
                        return;
                     }
                  }
               }
            }
         }
      }

   }

   private String vehicleScriptToText(VehicleScript var1, String var2) {
      float var3 = var1.getModelScale();
      ScriptParser.Block var4 = ScriptParser.parse(var2);
      var4 = (ScriptParser.Block)var4.children.get(0);
      VehicleScript.Model var5 = var1.getModel();
      ScriptParser.Block var6 = var4.getBlock("model", (String)null);
      if (var5 != null && var6 != null) {
         float var7 = var1.getModelScale();
         var6.setValue("scale", String.format(Locale.US, "%.4f", var7));
         Vector3f var8 = var1.getModel().getOffset();
         var6.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var8.x / var3, var8.y / var3, var8.z / var3));
      }

      ArrayList var12 = new ArrayList();

      int var13;
      for(var13 = 0; var13 < var4.children.size(); ++var13) {
         ScriptParser.Block var14 = (ScriptParser.Block)var4.children.get(var13);
         if ("physics".equals(var14.type)) {
            if (var12.size() == var1.getPhysicsShapeCount()) {
               var4.elements.remove(var14);
               var4.children.remove(var13);
               --var13;
            } else {
               var12.add(var14);
            }
         }
      }

      for(var13 = 0; var13 < var1.getPhysicsShapeCount(); ++var13) {
         VehicleScript.PhysicsShape var15 = var1.getPhysicsShape(var13);
         boolean var17 = var13 < var12.size();
         ScriptParser.Block var9 = var17 ? (ScriptParser.Block)var12.get(var13) : new ScriptParser.Block();
         var9.type = "physics";
         var9.id = var15.getTypeString();
         if (var17) {
            var9.elements.clear();
            var9.children.clear();
            var9.values.clear();
         }

         var9.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var15.getOffset().x() / var3, var15.getOffset().y() / var3, var15.getOffset().z() / var3));
         if (var15.type == 1) {
            var9.setValue("extents", String.format(Locale.US, "%.4f %.4f %.4f", var15.getExtents().x() / var3, var15.getExtents().y() / var3, var15.getExtents().z() / var3));
            var9.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var15.getRotate().x(), var15.getRotate().y(), var15.getRotate().z()));
         }

         if (var15.type == 2) {
            var9.setValue("radius", String.format(Locale.US, "%.4f", var15.getRadius() / var3));
         }

         if (var15.type == 3) {
            var9.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var15.getRotate().x(), var15.getRotate().y(), var15.getRotate().z()));
            var9.setValue("physicsShapeScript", var15.getPhysicsShapeScript());
            var9.setValue("scale", String.format(Locale.US, "%.4f", var15.getExtents().x() / var3));
         }

         if (!var17) {
            var4.elements.add(var9);
            var4.children.add(var9);
         }
      }

      this.removeAttachments(var4);

      for(var13 = 0; var13 < var1.getAttachmentCount(); ++var13) {
         ModelAttachment var16 = var1.getAttachment(var13);
         this.attachmentToBlock(var1, var16, var4);
      }

      Vector3f var20 = var1.getExtents();
      var4.setValue("extents", String.format(Locale.US, "%.4f %.4f %.4f", var20.x / var3, var20.y / var3, var20.z / var3));
      if (var1.hasPhysicsChassisShape()) {
         var20 = var1.getPhysicsChassisShape();
         var4.setValue("physicsChassisShape", String.format(Locale.US, "%.4f %.4f %.4f", var20.x / var3, var20.y / var3, var20.z / var3));
      }

      var20 = var1.getCenterOfMassOffset();
      var4.setValue("centerOfMassOffset", String.format(Locale.US, "%.4f %.4f %.4f", var20.x / var3, var20.y / var3, var20.z / var3));
      Vector2f var25 = var1.getShadowExtents();
      boolean var18 = var4.getValue("shadowExtents") != null;
      var4.setValue("shadowExtents", String.format(Locale.US, "%.4f %.4f", var25.x / var3, var25.y / var3));
      if (!var18) {
         var4.moveValueAfter("shadowExtents", "centerOfMassOffset");
      }

      var25 = var1.getShadowOffset();
      var18 = var4.getValue("shadowOffset") != null;
      var4.setValue("shadowOffset", String.format(Locale.US, "%.4f %.4f", var25.x / var3, var25.y / var3));
      if (!var18) {
         var4.moveValueAfter("shadowOffset", "shadowExtents");
      }

      ScriptParser.Block var19;
      for(var13 = 0; var13 < var1.getAreaCount(); ++var13) {
         VehicleScript.Area var21 = var1.getArea(var13);
         var19 = var4.getBlock("area", var21.getId());
         if (var19 != null) {
            var19.setValue("xywh", String.format(Locale.US, "%.4f %.4f %.4f %.4f", var21.getX() / (double)var3, var21.getY() / (double)var3, var21.getW() / (double)var3, var21.getH() / (double)var3));
         }
      }

      ScriptParser.Block var11;
      for(var13 = 0; var13 < var1.getPartCount(); ++var13) {
         VehicleScript.Part var22 = var1.getPart(var13);
         var19 = var4.getBlock("part", var22.getId());
         if (var19 != null) {
            for(int var23 = 0; var23 < var22.getModelCount(); ++var23) {
               VehicleScript.Model var10 = var22.getModel(var23);
               var11 = var19.getBlock("model", var10.getId());
               if (var11 != null) {
                  var11.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var10.offset.x, var10.offset.y, var10.offset.z));
                  var11.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var10.rotate.x, var10.rotate.y, var10.rotate.z));
               }
            }
         }
      }

      for(var13 = 0; var13 < var1.getPassengerCount(); ++var13) {
         VehicleScript.Passenger var24 = var1.getPassenger(var13);
         var19 = var4.getBlock("passenger", var24.getId());
         if (var19 != null) {
            Iterator var26 = var24.positions.iterator();

            while(var26.hasNext()) {
               VehicleScript.Position var29 = (VehicleScript.Position)var26.next();
               var11 = var19.getBlock("position", var29.id);
               if (var11 != null) {
                  var11.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var29.offset.x / var3, var29.offset.y / var3, var29.offset.z / var3));
                  var11.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var29.rotate.x / var3, var29.rotate.y / var3, var29.rotate.z / var3));
               }
            }
         }
      }

      for(var13 = 0; var13 < var1.getWheelCount(); ++var13) {
         VehicleScript.Wheel var27 = var1.getWheel(var13);
         var19 = var4.getBlock("wheel", var27.getId());
         if (var19 != null) {
            var19.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var27.offset.x / var3, var27.offset.y / var3, var27.offset.z / var3));
         }
      }

      StringBuilder var30 = new StringBuilder();
      String var28 = System.lineSeparator();
      var4.prettyPrint(1, var30, var28);
      return var30.toString();
   }

   private void removeAttachments(ScriptParser.Block var1) {
      for(int var2 = var1.children.size() - 1; var2 >= 0; --var2) {
         ScriptParser.Block var3 = (ScriptParser.Block)var1.children.get(var2);
         if ("attachment".equals(var3.type)) {
            var1.elements.remove(var3);
            var1.children.remove(var2);
         }
      }

   }

   private void attachmentToBlock(VehicleScript var1, ModelAttachment var2, ScriptParser.Block var3) {
      float var4 = var1.getModelScale();
      ScriptParser.Block var5 = var3.getBlock("attachment", var2.getId());
      if (var5 == null) {
         var5 = new ScriptParser.Block();
         var5.type = "attachment";
         var5.id = var2.getId();
         var3.elements.add(var5);
         var3.children.add(var5);
      }

      var5.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", var2.getOffset().x() / var4, var2.getOffset().y() / var4, var2.getOffset().z() / var4));
      var5.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", var2.getRotate().x(), var2.getRotate().y(), var2.getRotate().z()));
      if (var2.getBone() != null) {
         var5.setValue("bone", var2.getBone());
      }

      if (var2.getCanAttach() != null) {
         var5.setValue("canAttach", PZArrayUtil.arrayToString((Iterable)var2.getCanAttach(), "", "", ","));
      }

      if (var2.getZOffset() != 0.0F) {
         var5.setValue("zoffset", String.format(Locale.US, "%.4f", var2.getZOffset()));
      }

      if (!var2.isUpdateConstraint()) {
         var5.setValue("updateconstraint", "false");
      }

   }

   private void writeScript(String var1, ArrayList<String> var2) {
      String var3 = ZomboidFileSystem.instance.getString(var1);
      File var4 = new File(var3);

      try {
         FileWriter var5 = new FileWriter(var4);

         try {
            BufferedWriter var6 = new BufferedWriter(var5);

            try {
               DebugLog.General.printf("writing %s\n", var1);
               Iterator var7 = var2.iterator();

               while(true) {
                  if (!var7.hasNext()) {
                     this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("wroteScript"), new Object[]{this.m_table, var3});
                     break;
                  }

                  String var8 = (String)var7.next();
                  var6.write(var8);
               }
            } catch (Throwable var11) {
               try {
                  var6.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            var6.close();
         } catch (Throwable var12) {
            try {
               var5.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         var5.close();
      } catch (Throwable var13) {
         ExceptionLogger.logException(var13);
      }

   }

   private void updateModelScripts(VehicleScript var1) {
      for(int var2 = 0; var2 < var1.getPartCount(); ++var2) {
         VehicleScript.Part var3 = var1.getPart(var2);

         for(int var4 = 0; var4 < var3.getModelCount(); ++var4) {
            VehicleScript.Model var5 = var3.getModel(var4);
            if (var5.getFile() != null) {
               ModelScript var6 = ScriptManager.instance.getModelScript(var5.getFile());
               if (var6 != null && var6.getAttachmentCount() != 0) {
                  ArrayList var7 = AttachmentEditorState.readScript(var6.getFileName());
                  if (var7 != null) {
                     String var8 = var6.getFileName();
                     if (AttachmentEditorState.updateScript(var8, var7, var6)) {
                        String var9 = ZomboidFileSystem.instance.getString(var8);
                        this.m_luaEnv.caller.pcall(this.m_luaEnv.thread, this.m_table.rawget("wroteScript"), new Object[]{this.m_table, var9});
                     }
                  }
               }
            }
         }
      }

   }

   public static final class LuaEnvironment {
      public J2SEPlatform platform;
      public KahluaTable env;
      public KahluaThread thread;
      public LuaCaller caller;

      public LuaEnvironment(J2SEPlatform var1, KahluaConverterManager var2, KahluaTable var3) {
         this.platform = var1;
         this.env = var3;
         this.thread = LuaManager.thread;
         this.caller = LuaManager.caller;
      }
   }
}
