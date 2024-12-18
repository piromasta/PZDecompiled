package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class PlayerActionsState extends State {
   private static final PlayerActionsState _instance = new PlayerActionsState();

   public PlayerActionsState() {
   }

   public static PlayerActionsState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      InventoryItem var2 = var1.getPrimaryHandItem();
      InventoryItem var3 = var1.getSecondaryHandItem();
      if (!(var2 instanceof HandWeapon) && !(var3 instanceof HandWeapon)) {
         var1.setHideWeaponModel(true);
      }

      String var4 = var1.getVariableString("PerformingAction");
      if (GameClient.bClient && var1 instanceof IsoPlayer && var1.isLocal() && !var1.getCharacterActions().isEmpty() && var1.getNetworkCharacterAI().getAction() == null) {
         var1.getNetworkCharacterAI().setAction((BaseAction)var1.getCharacterActions().get(0));
         GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), true);
         var1.getNetworkCharacterAI().setPerformingAction(var4);
      }

   }

   public void execute(IsoGameCharacter var1) {
      if (GameClient.bClient && var1 instanceof IsoPlayer && var1.isLocal()) {
         String var2 = var1.getVariableString("PerformingAction");
         if (!var1.getCharacterActions().isEmpty() && (var1.getNetworkCharacterAI().getAction() != var1.getCharacterActions().get(0) || var2 != null && !var2.equals(var1.getNetworkCharacterAI().getPerformingAction()))) {
            GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), false);
            var1.getNetworkCharacterAI().setAction((BaseAction)var1.getCharacterActions().get(0));
            GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), true);
            var1.getNetworkCharacterAI().setPerformingAction(var2);
         }
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.setHideWeaponModel(false);
      var1.clearVariable("PlayerVoiceSound");
      if (GameClient.bClient && var1 instanceof IsoPlayer && var1.isLocal() && var1.getNetworkCharacterAI().getAction() != null) {
         GameClient.sendAction(var1.getNetworkCharacterAI().getAction(), false);
         var1.getNetworkCharacterAI().setAction((BaseAction)null);
      }

   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      var1.getStateMachineParams(this);
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound")) {
         if (var1.getVariableBoolean("PlayerVoiceSound")) {
            return;
         }

         if (var4 == null) {
            return;
         }

         var1.setVariable("PlayerVoiceSound", true);
         var4.stopPlayerVoiceSound(var2.m_ParameterValue);
         var4.playerVoiceSound(var2.m_ParameterValue);
      }

      if (GameClient.bClient && var2 != null && var1 instanceof IsoPlayer && var1.getNetworkCharacterAI().getAction() != null && !var1.isLocal()) {
         if ("changeWeaponSprite".equalsIgnoreCase(var2.m_EventName) && !StringUtils.isNullOrEmpty(var2.m_ParameterValue)) {
            if ("original".equals(var2.m_ParameterValue)) {
               var1.getNetworkCharacterAI().setOverride(false, (String)null, (String)null);
            } else {
               var1.getNetworkCharacterAI().setOverride(true, var2.m_ParameterValue, (String)null);
            }
         }

         if ("attachConnect".equalsIgnoreCase(var2.m_EventName)) {
            var1.setPrimaryHandItem((InventoryItem)null);
            var1.setSecondaryHandItem((InventoryItem)null);
         }
      }

   }
}
