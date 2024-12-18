package zombie.gameStates;

import java.util.HashMap;
import java.util.function.Consumer;
import org.lwjglx.input.Keyboard;
import zombie.GameWindow;
import zombie.SoundManager;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.modding.ActiveMods;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.ui.LoadingQueueUI;
import zombie.ui.UIManager;

public class LoadingQueueState extends GameState {
   private static boolean bCancel = false;
   private static boolean bDone = false;
   private static int placeInQueue = -1;
   private boolean bAButtonDown = false;
   private static final LoadingQueueUI ui = new LoadingQueueUI();

   public LoadingQueueState() {
   }

   public void enter() {
      bCancel = false;
      bDone = false;
      placeInQueue = -1;
      this.bAButtonDown = GameWindow.ActivatedJoyPad != null && GameWindow.ActivatedJoyPad.isAPressed();
      SoundManager.instance.setMusicState("Loading");
      if (GameClient.bClient) {
         GameClient.instance.sendLoginQueueRequest();
      }

   }

   public GameState redirectState() {
      return (GameState)(bCancel ? new MainScreenState() : new GameLoadingState());
   }

   public void render() {
      Core.getInstance().StartFrame();
      Core.getInstance().EndFrame();
      boolean var1 = UIManager.useUIFBO;
      UIManager.useUIFBO = false;
      Core.getInstance().StartFrameUI();
      SpriteRenderer.instance.renderi((Texture)null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0F, 0.0F, 0.0F, 1.0F, (Consumer)null);
      if (placeInQueue >= 0) {
         MainScreenState.instance.renderBackground();
         UIManager.render();
         ActiveMods.renderUI();
         ui.render();
      }

      Core.getInstance().EndFrameUI();
      UIManager.useUIFBO = var1;
   }

   public GameStateMachine.StateAction update() {
      if (!GameClient.bClient) {
         return GameStateMachine.StateAction.Continue;
      } else {
         boolean var1 = GameWindow.ActivatedJoyPad != null && GameWindow.ActivatedJoyPad.isAPressed();
         if (var1) {
            if (this.bAButtonDown) {
               var1 = false;
            }
         } else {
            this.bAButtonDown = false;
         }

         if (!var1 && !Keyboard.isKeyDown(1) && GameClient.instance.bConnected) {
            return bDone ? GameStateMachine.StateAction.Continue : GameStateMachine.StateAction.Remain;
         } else {
            bCancel = true;
            SoundManager.instance.setMusicState("MainMenu");
            if (GameClient.connection != null) {
               GameClient.instance.bConnected = false;
               GameClient.bClient = false;
               GameClient.connection.forceDisconnect("loading-queue-canceled");
               GameClient.connection = null;
               ConnectionManager.getInstance().process();
            }

            return GameStateMachine.StateAction.Continue;
         }
      }
   }

   public static void onConnectionImmediate() {
      bDone = true;
   }

   public static void onPlaceInQueue(int var0, HashMap<String, Object> var1) {
      placeInQueue = var0;
      ui.setPlaceInQueue(var0);
      ui.setServerInformation(var1);
   }
}
