package zombie.gameStates;

import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.ui.UIManager;

public class TermsOfServiceState extends GameState {
   private boolean bExit = false;
   private boolean bCreated = false;

   public TermsOfServiceState() {
   }

   public void enter() {
      LuaEventManager.triggerEvent("OnGameStateEnter", this);
      if (!this.bCreated) {
         this.bExit = true;
      }

   }

   public void exit() {
      UIManager.clearArrays();
   }

   public GameStateMachine.StateAction update() {
      return this.bExit ? GameStateMachine.StateAction.Continue : GameStateMachine.StateAction.Remain;
   }

   public void render() {
      Core.getInstance().StartFrame();
      Core.getInstance().EndFrame();
      if (Core.getInstance().StartFrameUI()) {
         UIManager.render();
      }

      Core.getInstance().EndFrameUI();
   }

   public Object fromLua0(String var1) {
      switch (var1) {
         case "created":
            this.bCreated = true;
            return null;
         case "exit":
            this.bExit = true;
            return null;
         default:
            throw new IllegalArgumentException("unhandled \"" + var1 + "\"");
      }
   }
}
