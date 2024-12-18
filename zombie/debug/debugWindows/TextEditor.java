package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.ImGuiIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;

public class TextEditor extends BaseDebugWindow {
   private File file;
   private boolean bDirty;
   private boolean bJustOpened;
   private final imgui.extension.texteditor.TextEditor textEditor = new imgui.extension.texteditor.TextEditor();
   private boolean isDebounceFKeys;

   public TextEditor() {
      this.textEditor.setShowWhitespaces(false);
   }

   public String getTitle() {
      return this.file.getAbsolutePath().substring(this.file.getAbsolutePath().indexOf("media") + 6);
   }

   protected void doKeyInput(ImGuiIO var1, boolean var2, boolean var3, boolean var4) {
      if (var1.getKeysDown(83) && var3) {
         try {
            this.save();
         } catch (IOException var10) {
            throw new RuntimeException(var10);
         }
      }

      if (var1.getKeysDown(299) && !this.isDebounceFKeys) {
         this.textEditor.setExecutingLine(-1);
         this.stepOver();
         this.isDebounceFKeys = true;
      } else if (var1.getKeysDown(300) && !this.isDebounceFKeys) {
         this.textEditor.setExecutingLine(-1);
         this.stepInto();
         this.isDebounceFKeys = true;
      } else if (var1.getKeysDown(294) && !this.isDebounceFKeys) {
         this.textEditor.setExecutingLine(-1);
         this.cont();
         this.isDebounceFKeys = true;
      } else if (var1.getKeysDown(298) && !this.isDebounceFKeys) {
         int var5 = this.textEditor.getBreakpointCount();
         ArrayList var6 = new ArrayList();

         int var8;
         for(int var7 = 0; var7 < var5; ++var7) {
            var8 = this.textEditor.getBreakpoint(var7);
            var6.add(var8);
         }

         int[] var11 = new int[var6.size()];

         for(var8 = 0; var8 < var6.size(); ++var8) {
            Integer var9 = (Integer)var6.get(var8);
            var11[var8] = var9;
         }

         this.setBreakpoints(var6);
         this.isDebounceFKeys = true;
      }

      if (!var1.getKeysDown(299) && !var1.getKeysDown(300) && !var1.getKeysDown(298) && !var1.getKeysDown(294)) {
         this.isDebounceFKeys = false;
      } else {
         this.isDebounceFKeys = true;
      }

   }

   protected void setBreakpoints(ArrayList<Integer> var1) {
   }

   protected void cont() {
   }

   protected void stepInto() {
   }

   protected void stepOver() {
   }

   protected void save() throws IOException {
      BufferedWriter var1 = new BufferedWriter(new FileWriter(this.file));

      try {
         String var2 = this.textEditor.getText();
         var1.write(var2);
      } catch (Throwable var5) {
         try {
            var1.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      var1.close();
      this.bDirty = false;
   }

   protected boolean isWindowFocused() {
      return this.textEditor.isFocused();
   }

   protected void doWindowContents() {
      if (this.textEditor.isTextChanged() && !this.bJustOpened) {
         this.bDirty = true;
      }

      this.bJustOpened = false;
      this.textEditor.render("TextEditor");
      if (ImGui.beginPopupModal("File changed...")) {
         float var1 = ImGui.calcTextSize("Do you want to save file: " + this.getTitle() + "?").x;
         float var2 = ImGui.calcTextSize("Yes").x + ImGui.calcTextSize("No").x + ImGui.calcTextSize("Cancel").x;
         var2 += 30.0F;
         ImGui.setWindowSize(var1 + 64.0F, 150.0F);
         ImGui.setCursorPosX((ImGui.getWindowSize().x - var1) * 0.5F);
         ImGui.textUnformatted("Do you want to save file: " + this.getTitle() + "?");
         ImGui.newLine();
         ImGui.separator();
         ImGui.newLine();
         ImGui.setCursorPosX((ImGui.getWindowSize().x - var2) / 2.0F);
         if (ImGui.button("Yes")) {
            this.open.set(false);

            try {
               this.save();
            } catch (IOException var4) {
               throw new RuntimeException(var4);
            }

            ImGui.closeCurrentPopup();
            DebugContext.instance.closeTransient(this);
         }

         ImGui.sameLine();
         if (ImGui.button("No")) {
            this.open.set(false);
            ImGui.closeCurrentPopup();
            DebugContext.instance.closeTransient(this);
         }

         ImGui.sameLine();
         if (ImGui.button("Cancel")) {
            ImGui.closeCurrentPopup();
         }

         ImGui.endPopup();
      }

   }

   protected boolean hasMenu() {
      return true;
   }

   protected void doMenu() {
      if (ImGui.beginMenuBar()) {
         if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save")) {
            }

            if (ImGui.menuItem("Save as...")) {
            }

            ImGui.endMenu();
         }

         ImGui.endMenuBar();
      }

   }

   protected void onCloseWindow() {
      if (this.bDirty) {
         ImGui.openPopup("File changed...");
         this.open.set(true);
      } else {
         DebugContext.instance.closeTransient(this);
      }

   }

   public void load(File var1) throws IOException {
      this.file = var1;
      BufferedReader var2 = new BufferedReader(new FileReader(var1));

      try {
         StringBuilder var3 = new StringBuilder();
         String var4 = var2.readLine();

         while(true) {
            if (var4 == null) {
               String var5 = var3.toString();
               this.textEditor.setText(var5);
               break;
            }

            var3.append(var4);
            var3.append(System.lineSeparator());
            var4 = var2.readLine();
         }
      } catch (Throwable var7) {
         try {
            var2.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      var2.close();
      this.bDirty = false;
      this.bJustOpened = true;
      this.open.set(true);
   }

   public void setExecutingLine(int var1) {
      this.textEditor.setExecutingLine(var1);
      this.textEditor.setCursorPosition(var1 - 1, 0);
   }
}
