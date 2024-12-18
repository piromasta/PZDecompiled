package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.network.GameClient;

public final class FishingState extends State {
   private static final FishingState _instance = new FishingState();

   public FishingState() {
   }

   public static FishingState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setVariable("FishingFinished", false);
   }

   public void execute(IsoGameCharacter var1) {
      if (GameClient.bClient && var1 instanceof IsoPlayer && ((IsoPlayer)var1).isLocalPlayer()) {
         GameClient.sendEvent((IsoPlayer)var1, "EventFishing");
         if (!var1.getCharacterActions().isEmpty() && var1.getNetworkCharacterAI().getAction() != var1.getCharacterActions().get(0)) {
            var1.getNetworkCharacterAI().setAction((BaseAction)var1.getCharacterActions().get(0));
            GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), true);
         }
      }

      if (var1.isSitOnGround() && ((IsoPlayer)var1).pressedMovement(false)) {
         var1.StopAllActionQueue();
         var1.setVariable("forceGetUp", true);
      }

   }

   public void exit(IsoGameCharacter var1) {
      if (GameClient.bClient && var1 instanceof IsoPlayer && var1.isLocal()) {
         if (var1.getNetworkCharacterAI().getAction() != null) {
            GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), false);
            var1.getNetworkCharacterAI().setAction((BaseAction)null);
         }

         ((IsoPlayer)var1).setFishingStage(FishingState.FishingStage.None.name());
      }

      var1.clearVariable("FishingStage");
   }

   public static enum FishingStage {
      None,
      Idle,
      Cast,
      Strike,
      StrikeMedium,
      StrikeHard,
      PickUp,
      PickUpTrash;

      private FishingStage() {
      }
   }
}
