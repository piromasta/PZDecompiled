package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.network.GameClient;
import zombie.popman.ZombiePopulationManager;

public final class ZombieSittingState extends State {
   private static final ZombieSittingState _instance = new ZombieSittingState();

   public ZombieSittingState() {
   }

   public static ZombieSittingState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      if (GameClient.bClient && var1.getCurrentSquare() != null) {
         ZombiePopulationManager.instance.sitAgainstWall(var2, var2.getCurrentSquare());
      }

   }

   public void exit(IsoGameCharacter var1) {
   }
}
