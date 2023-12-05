package zombie.ai.permission;

import zombie.characters.IsoGameCharacter;

public interface IStatePermissions {
   boolean isDeferredMovementAllowed(IsoGameCharacter var1);

   boolean isPlayerInputAllowed(IsoGameCharacter var1);
}
