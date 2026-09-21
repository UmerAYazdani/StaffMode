StaffMode clean split project

Files were separated to make the plugin easier to maintain:

- StaffModePlugin.java
  Main plugin class. Registers commands/listeners and starts managers.

- staff/StaffModeManager.java
  Handles /sm inventory save/restore, staff mode toggling, build/drop/pickup toggles.

- hours/StaffHoursManager.java
  Handles staff hour sessions, AFK pause/resume, config messages, and time calculations.

- gui/StaffHoursGui.java
  Handles /sh GUI: staff list, months, current week, weekly/month totals.

- gui/StaffPunishGui.java
  Handles staff punishment GUI, mute/ban/kick reason prompt, offenses menu, player info.

- offenses/OffenseManager.java
  Handles offense YAML storage and join alerts.

- listeners/StaffModeListener.java
  Handles movement/activity, quit/rejoin tracking, block protection, pickup/drop, teleport flight reset.

- commands/*.java
  One file per command: /sm, /punish, /RandomInspectionTP, /staffhours.

- util/ItemUtil.java
  Small helper methods for menu items and color codes.

Build:
1. Install/select JDK 21 in IntelliJ.
2. Open this folder as a Gradle project.
3. Run Gradle task: build.
4. Copy build/libs/StaffMode-1.0.1.jar into your server plugins folder.
