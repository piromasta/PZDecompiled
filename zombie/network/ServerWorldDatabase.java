package zombie.network;

import de.taimos.totp.TOTP;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.ZomboidFileSystem;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkUser;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.Core;
import zombie.core.secure.PZcrypt;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.util.PZSQLUtils;

public class ServerWorldDatabase {
   public static final int AUTH_TYPE_USERNAME_PASSWORD = 1;
   public static final int AUTH_TYPE_GOOGLE_AUTH = 2;
   public static final int AUTH_TYPE_TWO_FACTOR = 3;
   private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   public static ServerWorldDatabase instance = new ServerWorldDatabase();
   public String CommandLineAdminUsername = "admin";
   public String CommandLineAdminPassword;
   public boolean doAdmin = true;
   public DBSchema dbSchema = null;
   static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
   Connection conn;
   private static final String nullChar = String.valueOf('\u0000');

   public ServerWorldDatabase() {
   }

   public DBSchema getDBSchema() {
      if (this.dbSchema == null) {
         this.dbSchema = new DBSchema(this.conn);
      }

      return this.dbSchema;
   }

   public void executeQuery(String var1, KahluaTable var2) throws SQLException {
      PreparedStatement var3 = this.conn.prepareStatement(var1);
      KahluaTableIterator var4 = var2.iterator();
      int var5 = 1;

      while(var4.advance()) {
         var3.setString(var5++, (String)var4.getValue());
      }

      var3.executeUpdate();
   }

   public ArrayList<DBResult> getTableResult(String var1) throws SQLException {
      ArrayList var2 = new ArrayList();
      String var3 = "SELECT * FROM " + var1;
      if ("userlog".equals(var1)) {
         var3 = var3 + " ORDER BY lastUpdate DESC";
      }

      PreparedStatement var4 = this.conn.prepareStatement(var3);
      ResultSet var5 = var4.executeQuery();
      DatabaseMetaData var6 = this.conn.getMetaData();
      ResultSet var7 = var6.getColumns((String)null, (String)null, var1, (String)null);
      ArrayList var8 = new ArrayList();
      DBResult var9 = new DBResult();

      while(var7.next()) {
         String var10 = var7.getString(4);
         if (!var10.equals("world") && !var10.equals("moderator") && !var10.equals("admin") && !var10.equals("password") && !var10.equals("encryptedPwd") && !var10.equals("pwdEncryptType")) {
            var8.add(var10);
         }
      }

      var9.setColumns(var8);
      var9.setTableName(var1);

      while(var5.next()) {
         for(int var13 = 0; var13 < var8.size(); ++var13) {
            String var11 = (String)var8.get(var13);
            String var12 = var5.getString(var11);
            if ("'false'".equals(var12)) {
               var12 = "false";
            }

            if ("'true'".equals(var12)) {
               var12 = "true";
            }

            if (var12 == null) {
               var12 = "";
            }

            var9.getValues().put(var11, var12);
         }

         var2.add(var9);
         var9 = new DBResult();
         var9.setColumns(var8);
         var9.setTableName(var1);
      }

      var4.close();
      return var2;
   }

   public void getWhitelistUsers(HashMap<String, NetworkUser> var1) {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("SELECT * FROM whitelist");
         ResultSet var3 = var2.executeQuery();

         while(var3.next()) {
            IsoPlayer var4 = GameServer.getPlayerByUserNameForCommand(var3.getString("username"));
            NetworkUser var5 = new NetworkUser(var3.getString("world"), var3.getString("username"), var3.getString("lastConnection"), var3.getString("role"), var3.getInt("authType"), var3.getString("steamid"), var3.getString("displayName"), var4 != null);
            var5.setInWhitelist(true);
            var1.put(var5.getUsername(), var5);
         }

         var2.close();
      } catch (SQLException var6) {
         var6.printStackTrace();
      }

   }

   public void getUserlogUsers(HashMap<String, NetworkUser> var1) {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("SELECT * FROM userlog");
         ResultSet var3 = var2.executeQuery();

         while(var3.next()) {
            String var4 = var3.getString("username");
            if (!var1.containsKey(var4)) {
               NetworkUser var5 = new NetworkUser(GameServer.ServerName, var4, "", Roles.getDefaultForUser().getName(), 1, "", "", false);
               var1.put(var4, var5);
            }
         }

         var2.close();
      } catch (SQLException var6) {
         var6.printStackTrace();
      }

   }

   private int getUserCounter(String var1, String var2) {
      int var3 = 0;

      try {
         PreparedStatement var4 = this.conn.prepareStatement("SELECT SUM(amount) FROM userlog WHERE username = ? AND type = ?");
         var4.setString(1, var1);
         var4.setString(2, var2);

         for(ResultSet var5 = var4.executeQuery(); var5.next(); var3 = var5.getInt(1)) {
         }

         var4.close();
      } catch (SQLException var6) {
         DebugLog.General.printException(var6, "DataBase counter " + var2 + " read failed", LogSeverity.Error);
      }

      return var3;
   }

   public void updateUserCounters(Collection<NetworkUser> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         NetworkUser var3 = (NetworkUser)var2.next();
         int var4 = this.getUserCounter(var3.getUsername(), Userlog.UserlogType.WarningPoint.name());
         var3.setWarningPoints(var4);
         int var5 = this.getUserCounter(var3.getUsername(), Userlog.UserlogType.SuspiciousActivity.name());
         var3.setSuspicionPoints(var5);
         int var6 = this.getUserCounter(var3.getUsername(), Userlog.UserlogType.Kicked.name());
         var3.setKicks(var6);
      }

   }

   public boolean containsUser(String var1, String var2) {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
         var3.setString(1, var1);
         var3.setString(2, var2);
         ResultSet var4 = var3.executeQuery();
         if (var4.next()) {
            var3.close();
            return true;
         }

         var3.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

      return false;
   }

   public boolean containsUser(String var1) {
      return this.containsUser(var1, Core.GameSaveWorld);
   }

   public boolean containsCaseinsensitiveUser(String var1) {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
         var2.setString(1, var1);
         var2.setString(2, Core.GameSaveWorld);
         ResultSet var3 = var2.executeQuery();
         if (var3.next()) {
            var2.close();
            return true;
         }

         var2.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

      return false;
   }

   public String changeUsername(String var1, String var2) throws SQLException {
      PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
      var3.setString(1, var1);
      var3.setString(2, Core.GameSaveWorld);
      ResultSet var4 = var3.executeQuery();
      if (var4.next()) {
         String var5 = var4.getString("id");
         var3.close();
         var3 = this.conn.prepareStatement("UPDATE whitelist SET username = ? WHERE id = ?");
         var3.setString(1, var2);
         var3.setString(2, var5);
         var3.executeUpdate();
         var3.close();
         return "Changed " + var1 + " user's name into " + var2;
      } else {
         return !ServerOptions.instance.getBoolean("Open") ? "User \"" + var1 + "\" is not in the whitelist, use /adduser first" : "Changed's name " + var1 + " into " + var2;
      }
   }

   public String getTOTPCode(String var1) {
      Base32 var2 = new Base32();
      byte[] var3 = var2.decode(var1);
      String var4 = Hex.encodeHexString(var3);
      return TOTP.getOTP(var4);
   }

   public String addUser(String var1, String var2) throws SQLException {
      return this.addUser(var1, var2, 1);
   }

   public String addUser(String var1, String var2, int var3) throws SQLException {
      if (this.containsCaseinsensitiveUser(var1)) {
         return "A user with this name already exists";
      } else {
         try {
            PreparedStatement var4 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
            var4.setString(1, var1);
            var4.setString(2, Core.GameSaveWorld);
            ResultSet var5 = var4.executeQuery();
            if (var5.next()) {
               var4.close();
               return "User " + var1 + " already exist.";
            }

            var4.close();
            var4 = this.conn.prepareStatement("INSERT INTO whitelist (world, username, password, encryptedPwd, pwdEncryptType, authType, role) VALUES (?, ?, ?, 'true', '2', ?, ?)");
            var4.setString(1, Core.GameSaveWorld);
            var4.setString(2, var1);
            var4.setString(3, var2);
            var4.setInt(4, var3);
            var4.setString(5, Roles.getDefaultForNewUser().getName());
            var4.executeUpdate();
            var4.close();
         } catch (SQLException var6) {
            var6.printStackTrace();
         }

         return "User " + var1 + " created with the password " + var2;
      }
   }

   public void updateDisplayName(String var1, String var2) {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
         var3.setString(1, var1);
         var3.setString(2, Core.GameSaveWorld);
         ResultSet var4 = var3.executeQuery();
         if (var4.next()) {
            var3.close();
            var3 = this.conn.prepareStatement("UPDATE whitelist SET displayName = ? WHERE username = ?");
            var3.setString(1, var2);
            var3.setString(2, var1);
            var3.executeUpdate();
            var3.close();
         }

         var3.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

   }

   public String getDisplayName(String var1) {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
         var2.setString(1, var1);
         var2.setString(2, Core.GameSaveWorld);
         ResultSet var3 = var2.executeQuery();
         if (var3.next()) {
            String var4 = var3.getString("displayName");
            var2.close();
            return var4;
         }

         var2.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

      return null;
   }

   public String removeUser(String var1, String var2) throws SQLException {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("DELETE FROM whitelist WHERE world = ? and username = ?");
         var3.setString(1, var2);
         var3.setString(2, var1);
         var3.executeUpdate();
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

      return "User " + var1 + " removed from white list";
   }

   public String removeUser(String var1) throws SQLException {
      return this.removeUser(var1, Core.GameSaveWorld);
   }

   public void removeUserLog(String var1, String var2, String var3) throws SQLException {
      try {
         PreparedStatement var4 = this.conn.prepareStatement("DELETE FROM userlog WHERE username = ? AND type = ? AND text = ?");
         var4.setString(1, var1);
         var4.setString(2, var2);
         var4.setString(3, var3);
         var4.executeUpdate();
         var4.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

   }

   public void connect() {
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "db");
      if (!var1.exists()) {
         var1.mkdirs();
      }

      var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var2 = new File(var10002 + File.separator + "db" + File.separator + GameServer.ServerName + ".db");
      var2.setReadable(true, false);
      var2.setExecutable(true, false);
      var2.setWritable(true, false);
      DebugLog.DetailedInfo.trace("user database \"" + var2.getPath() + "\"");
      if (!var2.exists()) {
         DebugLog.log("user database doesn't exist");
      } else {
         if (this.conn == null) {
            try {
               this.conn = PZSQLUtils.getConnection(var2.getAbsolutePath());
            } catch (Exception var5) {
               var5.printStackTrace();
               DebugLog.log("failed to open user database");
            }
         } else {
            try {
               if (this.conn.isClosed()) {
                  this.conn = PZSQLUtils.getConnection(var2.getAbsolutePath());
               }
            } catch (Exception var4) {
               var4.printStackTrace();
               DebugLog.log("failed to open user database");
            }
         }

      }
   }

   public void create() throws SQLException, ClassNotFoundException {
      String var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var1 = new File(var10002 + File.separator + "db");
      if (!var1.exists()) {
         var1.mkdirs();
      }

      var10002 = ZomboidFileSystem.instance.getCacheDir();
      File var2 = new File(var10002 + File.separator + "db" + File.separator + GameServer.ServerName + ".db");
      var2.setReadable(true, false);
      var2.setExecutable(true, false);
      var2.setWritable(true, false);
      DebugLog.DetailedInfo.trace("user database \"" + var2.getPath() + "\"");
      if (!var2.exists()) {
         try {
            var2.createNewFile();
            this.conn = PZSQLUtils.getConnection(var2.getAbsolutePath());
            Statement var3 = this.conn.createStatement();
            var3.executeUpdate("CREATE TABLE [whitelist] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,[world] TEXT DEFAULT '" + GameServer.ServerName + "' NULL,[username] TEXT  NULL, [password] TEXT  NULL, [lastConnection] TEXT NULL, [role] TEXT 'user', [authType] INTEGER NULL DEFAULT 1, [googleKey] TEXT NULL, [encryptedPwd] BOOLEAN NULL DEFAULT false, [pwdEncryptType] INTEGER NULL DEFAULT 1, [steamid] TEXT NULL, [ownerid] TEXT NULL, [displayName] TEXT NULL)");
            var3.executeUpdate("CREATE UNIQUE INDEX [id] ON [whitelist]([id]  ASC)");
            var3.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
            var3.executeUpdate("CREATE TABLE [bannedip] ([ip] TEXT NOT NULL,[username] TEXT NULL, [reason] TEXT NULL)");
            var3.close();
         } catch (Exception var15) {
            var15.printStackTrace();
            DebugLog.log("failed to create user database, server shut down");
            System.exit(1);
         }
      }

      if (this.conn == null) {
         try {
            this.conn = PZSQLUtils.getConnection(var2.getAbsolutePath());
         } catch (Exception var14) {
            var14.printStackTrace();
            DebugLog.log("failed to open user database, server shut down");
            System.exit(1);
         }
      } else {
         try {
            if (this.conn.isClosed()) {
               this.conn = PZSQLUtils.getConnection(var2.getAbsolutePath());
            }
         } catch (Exception var13) {
            var13.printStackTrace();
            DebugLog.log("failed to open user database, server shut down");
         }
      }

      DatabaseMetaData var16 = this.conn.getMetaData();
      Statement var4 = this.conn.createStatement();
      ResultSet var5 = var16.getColumns((String)null, (String)null, "whitelist", "lastConnection");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'lastConnection' TEXT NULL");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "whitelist", "encryptedPwd");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'encryptedPwd' BOOLEAN NULL DEFAULT false");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "whitelist", "pwdEncryptType");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'pwdEncryptType' INTEGER NULL DEFAULT 1");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "whitelist", "authType");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'authType' INTEGER NULL DEFAULT 1");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "whitelist", "googleKey");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'googleKey' TEXT NULL");
      }

      var5.close();
      if (SteamUtils.isSteamModeEnabled()) {
         var5 = var16.getColumns((String)null, (String)null, "whitelist", "steamid");
         if (!var5.next()) {
            var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'steamid' TEXT NULL");
         }

         var5.close();
         var5 = var16.getColumns((String)null, (String)null, "whitelist", "ownerid");
         if (!var5.next()) {
            var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'ownerid' TEXT NULL");
         }

         var5.close();
      }

      var5 = var16.getColumns((String)null, (String)null, "whitelist", "role");
      PreparedStatement var6;
      PreparedStatement var8;
      String var9;
      if (!var5.next()) {
         var4.executeUpdate("CREATE TABLE [whitelist_new] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,[world] TEXT DEFAULT '" + GameServer.ServerName + "' NULL,[username] TEXT  NULL, [password] TEXT  NULL, [lastConnection] TEXT NULL, [role] TEXT 'user', [authType] INTEGER NULL DEFAULT 1, [googleKey] TEXT NULL, [encryptedPwd] BOOLEAN NULL DEFAULT false, [pwdEncryptType] INTEGER NULL DEFAULT 1, [steamid] TEXT NULL, [ownerid] TEXT NULL)");
         var6 = this.conn.prepareStatement("SELECT * FROM whitelist");

         ResultSet var7;
         for(var7 = var6.executeQuery(); var7.next(); var8.executeUpdate()) {
            var8 = this.conn.prepareStatement("INSERT INTO whitelist_new (world, username, password, lastConnection, role, authType, googleKey, encryptedPwd, pwdEncryptType, steamid, ownerid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            var9 = Roles.getDefaultForUser().getName();
            if ("true".equals(var7.getString("banned"))) {
               var9 = Roles.getDefaultForBanned().getName();
            }

            if ("true".equals(var7.getString("priority"))) {
               var9 = Roles.getDefaultForPriorityUser().getName();
            }

            if ("true".equals(var7.getString("moderator"))) {
               var9 = Roles.getDefaultForModerator().getName();
            }

            if ("true".equals(var7.getString("admin"))) {
               var9 = Roles.getDefaultForAdmin().getName();
            }

            if ("admin".equals(var7.getString("accesslevel"))) {
               var9 = Roles.getDefaultForAdmin().getName();
            }

            var8.setString(1, var7.getString("world"));
            var8.setString(2, var7.getString("username"));
            var8.setString(3, var7.getString("password"));
            var8.setString(4, var7.getString("lastConnection"));
            var8.setString(5, var9);
            var8.setString(6, var7.getString("authType"));
            var8.setString(7, var7.getString("googleKey"));
            var8.setString(8, var7.getString("encryptedPwd"));
            var8.setString(9, var7.getString("pwdEncryptType"));

            try {
               var8.setString(10, var7.getString("steamid"));
               var8.setString(11, var7.getString("ownerid"));
            } catch (Exception var12) {
               var8.setString(10, (String)null);
               var8.setString(11, (String)null);
            }
         }

         var7.close();
         var4.executeUpdate("DROP TABLE 'whitelist'");
         var4.executeUpdate("ALTER TABLE 'whitelist_new' RENAME TO 'whitelist'");
         var4.executeUpdate("CREATE UNIQUE INDEX [id] ON [whitelist]([id]  ASC)");
         var4.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "whitelist", "displayName");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'whitelist' ADD 'displayName' TEXT NULL");
      }

      var5.close();
      var5 = var4.executeQuery("SELECT * FROM sqlite_master WHERE type = 'index' AND sql LIKE '%UNIQUE%' and name = 'username'");
      if (!var5.next()) {
         try {
            var4.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
         } catch (Exception var11) {
            System.out.println("Can't create the username index because some of the username in the database are in double, will drop the double username.");
            var4.executeUpdate("DELETE FROM whitelist WHERE whitelist.rowid > (SELECT rowid FROM whitelist dbl WHERE whitelist.rowid <> dbl.rowid AND  whitelist.username = dbl.username);");
            var4.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
         }
      }

      var5 = var16.getTables((String)null, (String)null, "bannedip", (String[])null);
      if (!var5.next()) {
         var4.executeUpdate("CREATE TABLE [bannedip] ([ip] TEXT NOT NULL,[username] TEXT NULL, [reason] TEXT NULL)");
      }

      var5.close();
      var5 = var16.getTables((String)null, (String)null, "bannedid", (String[])null);
      if (!var5.next()) {
         var4.executeUpdate("CREATE TABLE [bannedid] ([steamid] TEXT NOT NULL, [reason] TEXT NULL)");
      }

      var5.close();
      var5 = var16.getTables((String)null, (String)null, "userlog", (String[])null);
      if (!var5.next()) {
         var4.executeUpdate("CREATE TABLE [userlog] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,[username] TEXT  NULL,[type] TEXT  NULL, [text] TEXT  NULL, [issuedBy] TEXT  NULL, [amount] INTEGER NULL, [lastUpdate] TEXT NULL)");
      }

      var5.close();
      var5 = var16.getColumns((String)null, (String)null, "userlog", "lastUpdate");
      if (!var5.next()) {
         var4.executeUpdate("ALTER TABLE 'userlog' ADD 'lastUpdate' TEXT NULL");
      }

      var5.close();
      var5 = var16.getTables((String)null, (String)null, "tickets", (String[])null);
      if (!var5.next()) {
         var4.executeUpdate("CREATE TABLE [tickets] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL, [message] TEXT NOT NULL, [author] TEXT NOT NULL,[answeredID] INTEGER,[viewed] BOOLEAN NULL DEFAULT false)");
      }

      var5.close();
      var6 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ?");
      var6.setString(1, this.CommandLineAdminUsername);
      var5 = var6.executeQuery();
      String var17;
      if (!var5.next()) {
         var6.close();
         var17 = this.CommandLineAdminPassword;
         if (var17 == null || var17.isEmpty()) {
            Scanner var18 = new Scanner(new InputStreamReader(System.in));
            System.out.println("User 'admin' not found, creating it ");
            System.out.println("Command line admin password: " + this.CommandLineAdminPassword);
            System.out.println("Enter new administrator password: ");
            var17 = var18.nextLine();

            label154:
            while(true) {
               if (var17 != null && !"".equals(var17)) {
                  System.out.println("Confirm the password: ");
                  var9 = var18.nextLine();

                  while(true) {
                     if (var9 != null && !"".equals(var9) && var17.equals(var9)) {
                        break label154;
                     }

                     System.out.println("Wrong password, confirm the password: ");
                     var9 = var18.nextLine();
                  }
               }

               System.out.println("Enter new administrator password: ");
               var17 = var18.nextLine();
            }
         }

         if (this.doAdmin) {
            var6 = this.conn.prepareStatement("INSERT INTO whitelist (username, password, role, encryptedPwd, pwdEncryptType) VALUES (?, ?, '" + Roles.getDefaultForAdmin().getName() + "', 'true', '2')");
         } else {
            var6 = this.conn.prepareStatement("INSERT INTO whitelist (username, password, role, encryptedPwd, pwdEncryptType) VALUES (?, ?, '" + Roles.getDefaultForNewUser().getName() + "', 'true', '2')");
         }

         var6.setString(1, this.CommandLineAdminUsername);
         var6.setString(2, PZcrypt.hash(encrypt(var17)));
         var6.executeUpdate();
         var6.close();
         System.out.println("Administrator account '" + this.CommandLineAdminUsername + "' created.");
      } else {
         var6.close();
      }

      var4.close();
      if (this.CommandLineAdminPassword != null && !this.CommandLineAdminPassword.isEmpty()) {
         var17 = PZcrypt.hash(encrypt(this.CommandLineAdminPassword));
         var8 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ?");
         var8.setString(1, this.CommandLineAdminUsername);
         var5 = var8.executeQuery();
         if (var5.next()) {
            var8.close();
            var8 = this.conn.prepareStatement("UPDATE whitelist SET password = ? WHERE username = ?");
            var8.setString(1, var17);
            var8.setString(2, this.CommandLineAdminUsername);
            var8.executeUpdate();
            System.out.println("admin password changed via -adminpassword option");
         } else {
            System.out.println("ERROR: -adminpassword ignored, no '" + this.CommandLineAdminUsername + "' account in db");
         }

         var8.close();
      }

   }

   public void close() {
      try {
         if (this.conn != null) {
            this.conn.close();
         }
      } catch (SQLException var2) {
         var2.printStackTrace();
      }

   }

   public static boolean isValidUserName(String var0) {
      if (var0 != null && !var0.trim().isEmpty() && !var0.contains(";") && !var0.contains("@") && !var0.contains("$") && !var0.contains(",") && !var0.contains("\\") && !var0.contains("/") && !var0.contains(".") && !var0.contains("'") && !var0.contains("?") && !var0.contains("\"") && var0.trim().length() >= 3 && var0.length() <= 20) {
         if (var0.contains(nullChar)) {
            return false;
         } else if (var0.trim().equals("admin")) {
            return true;
         } else {
            return !var0.trim().toLowerCase().startsWith("admin");
         }
      } else {
         return false;
      }
   }

   public LogonResult googleAuthClient(String var1, String var2) {
      LogonResult var3 = new LogonResult();

      try {
         PreparedStatement var4 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
         var4.setString(1, var1);
         var4.setString(2, Core.GameSaveWorld);
         ResultSet var5 = var4.executeQuery();
         if (var5.next()) {
            if (isNullOrEmpty(var5.getString("googleKey"))) {
               var3.bAuthorized = false;
               var4.close();
               var3.dcReason = "GoogleSecretKeyIsAbsent";
               return var3;
            }

            if (!var2.equals(this.getTOTPCode(var5.getString("googleKey")))) {
               var3.bAuthorized = false;
               var4.close();
               var3.dcReason = "InvalidGoogleAuthCode";
               return var3;
            }

            var3.bAuthorized = true;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return var3;
   }

   public LogonResult authClient(String var1, String var2, String var3, long var4, int var6) {
      DebugLog.DetailedInfo.trace("User " + var1 + " is trying to connect.");
      LogonResult var7 = new LogonResult();
      if (!ServerOptions.instance.AllowNonAsciiUsername.getValue() && !asciiEncoder.canEncode(var1)) {
         var7.bAuthorized = false;
         var7.dcReason = "NonAsciiCharacters";
         return var7;
      } else if (!isValidUserName(var1)) {
         var7.bAuthorized = false;
         var7.dcReason = "InvalidUsername";
         return var7;
      } else {
         try {
            PreparedStatement var8;
            ResultSet var9;
            if (!SteamUtils.isSteamModeEnabled() && !var3.equals("127.0.0.1")) {
               var8 = this.conn.prepareStatement("SELECT * FROM bannedip WHERE ip = ?");
               var8.setString(1, var3);
               var9 = var8.executeQuery();
               if (var9.next()) {
                  var7.bAuthorized = false;
                  var7.bannedReason = var9.getString("reason");
                  var7.role = Roles.getDefaultForBanned();
                  var8.close();
                  return var7;
               }

               var8.close();
            }

            if (isNullOrEmpty(var2) && ServerOptions.instance.Open.getValue() && ServerOptions.instance.AutoCreateUserInWhiteList.getValue() && var6 != 2) {
               var7.dcReason = "UserPasswordRequired";
               var7.bAuthorized = false;
               return var7;
            }

            var8 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
            var8.setString(1, var1);
            var8.setString(2, Core.GameSaveWorld);
            var9 = var8.executeQuery();
            if (var9.next()) {
               Role var10 = Roles.getRole(var9.getString("role"));
               var7.role = var10 != null ? var10 : Roles.getDefaultForNewUser();
               if (!var7.role.haveCapability(Capability.LoginOnServer)) {
                  var7.bAuthorized = false;
                  var7.bannedReason = "";
                  var7.role = Roles.getDefaultForBanned();
                  var8.close();
                  return var7;
               }

               String var11;
               String var12;
               PreparedStatement var13;
               if (!isNullOrEmpty(var9.getString("password")) && (var9.getString("encryptedPwd").equals("false") || var9.getString("encryptedPwd").equals("N"))) {
                  var11 = var9.getString("password");
                  var12 = encrypt(var11);
                  var13 = this.conn.prepareStatement("UPDATE whitelist SET encryptedPwd = 'true' WHERE username = ? and password = ?");
                  var13.setString(1, var1);
                  var13.setString(2, var11);
                  var13.executeUpdate();
                  var13.close();
                  var13 = this.conn.prepareStatement("UPDATE whitelist SET password = ? WHERE username = ? AND password = ?");
                  var13.setString(1, var12);
                  var13.setString(2, var1);
                  var13.setString(3, var11);
                  var13.executeUpdate();
                  var13.close();
                  var9 = var8.executeQuery();
               }

               if (!isNullOrEmpty(var9.getString("password")) && var9.getInt("pwdEncryptType") == 1) {
                  var11 = var9.getString("password");
                  var12 = PZcrypt.hash(var11);
                  var13 = this.conn.prepareStatement("UPDATE whitelist SET pwdEncryptType = '2', password = ? WHERE username = ? AND password = ?");
                  var13.setString(1, var12);
                  var13.setString(2, var1);
                  var13.setString(3, var11);
                  var13.executeUpdate();
                  var13.close();
                  var9 = var8.executeQuery();
               }

               if (var9.getInt("authType") != var6) {
                  var7.bAuthorized = false;
                  var8.close();
                  var7.dcReason = "WrongAuthenticationMethod";
               }

               if ((var9.getInt("authType") == 1 || var9.getInt("authType") == 3) && !isNullOrEmpty(var9.getString("password")) && !var9.getString("password").equals(var2)) {
                  var7.bAuthorized = false;
                  var8.close();
                  if (isNullOrEmpty(var2)) {
                     var7.dcReason = "DuplicateAccount";
                  } else {
                     var7.dcReason = "InvalidUsernamePassword";
                  }

                  return var7;
               }

               if (var9.getInt("authType") == 2 || var9.getInt("authType") == 3) {
                  if (!isNullOrEmpty(var9.getString("googleKey"))) {
                     var7.bNeedSecondFactor = true;
                  } else {
                     var7.bNeedSecondFactor = false;
                  }
               }

               var7.bAuthorized = true;
               var8.close();
               return var7;
            }

            if (ServerOptions.instance.Open.getValue()) {
               if (!this.isNewAccountAllowed(var3, var4)) {
                  var8.close();
                  var7.bAuthorized = false;
                  var7.dcReason = "MaxAccountsReached";
                  return var7;
               }

               var7.bAuthorized = true;
               var8.close();
               return var7;
            }

            var7.bAuthorized = false;
            var7.dcReason = "UnknownUsername";
            var8.close();
         } catch (Exception var14) {
            var14.printStackTrace();
         }

         return var7;
      }
   }

   public LogonResult authClient(long var1) {
      String var3 = SteamUtils.convertSteamIDToString(var1);
      System.out.println("Steam client " + var3 + " is initiating a connection.");
      LogonResult var4 = new LogonResult();

      try {
         PreparedStatement var5 = this.conn.prepareStatement("SELECT * FROM bannedid WHERE steamid = ?");
         var5.setString(1, var3);
         ResultSet var6 = var5.executeQuery();
         if (var6.next()) {
            var4.bAuthorized = false;
            var4.bannedReason = var6.getString("reason");
            var4.role = Roles.getDefaultForBanned();
            var5.close();
            return var4;
         }

         var5.close();
         var4.bAuthorized = true;
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      return var4;
   }

   public LogonResult authOwner(long var1, long var3) {
      String var5 = SteamUtils.convertSteamIDToString(var1);
      String var6 = SteamUtils.convertSteamIDToString(var3);
      System.out.println("Steam client " + var5 + " borrowed the game from " + var6);
      LogonResult var7 = new LogonResult();

      try {
         PreparedStatement var8 = this.conn.prepareStatement("SELECT * FROM bannedid WHERE steamid = ?");
         var8.setString(1, var6);
         ResultSet var9 = var8.executeQuery();
         if (var9.next()) {
            var7.bAuthorized = false;
            var7.bannedReason = var9.getString("reason");
            var7.role = Roles.getDefaultForBanned();
            var8.close();
            return var7;
         }

         var8.close();
         var7.bAuthorized = true;
         var8 = this.conn.prepareStatement("UPDATE whitelist SET ownerid = ? where steamid = ?");
         var8.setString(1, var6);
         var8.setString(2, var5);
         var8.executeUpdate();
         var8.close();
      } catch (Exception var10) {
         var10.printStackTrace();
      }

      return var7;
   }

   private boolean isNewAccountAllowed(String var1, long var2) {
      int var4 = ServerOptions.instance.MaxAccountsPerUser.getValue();
      if (var4 <= 0) {
         return true;
      } else if (!SteamUtils.isSteamModeEnabled()) {
         return true;
      } else {
         String var5 = SteamUtils.convertSteamIDToString(var2);
         int var6 = 0;

         try {
            PreparedStatement var7 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE steamid = ? AND ((accessLevel = ?) OR (accessLevel is NULL))");

            try {
               var7.setString(1, var5);
               var7.setString(2, "");

               for(ResultSet var8 = var7.executeQuery(); var8.next(); ++var6) {
               }
            } catch (Throwable var11) {
               if (var7 != null) {
                  try {
                     var7.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var7 != null) {
               var7.close();
            }
         } catch (Exception var12) {
            DebugLog.Multiplayer.printException(var12, "Query execution failed", LogSeverity.Error);
            return true;
         }

         DebugLog.Multiplayer.debugln("IsNewAccountAllowed: steam-id=%d count=%d/%d", var2, var6, var4);
         return var6 < var4;
      }
   }

   public static String encrypt(String var0) {
      if (isNullOrEmpty(var0)) {
         return "";
      } else {
         byte[] var1 = null;

         try {
            var1 = MessageDigest.getInstance("MD5").digest(var0.getBytes());
         } catch (NoSuchAlgorithmException var5) {
            System.out.println("Can't encrypt password");
            var5.printStackTrace();
         }

         StringBuilder var2 = new StringBuilder();

         for(int var3 = 0; var3 < var1.length; ++var3) {
            String var4 = Integer.toHexString(var1[var3]);
            if (var4.length() == 1) {
               var2.append('0');
               var2.append(var4.charAt(var4.length() - 1));
            } else {
               var2.append(var4.substring(var4.length() - 2));
            }
         }

         return var2.toString();
      }
   }

   public String changePassword(String var1, String var2) throws SQLException {
      PreparedStatement var4 = this.conn.prepareStatement("UPDATE whitelist SET pwdEncryptType = '2', password = ?, authType = ? WHERE username = ? and world = ?");
      var4.setString(1, var2);
      var4.setInt(2, 1);
      var4.setString(3, var1);
      var4.setString(4, Core.GameSaveWorld);
      var4.executeUpdate();
      var4.close();
      return "Your new password is " + var2;
   }

   public String changePwd(String var1, String var2, String var3) throws SQLException {
      PreparedStatement var5 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND password = ? AND world = ?");
      var5.setString(1, var1);
      var5.setString(2, var2);
      var5.setString(3, Core.GameSaveWorld);
      ResultSet var6 = var5.executeQuery();
      if (var6.next()) {
         var5.close();
         var5 = this.conn.prepareStatement("UPDATE whitelist SET pwdEncryptType = '2', password = ? WHERE username = ? and password = ?");
         var5.setString(1, var3);
         var5.setString(2, var1);
         var5.setString(3, var2);
         var5.executeUpdate();
         var5.close();
         return "Your new password is " + var3;
      } else {
         var5.close();
         return "Wrong password for user " + var1;
      }
   }

   public String grantAdmin(String var1, boolean var2) throws SQLException {
      PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
      var3.setString(1, var1);
      var3.setString(2, Core.GameSaveWorld);
      ResultSet var4 = var3.executeQuery();
      if (var4.next()) {
         var3.close();
         var3 = this.conn.prepareStatement("UPDATE whitelist SET admin = ? WHERE username = ?");
         var3.setString(1, var2 ? "true" : "false");
         var3.setString(2, var1);
         var3.executeUpdate();
         var3.close();
         return var2 ? "User " + var1 + " is now admin" : "User " + var1 + " is no longer admin";
      } else {
         var3.close();
         return "User \"" + var1 + "\" is not in the whitelist, use /adduser first";
      }
   }

   public String setRole(String var1, Role var2) throws SQLException {
      if (!this.containsUser(var1)) {
         this.addUser(var1, "");
      }

      PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
      var3.setString(1, var1);
      var3.setString(2, Core.GameSaveWorld);
      ResultSet var4 = var3.executeQuery();
      if (var4.next()) {
         var3.close();
         var3 = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE username = ?");
         var3.setString(1, var2.getName());
         var3.setString(2, var1);
         var3.executeUpdate();
         var3.close();
         return "User " + var1 + " is now " + var2.getName();
      } else {
         var3.close();
         return "User \"" + var1 + "\" is not in the whitelist, use /adduser first";
      }
   }

   public ArrayList<Userlog> getUserlog(String var1) {
      ArrayList var2 = new ArrayList();

      try {
         PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM userlog WHERE username = ?");
         var3.setString(1, var1);
         ResultSet var4 = var3.executeQuery();

         while(var4.next()) {
            var2.add(new Userlog(var1, var4.getString("type"), var4.getString("text"), var4.getString("issuedBy"), var4.getInt("amount"), var4.getString("lastUpdate")));
         }

         var3.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

      return var2;
   }

   public void addUserlog(String var1, Userlog.UserlogType var2, String var3, String var4, int var5) {
      try {
         boolean var6 = true;
         String var7 = dateFormat.format(Calendar.getInstance().getTime());
         PreparedStatement var8;
         ResultSet var9;
         PreparedStatement var10;
         if (var2 != Userlog.UserlogType.LuaChecksum && var2 != Userlog.UserlogType.DupeItem) {
            if (var2 == Userlog.UserlogType.Kicked || var2 == Userlog.UserlogType.Banned || var2 == Userlog.UserlogType.SuspiciousActivity || var2 == Userlog.UserlogType.UnauthorizedPacket) {
               var8 = this.conn.prepareStatement("SELECT * FROM userlog WHERE username = ? AND type = ? AND text = ? AND issuedBy = ?");
               var8.setString(1, var1);
               var8.setString(2, var2.toString());
               var8.setString(3, var3);
               var8.setString(4, var4);
               var9 = var8.executeQuery();
               if (var9.next()) {
                  var6 = false;
                  var5 = Integer.parseInt(var9.getString("amount")) + 1;
                  var8.close();
                  var10 = this.conn.prepareStatement("UPDATE userlog set amount = ?, lastUpdate = ? WHERE username = ? AND type = ? AND text = ? AND issuedBy = ?");
                  var10.setString(1, String.valueOf(var5));
                  var10.setString(2, var7);
                  var10.setString(3, var1);
                  var10.setString(4, var2.toString());
                  var10.setString(5, var3);
                  var10.setString(6, var4);
                  var10.executeUpdate();
                  var10.close();
               }
            }
         } else {
            var8 = this.conn.prepareStatement("SELECT * FROM userlog WHERE username = ? AND type = ?");
            var8.setString(1, var1);
            var8.setString(2, var2.toString());
            var9 = var8.executeQuery();
            if (var9.next()) {
               var6 = false;
               var5 = Integer.parseInt(var9.getString("amount")) + 1;
               var8.close();
               var10 = this.conn.prepareStatement("UPDATE userlog set amount = ?, lastUpdate = ?, text = ? WHERE username = ? AND type = ?");
               var10.setString(1, String.valueOf(var5));
               var10.setString(2, var7);
               var10.setString(3, var3);
               var10.setString(4, var1);
               var10.setString(5, var2.toString());
               var10.executeUpdate();
               var10.close();
            }
         }

         if (var6) {
            var8 = this.conn.prepareStatement("INSERT INTO userlog (username, type, text, issuedBy, amount, lastUpdate) VALUES (?, ?, ?, ?, ?, ?)");
            var8.setString(1, var1);
            var8.setString(2, var2.toString());
            var8.setString(3, var3);
            var8.setString(4, var4);
            var8.setString(5, String.valueOf(var5));
            var8.setString(6, var7);
            var8.executeUpdate();
            var8.close();
         }
      } catch (Exception var11) {
         var11.printStackTrace();
      }

   }

   public String banUser(String var1, boolean var2) throws SQLException {
      PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
      var3.setString(1, var1);
      var3.setString(2, Core.GameSaveWorld);
      ResultSet var4 = var3.executeQuery();
      boolean var5 = var4.next();
      if (var2 && !var5) {
         PreparedStatement var6 = this.conn.prepareStatement("INSERT INTO whitelist (world, username, role, password, encryptedPwd) VALUES (?, ?, 'banned', 'bogus', 'false')");
         var6.setString(1, Core.GameSaveWorld);
         var6.setString(2, var1);
         var6.executeUpdate();
         var6.close();
         var4 = var3.executeQuery();
         var5 = true;
      }

      var3.close();
      if (var5) {
         String var8 = var2 ? "banned" : "user";
         var3 = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE username = ?");
         var3.setString(1, var8);
         var3.setString(2, var1);
         var3.executeUpdate();
         var3.close();
         if (!SteamUtils.isSteamModeEnabled()) {
         }

         var3 = this.conn.prepareStatement("SELECT steamid FROM whitelist WHERE username = ? AND world = ?");
         var3.setString(1, var1);
         var3.setString(2, Core.GameSaveWorld);
         var4 = var3.executeQuery();
         if (var4.next()) {
            String var7 = var4.getString("steamid");
            var3.close();
            if (var7 != null && !var7.isEmpty()) {
               this.banSteamID(var7, "", var2);
            }
         } else {
            var3.close();
         }

         return "User \"" + var1 + "\" is now " + (var2 ? "banned" : "un-banned");
      } else {
         return "User \"" + var1 + "\" is not in the whitelist, use /adduser first";
      }
   }

   public String banIp(String var1, String var2, String var3, boolean var4) throws SQLException {
      PreparedStatement var5;
      if (var4) {
         var5 = this.conn.prepareStatement("INSERT INTO bannedip (ip, username, reason) VALUES (?, ?, ?)");
         var5.setString(1, var1);
         var5.setString(2, var2);
         var5.setString(3, var3);
         var5.executeUpdate();
         var5.close();
      } else {
         if (var1 != null) {
            var5 = this.conn.prepareStatement("DELETE FROM bannedip WHERE ip = ?");
            var5.setString(1, var1);
            var5.executeUpdate();
            var5.close();
         }

         var5 = this.conn.prepareStatement("DELETE FROM bannedip WHERE username = ?");
         var5.setString(1, var2);
         var5.executeUpdate();
         var5.close();
      }

      return "";
   }

   public String banSteamID(String var1, String var2, boolean var3) throws SQLException {
      PreparedStatement var4;
      if (var3) {
         var4 = this.conn.prepareStatement("INSERT INTO bannedid (steamid, reason) VALUES (?, ?)");
         var4.setString(1, var1);
         var4.setString(2, var2);
         var4.executeUpdate();
         var4.close();
      } else {
         var4 = this.conn.prepareStatement("DELETE FROM bannedid WHERE steamid = ?");
         var4.setString(1, var1);
         var4.executeUpdate();
         var4.close();
      }

      return "";
   }

   public String setUserSteamID(String var1, String var2) {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ?");
         var3.setString(1, var1);
         ResultSet var4 = var3.executeQuery();
         if (!var4.next()) {
            var3.close();
            return "User " + var1 + " not found";
         }

         var3.close();
         var3 = this.conn.prepareStatement("UPDATE whitelist SET steamid = ? WHERE username = ?");
         var3.setString(1, var2);
         var3.setString(2, var1);
         var3.executeUpdate();
         var3.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

      return "User " + var1 + " SteamID set to " + var2;
   }

   public void setPassword(String var1, String var2) throws SQLException {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("UPDATE whitelist SET pwdEncryptType = '2', password = ? WHERE username = ? and world = ?");
         var3.setString(1, var2);
         var3.setString(2, var1);
         var3.setString(3, Core.GameSaveWorld);
         var3.executeUpdate();
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

   }

   public String getUserGoogleKey(String var1) throws SQLException {
      String var2 = "";

      try {
         PreparedStatement var3 = this.conn.prepareStatement("SELECT googleKey FROM whitelist WHERE username = ? and world = ?");
         var3.setString(1, var1);
         var3.setString(2, Core.GameSaveWorld);
         ResultSet var4 = var3.executeQuery();
         if (var4.next()) {
            var2 = var4.getString("googleKey");
         }

         var3.close();
      } catch (SQLException var5) {
         var5.printStackTrace();
      }

      return var2;
   }

   public void setUserGoogleKey(String var1, String var2) throws SQLException {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("UPDATE whitelist SET googleKey = ? WHERE username = ? and world = ?");
         var3.setString(1, var2);
         var3.setString(2, var1);
         var3.setString(3, Core.GameSaveWorld);
         var3.executeUpdate();
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

   }

   public void resetUserGoogleKey(String var1) throws SQLException {
      try {
         PreparedStatement var2 = this.conn.prepareStatement("UPDATE whitelist SET googleKey = ?, authType = ? WHERE username = ? and world = ?");
         var2.setString(1, "");
         var2.setInt(2, 1);
         var2.setString(3, var1);
         var2.setString(4, Core.GameSaveWorld);
         var2.executeUpdate();
         var2.close();
      } catch (SQLException var3) {
         var3.printStackTrace();
      }

   }

   public void updateLastConnectionDate(String var1, String var2) {
      try {
         PreparedStatement var3 = this.conn.prepareStatement("UPDATE whitelist SET lastConnection = ? WHERE username = ? AND password = ?");
         var3.setString(1, dateFormat.format(Calendar.getInstance().getTime()));
         var3.setString(2, var1);
         var3.setString(3, var2);
         var3.executeUpdate();
         var3.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

   }

   private static boolean isNullOrEmpty(String var0) {
      return var0 == null || var0.isEmpty();
   }

   public String addWarningPoint(String var1, String var2, int var3, String var4) throws SQLException {
      PreparedStatement var5 = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
      var5.setString(1, var1);
      var5.setString(2, Core.GameSaveWorld);
      ResultSet var6 = var5.executeQuery();
      if (var6.next()) {
         this.addUserlog(var1, Userlog.UserlogType.WarningPoint, var2, var4, var3);
         return "Added a warning point on " + var1 + " reason: " + var2;
      } else {
         return "User " + var1 + " doesn't exist.";
      }
   }

   public void addTicket(String var1, String var2, int var3) throws SQLException {
      PreparedStatement var4;
      if (var3 > -1) {
         var4 = this.conn.prepareStatement("INSERT INTO tickets (author, message, answeredID) VALUES (?, ?, ?)");
         var4.setString(1, var1);
         var4.setString(2, var2);
         var4.setInt(3, var3);
         var4.executeUpdate();
         var4.close();
      } else {
         var4 = this.conn.prepareStatement("INSERT INTO tickets (author, message) VALUES (?, ?)");
         var4.setString(1, var1);
         var4.setString(2, var2);
         var4.executeUpdate();
         var4.close();
      }

   }

   public ArrayList<DBTicket> getTickets(String var1) throws SQLException {
      ArrayList var2 = new ArrayList();
      PreparedStatement var3 = null;
      if (var1 != null) {
         var3 = this.conn.prepareStatement("SELECT * FROM tickets WHERE author = ? and answeredID is null");
         var3.setString(1, var1);
      } else {
         var3 = this.conn.prepareStatement("SELECT * FROM tickets where answeredID is null");
      }

      ResultSet var4 = var3.executeQuery();

      while(var4.next()) {
         DBTicket var5 = new DBTicket(var4.getString("author"), var4.getString("message"), var4.getInt("id"));
         var2.add(var5);
         DBTicket var6 = this.getAnswer(var5.getTicketID());
         if (var6 != null) {
            var5.setAnswer(var6);
         }
      }

      return var2;
   }

   private DBTicket getAnswer(int var1) throws SQLException {
      PreparedStatement var2 = null;
      var2 = this.conn.prepareStatement("SELECT * FROM tickets WHERE answeredID = ?");
      var2.setInt(1, var1);
      ResultSet var3 = var2.executeQuery();
      return var3.next() ? new DBTicket(var3.getString("author"), var3.getString("message"), var3.getInt("id")) : null;
   }

   public void removeTicket(int var1) throws SQLException {
      DBTicket var2 = this.getAnswer(var1);
      PreparedStatement var3;
      if (var2 != null) {
         var3 = this.conn.prepareStatement("DELETE FROM tickets WHERE id = ?");
         var3.setInt(1, var2.getTicketID());
         var3.executeUpdate();
         var3.close();
      }

      var3 = this.conn.prepareStatement("DELETE FROM tickets WHERE id = ?");
      var3.setInt(1, var1);
      var3.executeUpdate();
      var3.close();
   }

   public class LogonResult {
      public boolean bAuthorized = false;
      public boolean bNeedSecondFactor = false;
      public int x;
      public int y;
      public int z;
      public String bannedReason = null;
      public String dcReason = null;
      public Role role = Roles.getDefaultForNewUser();

      public LogonResult() {
      }
   }
}
