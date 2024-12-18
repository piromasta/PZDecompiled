package zombie.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import org.lwjglx.opengl.Display;
import zombie.core.Core;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.debugWindows.AimPlotter;
import zombie.debug.debugWindows.BallisticsTargetPanel;
import zombie.debug.debugWindows.Console;
import zombie.debug.debugWindows.FirearmPanel;
import zombie.debug.debugWindows.JavaInspector;
import zombie.debug.debugWindows.LuaPanel;
import zombie.debug.debugWindows.ScenePanel;
import zombie.debug.debugWindows.TargetHitInfoPanel;
import zombie.debug.debugWindows.TracerEffectsDebugWindow;
import zombie.debug.debugWindows.UIPanel;
import zombie.debug.debugWindows.Viewport;

public class DebugContext {
   public static float FLT_MIN = 1.17549435E-38F;
   public static DebugContext instance = new DebugContext();
   public int dockspace_id;
   public TextureFBO DebugViewportTexture;
   public boolean bFocusedGameViewport;
   private int dockspace;
   private float viewportMouseX;
   private float viewportMouseY;
   private float lastViewportMouseX;
   private float lastViewportMouseY;
   public Viewport Viewport;
   private final ArrayList<BaseDebugWindow> Windows = new ArrayList();
   private final ArrayList<BaseDebugWindow> TransientWindows = new ArrayList();

   public DebugContext() {
   }

   public static boolean isUsingGameViewportWindow() {
      return Core.isUseGameViewport();
   }

   public void initRenderTarget() {
      if (Core.isUseGameViewport()) {
         RenderThread.invokeOnRenderContext(() -> {
            Texture var1 = new Texture(Core.width, Core.height, 18);
            this.DebugViewportTexture = new TextureFBO(var1);
         });
      }

   }

   public void init() {
      if (Core.isImGui()) {
         this.initRenderTarget();
         this.Windows.add(new LuaPanel());
         this.Windows.add(new UIPanel());
         this.Windows.add(new ScenePanel());
         this.Windows.add(new FirearmPanel());
         this.Windows.add(new AimPlotter());
         this.Windows.add(new TracerEffectsDebugWindow());
         this.Windows.add(new BallisticsTargetPanel());
         this.Windows.add(new TargetHitInfoPanel());
         if (Core.isUseGameViewport()) {
            this.Viewport = new Viewport();
            this.Windows.add(this.Viewport);
         }

         this.Windows.add(new Console());
      }
   }

   public void destroy() {
      if (Core.isImGui()) {
         if (Core.isUseGameViewport()) {
            this.DebugViewportTexture.destroy();
            this.DebugViewportTexture = null;
         }

      }
   }

   public void tick() {
      if (Core.isImGui()) {
         if (Display.inImGuiFrame()) {
            this.doMainMenu();

            int var1;
            BaseDebugWindow var2;
            for(var1 = 0; var1 < this.Windows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.Windows.get(var1);
               if (var2.open.get()) {
                  var2.endFrameTick();
               }
            }

            for(var1 = 0; var1 < this.TransientWindows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.TransientWindows.get(var1);
               if (var2.open.get()) {
                  var2.endFrameTick();
               }
            }

         }
      }
   }

   private void doMainMenu() {
      if (ImGui.beginMainMenuBar()) {
         if (ImGui.beginMenu("Windows")) {
            int var1;
            BaseDebugWindow var2;
            for(var1 = 0; var1 < this.Windows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.Windows.get(var1);
               if (ImGui.menuItem(var2.getTitle(), (String)null, var2.open.get())) {
                  var2.open = new ImBoolean(!var2.open.get());
               }
            }

            ImGui.separator();

            for(var1 = 0; var1 < this.TransientWindows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.TransientWindows.get(var1);
               if (ImGui.menuItem(var2.getTitle(), (String)null, var2.open.get())) {
                  var2.open = new ImBoolean(!var2.open.get());
               }
            }

            ImGui.endMenu();
         }

         ImGui.endMainMenuBar();
      }

   }

   public void startDrawing() {
      this.DebugViewportTexture.startDrawing(true, false);
   }

   public void endDrawing() {
      this.DebugViewportTexture.endDrawing();
   }

   public void tickFrameStart() {
      if (Core.isImGui()) {
         if (Display.inImGuiFrame()) {
            if (!Core.isUseGameViewport()) {
               this.dockspace = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), 8);
            } else {
               this.dockspace = ImGui.dockSpaceOverViewport(ImGui.getMainViewport());
            }

            int var1;
            BaseDebugWindow var2;
            for(var1 = 0; var1 < this.Windows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.Windows.get(var1);
               if (var2.open.get()) {
                  var2.startFrameTick();
               }
            }

            for(var1 = 0; var1 < this.TransientWindows.size(); ++var1) {
               var2 = (BaseDebugWindow)this.TransientWindows.get(var1);
               if (var2.open.get()) {
                  var2.startFrameTick();
               }
            }

         }
      }
   }

   public int getViewportMouseX() {
      return (int)this.viewportMouseX;
   }

   public int getViewportMouseY() {
      return Core.height - (int)this.viewportMouseY;
   }

   public void setViewportX(float var1) {
      this.viewportMouseX = var1;
   }

   public void setViewportY(float var1) {
      this.viewportMouseY = var1;
   }

   public BaseDebugWindow getExistingTransientWindow(BaseDebugWindow var1) {
      for(int var2 = 0; var2 < this.TransientWindows.size(); ++var2) {
         BaseDebugWindow var3 = (BaseDebugWindow)this.TransientWindows.get(var2);
         if (var3.getTitle().equals(var1.getTitle())) {
            return var3;
         }
      }

      return null;
   }

   public void inspectJava(Object var1) {
      if (var1 != null) {
         JavaInspector var2 = new JavaInspector(var1);
         BaseDebugWindow var3 = this.getExistingTransientWindow(var2);
         if (var3 == null) {
            this.TransientWindows.add(var2);
         } else {
            var3.open = new ImBoolean(true);
         }

      }
   }

   public ArrayList<BaseDebugWindow> getTransientWindows() {
      return this.TransientWindows;
   }

   public void closeTransient(BaseDebugWindow var1) {
      this.TransientWindows.remove(var1);
   }

   public ArrayList<BaseDebugWindow> getWindows() {
      return this.Windows;
   }
}
