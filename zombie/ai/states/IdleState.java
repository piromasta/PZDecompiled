package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class IdleState extends State {
   private static final IdleState _instance = new IdleState();

   public IdleState() {
   }

   public static IdleState instance() {
      return _instance;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2.m_EventName.equalsIgnoreCase("PlaySound") && !StringUtils.isNullOrEmpty(var2.m_ParameterValue)) {
         var1.getSquare().playSound(var2.m_ParameterValue);
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound") && var3 != null && var3.getVariableBoolean("dbgForceAnim")) {
         var3.stopPlayerVoiceSound(var2.m_ParameterValue);
         var3.playerVoiceSound(var2.m_ParameterValue);
      }

   }
}
