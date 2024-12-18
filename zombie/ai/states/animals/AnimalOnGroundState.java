package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public final class AnimalOnGroundState extends State {
   private static final AnimalOnGroundState _instance = new AnimalOnGroundState();

   public AnimalOnGroundState() {
   }

   public static AnimalOnGroundState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setCollidable(false);
      if (var1.isDead()) {
         var1.die();
      }

   }

   public void execute(IsoGameCharacter var1) {
      if (var1.isDead()) {
         var1.die();
      }

   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
   }
}
