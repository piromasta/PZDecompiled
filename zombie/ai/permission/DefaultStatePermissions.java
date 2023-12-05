package zombie.ai.permission;

import zombie.characters.IsoGameCharacter;

public class DefaultStatePermissions implements IStatePermissions {
   public static final DefaultStatePermissions Instance = new DefaultStatePermissions();

   public DefaultStatePermissions() {
   }

   public boolean isDeferredMovementAllowed(IsoGameCharacter var1) {
      return true;
   }

   public boolean isPlayerInputAllowed(IsoGameCharacter var1) {
      return true;
   }
}
