package zombie.debug;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import zombie.debug.debugWindows.PZImGui;

public class BaseDebugWindow {
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected ImVec2 contentMin;
   protected ImBoolean open = new ImBoolean(true);
   protected boolean wasWindowOpened = true;
   protected boolean wasWindowDocked = false;

   public BaseDebugWindow() {
   }

   public String getTitle() {
      return "";
   }

   public void doWindow() {
      ImGui.setNextWindowSize(500.0F, 500.0F, 4);
      if (PZImGui.begin(this.getTitle(), this.open, this.getWindowFlags() | (this.hasMenu() ? 1024 : 0))) {
         this.x = ImGui.getWindowPosX();
         this.y = ImGui.getWindowPosY();
         this.width = ImGui.getWindowWidth();
         this.height = ImGui.getWindowHeight();
         this.contentMin = ImGui.getWindowContentRegionMin();
         this.doMenu();
         this.doWindowContents();
         this.doKeyInputInternal();
      }

      this.updateWindowState();
      this.updateWindowDockedState();
      ImGui.end();
   }

   protected void updateWindowDockedState() {
      boolean var1 = ImGui.isWindowDocked();
      if (var1 && !this.wasWindowDocked) {
         this.onWindowDocked();
         this.wasWindowDocked = true;
      } else if (!var1 && this.wasWindowDocked) {
         this.onWindowUndocked();
         this.wasWindowDocked = false;
      }

   }

   protected void updateWindowState() {
      boolean var1 = this.open.get();
      if (var1 && !this.wasWindowOpened) {
         this.onOpenWindow();
         this.wasWindowOpened = true;
      } else if (!var1 && this.wasWindowOpened) {
         this.onCloseWindow();
         this.wasWindowOpened = false;
      }

   }

   protected void onWindowDocked() {
   }

   protected void onWindowUndocked() {
   }

   protected void onCloseWindow() {
   }

   protected void onOpenWindow() {
   }

   protected void doMenu() {
   }

   protected boolean isWindowFocused() {
      return ImGui.isWindowFocused();
   }

   private void doKeyInputInternal() {
      if (this.isWindowFocused()) {
         ImGuiIO var1 = ImGui.getIO();
         this.doKeyInput(var1, var1.getKeyShift(), var1.getKeyCtrl(), var1.getKeyAlt());
      }
   }

   protected void doKeyInput(ImGuiIO var1, boolean var2, boolean var3, boolean var4) {
   }

   protected boolean hasMenu() {
      return false;
   }

   public int getWindowFlags() {
      return 0;
   }

   protected void doWindowContents() {
   }

   public boolean doFrameStartTick() {
      return false;
   }

   public void startFrameTick() {
      if (this.doFrameStartTick()) {
         this.doWindow();
      }

   }

   public void endFrameTick() {
      if (!this.doFrameStartTick()) {
         this.doWindow();
      }

   }
}
