package zombie.ai.states;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.network.GameServer;

public final class PlayerOnGroundState extends State {
   private static final PlayerOnGroundState _instance = new PlayerOnGroundState();

   public PlayerOnGroundState() {
   }

   public static PlayerOnGroundState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setIgnoreMovement(true);
      ((IsoPlayer)var1).setBlockMovement(true);
      var1.setVariable("bAnimEnd", false);
   }

   public void execute(IsoGameCharacter var1) {
      if (!GameServer.bServer && var1.isDead()) {
         var1.die();
      } else {
         var1.setReanimateTimer(var1.getReanimateTimer() - GameTime.getInstance().getMultiplier() / 1.6F);
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.setIgnoreMovement(false);
      ((IsoPlayer)var1).setBlockMovement(false);
   }
}
