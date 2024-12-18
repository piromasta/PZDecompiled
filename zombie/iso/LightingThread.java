package zombie.iso;

import org.lwjglx.opengl.Display;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.network.GameServer;
import zombie.ui.FPSGraph;

public final class LightingThread {
   public static final LightingThread instance = new LightingThread();
   public Thread lightingThread;
   public boolean bFinished = false;
   public volatile boolean Interrupted = false;
   public static boolean DebugLockTime = false;

   public LightingThread() {
   }

   public void stop() {
      if (!PerformanceSettings.LightingThread) {
         LightingJNI.stop();
      } else {
         this.bFinished = true;

         while(this.lightingThread.isAlive()) {
         }

         LightingJNI.stop();
         this.lightingThread = null;
      }
   }

   public void create() {
      if (!GameServer.bServer) {
         if (PerformanceSettings.LightingThread) {
            this.bFinished = false;
            this.lightingThread = new Thread(ThreadGroups.Workers, this::threadLoop);
            this.lightingThread.setPriority(5);
            this.lightingThread.setDaemon(true);
            this.lightingThread.setName("Lighting Thread");
            this.lightingThread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
            this.lightingThread.start();
         }
      }
   }

   private void threadLoop() {
      while(!this.bFinished) {
         if (IsoWorld.instance.CurrentCell == null) {
            return;
         }

         try {
            this.runInner();
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }
      }

   }

   private void runInner() throws Exception {
      Display.sync(PerformanceSettings.LightingFPS);
      LightingJNI.DoLightingUpdateNew(System.nanoTime(), Core.dirtyGlobalLightsCount > 0);
      if (Core.dirtyGlobalLightsCount > 3) {
         Core.dirtyGlobalLightsCount = 2;
      }

      if (Core.dirtyGlobalLightsCount > 0) {
         --Core.dirtyGlobalLightsCount;
      }

      while(LightingJNI.WaitingForMain() && !this.bFinished) {
         Thread.sleep(13L);
      }

      if (Core.bDebug && FPSGraph.instance != null) {
         FPSGraph.instance.addLighting(System.currentTimeMillis());
      }

   }

   public void GameLoadingUpdate() {
   }

   public void update() {
      if (IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null) {
         if (LightingJNI.init) {
            LightingJNI.update();
         }

      }
   }

   public void scrollLeft(int var1) {
      if (LightingJNI.init) {
         LightingJNI.scrollLeft(var1);
      }

   }

   public void scrollRight(int var1) {
      if (LightingJNI.init) {
         LightingJNI.scrollRight(var1);
      }

   }

   public void scrollUp(int var1) {
      if (LightingJNI.init) {
         LightingJNI.scrollUp(var1);
      }

   }

   public void scrollDown(int var1) {
      if (LightingJNI.init) {
         LightingJNI.scrollDown(var1);
      }

   }
}
