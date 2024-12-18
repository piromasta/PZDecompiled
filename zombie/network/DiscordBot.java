package zombie.network;

import java.util.Set;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public class DiscordBot {
   private final DiscordSender sender;
   private final String serverName;
   private TextChannel channel;
   private DiscordApi api;

   public DiscordBot(String var1, DiscordSender var2) {
      this.serverName = var1;
      this.sender = var2;
   }

   public void connect(boolean var1, String var2, String var3, String var4) {
      if (var1) {
         DebugLog.Discord.debugln("enabled");
         if (!StringUtils.isNullOrEmpty(var2)) {
            this.api = (DiscordApi)(new DiscordApiBuilder()).setToken(var2).setAllIntents().login().join();
            if (this.api != null) {
               this.channel = this.getChannel(var4, var3);
               if (this.channel != null) {
                  DebugLog.Discord.debugln("initialization succeeded");
                  this.api.updateUsername(this.serverName);
                  this.api.addMessageCreateListener(this::receiveMessage);
                  DebugLog.Discord.println("invite-url %s", this.api.createBotInvite());
               } else {
                  DebugLog.Discord.warn("initialization failed");
               }
            }
         } else {
            DebugLog.Discord.warn("token not configured");
         }
      } else {
         DebugLog.Discord.debugln("disabled");
      }

   }

   public TextChannel getChannel(String var1, String var2) {
      TextChannel var3 = null;
      if (!StringUtils.isNullOrEmpty(var1)) {
         var3 = (TextChannel)this.api.getTextChannelById(var1).orElse((Object)null);
         if (var3 == null) {
            DebugLog.Discord.warn("channel with ID \"%s\" not found. Try to use channel name instead", var1);
         } else {
            DebugLog.Discord.debugln("enabled on channel with ID \"%s\"", var1);
         }
      } else if (!StringUtils.isNullOrEmpty(var2)) {
         Set var4 = this.api.getTextChannelsByName(var2);
         if (var4.size() == 0) {
            DebugLog.Discord.warn("channel with name \"%s\" not found. Try to use channel ID instead", var2);
         } else if (var4.size() == 1) {
            DebugLog.Discord.debugln("enabled on channel with name \"%s\"", var2);
            var3 = (TextChannel)var4.stream().findFirst().orElse((Object)null);
         } else {
            DebugLog.Discord.warn("server has few channels with name \"%s\". Please, use channel ID instead", var2);
         }
      } else {
         var3 = (TextChannel)this.api.getTextChannels().stream().findFirst().orElse((Object)null);
         if (var3 == null) {
            DebugLog.Discord.warn("channels not found");
         }
      }

      return var3;
   }

   public void sendMessage(String var1, String var2) {
      if (this.channel != null) {
         String var3 = var1 + ": " + var2;
         this.channel.sendMessage(var3);
         DebugLog.Discord.debugln("send message: \"%s\"", var3);
      }

   }

   public void receiveMessage(MessageCreateEvent var1) {
      TextChannel var2 = var1.getChannel();
      if (this.channel != null && var2 != null && this.channel.getId() == var2.getId()) {
         Message var3 = var1.getMessage();
         if (!var3.getAuthor().isYourself()) {
            String var4 = this.removeSmilesAndImages(var3.getReadableContent());
            if (!var4.isEmpty()) {
               this.sender.sendMessageFromDiscord(var3.getAuthor().getDisplayName(), var4);
            }

            DebugLog.Discord.debugln("get message \"%s\" from user \"%s\"", var4, var3.getAuthor().getDisplayName());
         }
      }

   }

   private String removeSmilesAndImages(String var1) {
      StringBuilder var2 = new StringBuilder();
      char[] var3 = var1.toCharArray();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Character var6 = var3[var5];
         if (!Character.isLowSurrogate(var6) && !Character.isHighSurrogate(var6)) {
            var2.append(var6);
         }
      }

      return var2.toString();
   }
}
