package zombie.pathfind.nativeCode;

import java.util.ArrayDeque;

final class RequestQueue {
   final ArrayDeque<PathFindRequest> playerQ = new ArrayDeque();
   final ArrayDeque<PathFindRequest> aggroZombieQ = new ArrayDeque();
   final ArrayDeque<PathFindRequest> otherQ = new ArrayDeque();

   RequestQueue() {
   }

   boolean isEmpty() {
      return this.playerQ.isEmpty() && this.aggroZombieQ.isEmpty() && this.otherQ.isEmpty();
   }

   PathFindRequest removeFirst() {
      if (!this.playerQ.isEmpty()) {
         return (PathFindRequest)this.playerQ.removeFirst();
      } else {
         return !this.aggroZombieQ.isEmpty() ? (PathFindRequest)this.aggroZombieQ.removeFirst() : (PathFindRequest)this.otherQ.removeFirst();
      }
   }

   PathFindRequest removeLast() {
      if (!this.otherQ.isEmpty()) {
         return (PathFindRequest)this.otherQ.removeLast();
      } else {
         return !this.aggroZombieQ.isEmpty() ? (PathFindRequest)this.aggroZombieQ.removeLast() : (PathFindRequest)this.playerQ.removeLast();
      }
   }
}
