package zombie.ai;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.action.ActionGroup;
import zombie.core.network.ByteBufferWriter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.network.GameClient;
import zombie.util.Lambda;
import zombie.util.list.PZArrayUtil;

public final class StateMachine {
   private boolean m_isLocked = false;
   public int activeStateChanged = 0;
   private State m_currentState;
   private State m_previousState;
   private final IsoGameCharacter m_owner;
   private final List<SubstateSlot> m_subStates = new ArrayList();
   private static final byte VAL_TYPE_NO_SERIALIZATION = -1;
   private static final byte VAL_TYPE_STRING = 0;
   private static final byte VAL_TYPE_FLOAT = 1;
   private static final byte VAL_TYPE_DOUBLE = 2;
   private static final byte VAL_TYPE_BOOLEAN = 3;
   private static final byte VAL_TYPE_LONG = 4;
   private static final byte VAL_TYPE_INTEGER = 5;
   private static final byte VAL_TYPE_SHORT = 6;
   private static final byte VAL_TYPE_BYTE = 7;
   private static final byte VAL_TYPE_DIRECTION = 8;
   private static final byte VAL_TYPE_PREV_STATE = 9;

   public StateMachine(IsoGameCharacter var1) {
      this.m_owner = var1;
   }

   public void changeState(State var1, Iterable<State> var2) {
      this.changeState(var1, var2, false);
   }

   public void changeState(State var1, Iterable<State> var2, boolean var3) {
      if (!this.m_isLocked) {
         this.changeRootState(var1, var3);
         PZArrayUtil.forEach(this.m_subStates, (var0) -> {
            var0.shouldBeActive = false;
         });
         PZArrayUtil.forEach(var2, Lambda.consumer(this, (var0, var1x) -> {
            if (var0 != null) {
               var1x.ensureSubstateActive(var0);
            }

         }));
         Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_subStates, this, (var0, var1x) -> {
            if (!var0.shouldBeActive && !var0.isEmpty()) {
               var1x.removeSubstate(var0);
            }

         });
      }
   }

   private void changeRootState(State var1, boolean var2) {
      if (this.m_currentState == var1) {
         if (var2) {
            this.stateEnter(this.m_currentState);
         }

      } else {
         State var3 = this.m_currentState;
         if (var3 != null) {
            this.stateExit(var3);
         }

         this.m_previousState = var3;
         this.m_currentState = var1;
         if (var1 != null) {
            this.stateEnter(var1);
         }

         LuaEventManager.triggerEvent("OnAIStateChange", this.m_owner, this.m_currentState, this.m_previousState);
      }
   }

   private void ensureSubstateActive(State var1) {
      SubstateSlot var2 = this.getExistingSlot(var1);
      if (var2 != null) {
         var2.shouldBeActive = true;
      } else {
         SubstateSlot var3 = (SubstateSlot)PZArrayUtil.find(this.m_subStates, SubstateSlot::isEmpty);
         if (var3 != null) {
            var3.setState(var1);
            var3.shouldBeActive = true;
         } else {
            SubstateSlot var4 = new SubstateSlot(var1);
            this.m_subStates.add(var4);
         }

         this.stateEnter(var1);
      }
   }

   private SubstateSlot getExistingSlot(State var1) {
      return (SubstateSlot)PZArrayUtil.find(this.m_subStates, Lambda.predicate(var1, (var0, var1x) -> {
         return var0.getState() == var1x;
      }));
   }

   private void removeSubstate(State var1) {
      SubstateSlot var2 = this.getExistingSlot(var1);
      if (var2 != null) {
         this.removeSubstate(var2);
      }
   }

   private void removeSubstate(SubstateSlot var1) {
      State var2 = var1.getState();
      var1.setState((State)null);
      if (var2 != this.m_currentState || var2 != SwipeStatePlayer.instance()) {
         this.stateExit(var2);
      }
   }

   public boolean isSubstate(State var1) {
      return PZArrayUtil.contains(this.m_subStates, Lambda.predicate(var1, (var0, var1x) -> {
         return var0.getState() == var1x;
      }));
   }

   public State getCurrent() {
      return this.m_currentState;
   }

   public State getPrevious() {
      return this.m_previousState;
   }

   public int getSubStateCount() {
      return this.m_subStates.size();
   }

   public State getSubStateAt(int var1) {
      return ((SubstateSlot)this.m_subStates.get(var1)).getState();
   }

   public void revertToPreviousState(State var1) {
      if (this.isSubstate(var1)) {
         this.removeSubstate(var1);
      } else if (this.m_currentState != var1) {
         DebugLog.ActionSystem.warn("The sender $s is not an active state in this state machine.", String.valueOf(var1));
      } else {
         this.changeRootState(this.m_previousState, false);
      }
   }

   public void update() {
      if (this.m_currentState != null) {
         this.m_currentState.execute(this.m_owner);
      }

      Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_subStates, this.m_owner, (var0, var1) -> {
         if (!var0.isEmpty()) {
            var0.state.execute(var1);
         }

      });
      this.logCurrentState();
   }

   private void logCurrentState() {
      if (this.m_owner.isAnimationRecorderActive()) {
         this.m_owner.getAnimationPlayerRecorder().logAIState(this.m_currentState, this.m_subStates);
      }

   }

   private void stateEnter(State var1) {
      var1.enter(this.m_owner);
   }

   private void stateExit(State var1) {
      var1.exit(this.m_owner);
   }

   public final void stateAnimEvent(int var1, AnimEvent var2) {
      if (var1 == 0) {
         if (this.m_currentState != null) {
            this.m_currentState.animEvent(this.m_owner, var2);
         }

      } else {
         Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_subStates, this.m_owner, var2, (var0, var1x, var2x) -> {
            if (!var0.isEmpty()) {
               var0.state.animEvent(var1x, var2x);
            }

         });
      }
   }

   public boolean isLocked() {
      return this.m_isLocked;
   }

   public void setLocked(boolean var1) {
      this.m_isLocked = var1;
   }

   public void parse(ByteBuffer var1, int var2) {
      ActionGroup var3 = this.m_owner.getActionContext().getGroup();
      String var4 = var3.findStateName(var2);
      State var5 = this.m_owner.tryGetAIState(var3.findStateName(var2));
      if (var5 == null) {
         var5 = this.m_owner.getDefaultState();
      }

      HashMap var6 = this.m_owner.getStateMachineParams(var5);
      byte var7 = var1.get();

      for(byte var8 = 0; var8 < var7; ++var8) {
         Integer var9 = Integer.valueOf(var1.get());
         byte var10 = var1.get();
         switch (var10) {
            case -1:
               DebugLog.Multiplayer.warn("Cannot parse state machine param for state %s", var4);
               break;
            case 0:
               var6.put(var9, GameWindow.ReadString(var1));
               break;
            case 1:
               var6.put(var9, var1.getFloat());
               break;
            case 2:
               var6.put(var9, var1.getDouble());
               break;
            case 3:
               var6.put(var9, var1.get() != 0);
               break;
            case 4:
               var6.put(var9, var1.getLong());
               break;
            case 5:
               var6.put(var9, var1.getInt());
               break;
            case 6:
               var6.put(var9, var1.getShort());
               break;
            case 7:
               var6.put(var9, var1.get());
               break;
            case 8:
               var6.put(var9, IsoDirections.fromIndex(var1.get()));
               break;
            case 9:
               int var11 = var1.getInt();
               String var12 = this.m_owner.getActionContext().getGroup().findStateName(var11);
               if (var12 != null) {
                  this.m_previousState = this.m_owner.tryGetAIState(var12);
                  if (this.m_previousState == null) {
                     this.m_previousState = this.m_owner.getDefaultState();
                  }
               } else {
                  this.m_previousState = this.m_owner.getDefaultState();
               }

               var6.put(var9, this.m_previousState);
               break;
            default:
               DebugLog.Multiplayer.warn("Unknown value type %d state machine param for state %s", var10, var4);
         }
      }

   }

   public void write(ByteBufferWriter var1, int var2) {
      HashMap var3;
      if (GameClient.bClient) {
         var3 = this.m_owner.getStateMachineParams(this.m_currentState);
      } else {
         ActionGroup var4 = this.m_owner.getActionContext().getGroup();
         State var5 = this.m_owner.tryGetAIState(var4.findStateName(var2));
         if (var5 == null) {
            var5 = this.m_owner.getDefaultState();
         }

         var3 = this.m_owner.getStateMachineParams(var5);
      }

      var1.putByte((byte)var3.size());
      Iterator var8 = var3.entrySet().iterator();

      while(var8.hasNext()) {
         Map.Entry var9 = (Map.Entry)var8.next();
         Integer var6 = (Integer)var9.getKey();
         var1.putByte((byte)var6);
         Object var7 = var9.getValue();
         if (var7 instanceof String) {
            var1.putByte((byte)0);
            GameWindow.WriteString(var1.bb, (String)var7);
         } else if (var7 instanceof Float) {
            var1.putByte((byte)1);
            var1.putFloat((Float)var7);
         } else if (var7 instanceof Double) {
            var1.putByte((byte)2);
            var1.putDouble((Double)var7);
         } else if (var7 instanceof Boolean) {
            var1.putByte((byte)3);
            var1.putByte((byte)(var7 == Boolean.TRUE ? 1 : 0));
         } else if (var7 instanceof Long) {
            var1.putByte((byte)4);
            var1.putLong((Long)var7);
         } else if (var7 instanceof Integer) {
            var1.putByte((byte)5);
            var1.putInt((Integer)var7);
         } else if (var7 instanceof Short) {
            var1.putByte((byte)6);
            var1.putShort((Short)var7);
         } else if (var7 instanceof Byte) {
            var1.putByte((byte)7);
            var1.putByte((Byte)var7);
         } else if (var7 instanceof IsoDirections) {
            var1.putByte((byte)8);
            var1.putByte((byte)((IsoDirections)var7).index());
         } else if (var7 instanceof State) {
            var1.putByte((byte)9);
            var1.putInt(this.m_owner.getPreviousActionContextStateName().hashCode());
         } else {
            var1.putByte((byte)-1);
         }
      }

   }

   public static class SubstateSlot {
      private State state;
      boolean shouldBeActive;

      SubstateSlot(State var1) {
         this.state = var1;
         this.shouldBeActive = true;
      }

      public State getState() {
         return this.state;
      }

      void setState(State var1) {
         this.state = var1;
      }

      public boolean isEmpty() {
         return this.state == null;
      }
   }
}
