package zombie.chat;

import java.util.ArrayList;
import zombie.chat.defaultChats.WhisperChat;

public final class WhisperChatCreation {
   String destPlayerName = null;
   WhisperChat.ChatStatus status;
   long createTime;
   final ArrayList<String> messages;

   public WhisperChatCreation() {
      this.status = WhisperChat.ChatStatus.None;
      this.createTime = 0L;
      this.messages = new ArrayList();
   }
}
