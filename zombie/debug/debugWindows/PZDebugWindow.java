package zombie.debug.debugWindows;

import zombie.SoundManager;
import zombie.debug.BaseDebugWindow;

public class PZDebugWindow extends BaseDebugWindow {
   public PZDebugWindow() {
   }

   public void doWindow() {
      super.doWindow();
   }

   protected void onWindowDocked() {
      if (!SoundManager.instance.isPlayingUISound("UIDocked")) {
         SoundManager.instance.playUISound("UIDocked");
      }

   }

   protected void onWindowUndocked() {
      if (!SoundManager.instance.isPlayingUISound("UIUndocked")) {
         SoundManager.instance.playUISound("UIUndocked");
      }

   }

   protected void onOpenWindow() {
      if (!SoundManager.instance.isPlayingUISound("UIDocked")) {
         SoundManager.instance.playUISound("UIDocked");
      }

   }

   protected void onCloseWindow() {
      if (!SoundManager.instance.isPlayingUISound("UIUndocked")) {
         SoundManager.instance.playUISound("UIUndocked");
      }

   }
}
