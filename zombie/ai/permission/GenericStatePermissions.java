package zombie.ai.permission;

import zombie.characters.IsoGameCharacter;

public class GenericStatePermissions implements IStatePermissions {
   private boolean m_deferredMovement = false;
   private boolean m_playerInput = false;

   public GenericStatePermissions() {
   }

   public void setDeferredMovementAllowed(boolean var1) {
      this.m_deferredMovement = var1;
   }

   public boolean isDeferredMovementAllowed(IsoGameCharacter var1) {
      return this.m_deferredMovement;
   }

   public void setPlayerInputAllowed(boolean var1) {
      this.m_playerInput = var1;
   }

   public boolean isPlayerInputAllowed(IsoGameCharacter var1) {
      return this.m_playerInput;
   }
}
